package org.solarframework.proxyserver.spring;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.solarframework.proxyserver.obj.domains.Domain;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

/** The controller against a real backend on a local port: the files of a folder, the fetch for a server, and what that server is told about the caller. */
class ProxyControllerTest {

    @TempDir Path root;
    private HttpServer backend;
    private final Map<String, String> seen = new ConcurrentHashMap<>();
    private final ProxyController controller = new ProxyController();

    @BeforeEach void start() throws Exception {
        backend = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        backend.createContext("/", exchange -> {
            exchange.getRequestHeaders().forEach((name, values) -> seen.put(name.toLowerCase(), String.join(",", values)));
            seen.put("uri", exchange.getRequestURI().toString());
            byte[] body = "from the backend".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/plain");
            exchange.sendResponseHeaders(exchange.getRequestURI().getPath().equals("/missing") ? 404 : 200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        backend.start();
        Path folder = Files.createDirectories(root.resolve("site"));
        Files.writeString(folder.resolve("index.html"), "home");
        Files.createDirectories(folder.resolve(".git"));
        Files.writeString(folder.resolve(".git/config"), "TOP SECRET");
        ProxyService.PROXYSERVER = new ProxyService(List.of(new Domain("127.0.0.1", "mysite.com", folder.toString()),
                new Domain("127.0.0.1", "app.com", "http://127.0.0.1:" + backend.getAddress().getPort())), false);
    }

    @AfterEach void stop() { backend.stop(0); }

    private ResponseEntity<Resource> get(String host, String path) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        request.setRemoteAddr("10.0.0.4");
        request.setScheme("https");
        request.addHeader("X-Forwarded-For", "6.6.6.6");
        request.addHeader("X-ProxyGuard-Checked", "1");
        return controller.serve(request, host);
    }

    private static String text(ResponseEntity<Resource> response) throws Exception {
        return new String(response.getBody().getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }

    @Test void aFolderDomainServesItsFiles() throws Exception {
        assertEquals("home", text(get("mysite.com:443", "/")));
    }

    @Test void aFileInsideADotFolderIsNotServed() throws Exception {
        assertEquals(404, get("mysite.com", "/.git/config").getStatusCode().value());
    }

    /** Java threw a NullPointerException here. */
    @Test void aHostNobodyOwnsIsNotFound() throws Exception {
        assertEquals(404, get("nobody.com", "/").getStatusCode().value());
    }

    /** Java ran stripPort over the domain's path, so "http://127.0.0.1:port" became "http" and no fetch ever reached a server. */
    @Test void aServerDomainIsFetchedFromAndToldWhoCalled() throws Exception {
        ResponseEntity<Resource> response = get("app.com", "/api/x");

        assertEquals("from the backend", text(response));
        assertEquals("/api/x", seen.get("uri"));
        assertEquals("10.0.0.4", seen.get("x-forwarded-for"));
        assertEquals("10.0.0.4", seen.get("x-real-ip"));
        assertEquals("0", seen.get("x-proxyguard-checked"));
        assertEquals("https", seen.get("x-forwarded-proto"));
        assertEquals("app.com", seen.get("x-forwarded-host"));
    }

    @Test void theServersOwnStatusComesBack() throws Exception {
        assertEquals(404, get("app.com", "/missing").getStatusCode().value());
    }

    @Test void aDotDotSegmentIsABadRequest() throws Exception {
        assertEquals(400, get("mysite.com", "/../secret").getStatusCode().value());
    }
}

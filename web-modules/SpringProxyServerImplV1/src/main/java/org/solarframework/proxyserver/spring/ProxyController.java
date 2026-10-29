package org.solarframework.proxyserver.spring;

import jakarta.servlet.http.HttpServletRequest;
import org.solarframework.proxyserver.obj.CallerHeaders;
import org.solarframework.proxyserver.obj.domains.BaseDomain;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.Arrays;

import static org.solarframework.proxyserver.spring.ProxyService.PROXYSERVER;

/**
 * Every GET of a domain: its folder's file ({@link BaseDomain#fileFor}), or for a domain that forwards, a plain GET of the same address from its server - not a
 * forward of the method, headers and body. The fetch tells the server who called ({@link CallerHeaders}); without it every caller reached the server as this
 * proxy's own loopback address, so an allow list holding {@code localhost} admitted everyone and all callers shared one limit. No rate limits here.
 */
@RestController
public class ProxyController {
    private static final HttpClient CLIENT = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();

    @GetMapping("/**")
    public ResponseEntity<Resource> serve(HttpServletRequest request, @RequestHeader("Host") String host) throws Exception {
        BaseDomain<?> domain = PROXYSERVER.getDomainOfHost(PROXYSERVER.stripPort(host));
        if (domain == null) return ResponseEntity.notFound().build();
        String filePath = request.getRequestURI().substring(request.getContextPath().length());
        if (Arrays.asList(filePath.split("/")).contains("..")) return ResponseEntity.badRequest().build();
        if (filePath.startsWith("/_")) filePath = filePath.replaceFirst("/_", "/");
        return domain.isProxy() ? fetch(domain.getPath(), filePath, request, host) : domain.fileFor(filePath).map(ProxyController::file).orElseGet(() -> ResponseEntity.notFound().build());
    }

    private static ResponseEntity<Resource> file(Path file) {
        return ResponseEntity.ok().contentType(MediaTypeFactory.getMediaType(file.getFileName().toString()).orElse(MediaType.APPLICATION_OCTET_STREAM)).body(new FileSystemResource(file));
    }

    /** The server's own status and content type come back; one that cannot be reached is a 502. */
    private static ResponseEntity<Resource> fetch(String target, String filePath, HttpServletRequest request, String host) throws InterruptedException {
        String query = request.getQueryString() == null ? "" : "?" + request.getQueryString();
        HttpRequest.Builder get = HttpRequest.newBuilder(URI.create(target.replaceFirst("/+$", "") + filePath + query)).GET();
        new CallerHeaders(request.getRemoteAddr(), host, request.getScheme(), false).writeInto(get::setHeader, name -> {});
        try {
            HttpResponse<InputStream> response = CLIENT.send(get.build(), HttpResponse.BodyHandlers.ofInputStream());
            MediaType type = response.headers().firstValue(HttpHeaders.CONTENT_TYPE).map(MediaType::parseMediaType).orElse(MediaType.APPLICATION_OCTET_STREAM);
            return ResponseEntity.status(response.statusCode()).contentType(type).body(new InputStreamResource(response.body()));
        } catch (java.io.IOException e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }
    }

    private static boolean isImage(String path) {
        return path.endsWith(".png") || path.endsWith(".jpg") || path.endsWith(".jpeg");
    }
}

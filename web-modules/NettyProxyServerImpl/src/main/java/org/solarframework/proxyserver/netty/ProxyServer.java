package org.solarframework.proxyserver.netty;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solarframework.proxyserver.obj.CallerHeaders;
import org.solarframework.proxyserver.obj.domains.BaseDomain;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.server.HttpServer;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;
import reactor.netty.tcp.SslProvider;

import javax.net.ssl.KeyManagerFactory;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.util.Map;

import static org.solarframework.proxyserver.netty.ProxyService.PROXYSERVER;

public class ProxyServer {
    private final Logger log = LoggerFactory.getLogger(ProxyServer.class);

    private DisposableServer server;

    private static final Map<String, String> MIME = Map.ofEntries(
            Map.entry(".html", "text/html"),
            Map.entry(".htm",  "text/html"),
            Map.entry(".css",  "text/css"),
            Map.entry(".js",   "application/javascript"),
            Map.entry(".mjs",  "application/javascript"),
            Map.entry(".json", "application/json"),
            Map.entry(".png",  "image/png"),
            Map.entry(".jpg",  "image/jpeg"),
            Map.entry(".jpeg", "image/jpeg"),
            Map.entry(".gif",  "image/gif"),
            Map.entry(".svg",  "image/svg+xml"),
            Map.entry(".ico",  "image/x-icon"),
            Map.entry(".woff", "font/woff"),
            Map.entry(".woff2","font/woff2"),
            Map.entry(".txt",  "text/plain")
    );

    public ProxyServer start() throws Exception {
        SslProvider ssl = SslProvider.builder().sslContext(sslContext()).build();
        this.server = HttpServer.create().port(443).secure(ssl).handle(this::route).bindNow();
        log.info("Started proxy server on port 443.");
        return this;
    }

    public ProxyServer stayConnected() {
        server.onDispose().block();
        return this;
    }

    public void stop() {
        if (server != null) {
            server.disposeNow();
            log.info("Stopped proxy server on port 443.");
        }
    }

    // --- routing ---

    private Publisher<Void> route(HttpServerRequest req, HttpServerResponse res) {
        String host = PROXYSERVER.stripPort(req.requestHeaders().get(HttpHeaderNames.HOST));
        BaseDomain<?> d = (host == null) ? null : PROXYSERVER.getDomainOfHost(host);
        if (d == null) return res.status(HttpResponseStatus.NOT_FOUND).send();

        ProxyService.PermitOutcome permit = PROXYSERVER.takePermit(clientIp(req), host, req.fullPath());
        if (!permit.allowed()) {
            log.warn("Refused: {} is not on the allow list for {}{}", clientIp(req), host, req.fullPath());
            ProxyService.notifyRefusal(clientIp(req), host, req.fullPath(), 0, permit.contributed(), true);
            return forbidden(res);
        }
        if (permit.waitSeconds() > 0) {
            log.warn("Rate limit reached by {} on {}{}", clientIp(req), host, req.fullPath());
            ProxyService.notifyRefusal(clientIp(req), host, req.fullPath(), permit.waitSeconds(), permit.contributed(), false);
            return tooManyRequests(res, permit.waitSeconds());
        }

        return d.isProxy() ? proxy(d.getPath(), req, res, permit.contributed()) : serveStatic(d, req, res);
    }

    /** NFR-10, NFR-20: worded identically to PluginController's own throttleMessage, so a caller cannot tell which
     *  layer refused it - keep the two in sync if either wording ever changes. */
    private Publisher<Void> tooManyRequests(HttpServerResponse res, long retryAfter) {
        return res.status(HttpResponseStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaderNames.RETRY_AFTER, String.valueOf(retryAfter))
                .header(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=utf-8")
                .sendString(Mono.just("Error: Too many requests. Please wait " + retryAfter + " seconds and try again."));
    }
    /** NFR-10, NFR-20: worded identically to PluginController's own refusalMessage - see the note on tooManyRequests. */
    private Publisher<Void> forbidden(HttpServerResponse res) {
        return res.status(HttpResponseStatus.FORBIDDEN)
                .header(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=utf-8")
                .sendString(Mono.just("Error: This endpoint is not available from your location."));
    }

    /** The proxy is the edge, so the socket address is the real caller and no header needs to be believed. */
    private static String clientIp(HttpServerRequest req) {
        InetSocketAddress address = req.remoteAddress();
        return address == null ? "unknown" : address.getAddress().getHostAddress();
    }

    private Publisher<Void> serveStatic(BaseDomain<?> domain, HttpServerRequest req, HttpServerResponse res) {
        return domain.fileFor(req.fullPath())
                .<Publisher<Void>>map(file -> res.header(HttpHeaderNames.CONTENT_TYPE, contentType(file)).sendFile(file))
                .orElseGet(() -> res.status(HttpResponseStatus.NOT_FOUND).send());
    }

    /** What the backend is told about the caller is the proxy's own (CallerHeaders); this server only ever listens on HTTPS. */
    private Publisher<Void> proxy(String target, HttpServerRequest req, HttpServerResponse res, boolean alreadyGuarded) {
        CallerHeaders caller = new CallerHeaders(clientIp(req), req.requestHeaders().get(HttpHeaderNames.HOST), "https", alreadyGuarded);
        return HttpClient.create()
                .headers(h -> caller.writeInto(h.set(req.requestHeaders())::set, h::remove))
                .request(req.method())
                .uri(target + req.uri())
                .send((cReq, out) -> out.send(req.receive().retain()))
                .responseConnection((cRes, conn) -> {
                    res.status(cRes.status());
                    cRes.responseHeaders().forEach(e -> res.addHeader(e.getKey(), e.getValue()));
                    return res.send(conn.inbound().receive().retain()).then();
                })
                .then();
    }

    private static String contentType(Path file) {
        String name = file.getFileName().toString();
        int dot = name.lastIndexOf('.');
        String ext = (dot == -1) ? "" : name.substring(dot).toLowerCase();
        return MIME.getOrDefault(ext, "application/octet-stream");
    }

    private SslContext sslContext() throws Exception {
        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (InputStream in = Files.newInputStream(Path.of("./config/certs/ssldomains.p12"))) {
            ks.load(in, "password".toCharArray());
        }
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, "password".toCharArray());
        return SslContextBuilder.forServer(kmf).build();
    }
}
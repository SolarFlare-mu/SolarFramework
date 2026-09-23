package org.solarframework.ai.spring;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.solarframework.ai.IAIService;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AIServiceRetryTest {

    // Spring AI retries a 5xx ten times with back-off by default; a slow local model turned that into minutes of waiting
    @Test
    void aFailedModelCallIsNotRetried() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", ex -> {
            if (ex.getRequestURI().getPath().endsWith("/chat/completions")) calls.incrementAndGet();
            ex.getRequestBody().readAllBytes();
            ex.sendResponseHeaders(500, -1);
            ex.close();
        });
        server.start();
        try {
            IAIService service = new AIManager().makeNewService("retry", "http://127.0.0.1:" + server.getAddress().getPort(), "N/A", "any-model");
            service.setTimeoutSeconds(5);
            assertThrows(RuntimeException.class, () -> service.createChatbot().build().prompt("Hi"));
            assertEquals(1, calls.get());
        } finally {
            server.stop(0);
        }
    }
}

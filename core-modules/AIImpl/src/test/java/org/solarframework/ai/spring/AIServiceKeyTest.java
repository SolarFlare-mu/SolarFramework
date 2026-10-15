package org.solarframework.ai.spring;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/** A stub endpoint that, like a cloud provider, answers its model list only with the right key. */
class AIServiceKeyTest {

    private static final String NAME = "SOLAR_AI_SERVICE_TEST_KEY";
    private HttpServer server;
    private AIService service;

    @BeforeEach void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/models", x -> {
            boolean ok = "Bearer gsk_secret".equals(x.getRequestHeaders().getFirst("Authorization"));
            byte[] body = (ok ? "{\"object\":\"list\",\"data\":[{\"id\":\"cloud-model\"}]}" : "{\"error\":{\"message\":\"Invalid API Key\"}}").getBytes(StandardCharsets.UTF_8);
            x.sendResponseHeaders(ok ? 200 : 401, body.length);
            x.getResponseBody().write(body);
            x.close();
        });
        server.start();
        service = TestAI.service("cloud", "http://127.0.0.1:" + server.getAddress().getPort(), "cloud-model");
    }

    @AfterEach void stop() { server.stop(0); System.clearProperty(NAME); }

    @Test void aKeyNamingAVariableIsSentAsItsValue() {
        System.setProperty(NAME, "gsk_secret");
        service.setApiKey("${" + NAME + "}");
        assertTrue(service.isAvailable());
        assertEquals(java.util.List.of("cloud-model"), service.getAvailableModels(), "the standard list stands in for LM Studio's");
        assertEquals("${" + NAME + "}", service.getApiKey(), "the reference is kept, so saving never writes the secret");
    }

    @Test void aPlainKeyStillWorks() {
        service.setApiKey("gsk_secret");
        assertTrue(service.isAvailable());
    }

    @Test void anUnsetVariableIsNoKeyAtAll() {
        service.setApiKey("${" + NAME + "}");
        assertFalse(service.isAvailable());
    }
}

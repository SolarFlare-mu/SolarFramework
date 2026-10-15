package org.solarframework.ai.lmstudio;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Runs against a stub server, so it never needs LM Studio. */
class LMStudioModelManagerTest {

    private static final String MODELS = """
            {"data":[
              {"id":"openai/gpt-oss-20b","object":"model","type":"llm","publisher":"openai","arch":"gpt-oss",
               "quantization":"MXFP4","state":"loaded","max_context_length":131072,"loaded_context_length":8192,
               "capabilities":["tool_use"]},
              {"id":"google/gemma-3-12b","object":"model","type":"llm","publisher":"google","arch":"gemma3",
               "quantization":"Q4_K_M","state":"not-loaded","max_context_length":131072,"loaded_context_length":0}
            ]}""";

    private HttpServer server;
    private final Map<String, String> routes = new HashMap<>();
    private LMStudioModelManager manager;
    private String authorization;

    @BeforeEach void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            authorization = exchange.getRequestHeaders().getFirst("Authorization");
            // LM Studio answers 200 even for endpoints it does not have, with the failure in the body.
            String body = routes.getOrDefault(exchange.getRequestURI().getPath(),
                    "{\"error\":\"Unexpected endpoint or method. (" + exchange.getRequestURI().getPath() + ")\"}");
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        manager = new LMStudioModelManager("http://127.0.0.1:" + server.getAddress().getPort());
    }

    @AfterEach void stop() { server.stop(0); }

    @Test void readsStateCapabilitiesAndContext() {
        routes.put("/api/v0/models", MODELS);

        assertEquals(2, manager.list().size());
        LMStudioModelManager.Model oss = manager.get("openai/gpt-oss-20b").orElseThrow();
        assertTrue(oss.isLoaded());
        assertEquals(8192, oss.usableContext(), "a model can load far below its maximum");
        assertEquals("MXFP4", oss.quantization());
        assertTrue(oss.capabilities().contains("tool_use"));
    }

    @Test void anUnloadedModelReportsItsMaximumInstead() {
        routes.put("/api/v0/models", MODELS);
        LMStudioModelManager.Model gemma = manager.get("google/gemma-3-12b").orElseThrow();

        assertFalse(gemma.isLoaded());
        assertEquals(131072, gemma.usableContext());
        assertTrue(gemma.capabilities().isEmpty());
    }

    /** The regression that matters: a 200 carrying an error body is a failure, not an empty success. */
    @Test void treatsA200ErrorBodyAsFailure() {
        routes.put("/v1/models", "{\"error\":\"Unexpected endpoint or method.\"}");
        assertFalse(manager.isAvailable());
        assertTrue(manager.list().isEmpty());
    }

    @Test void availabilityFollowsTheProbeEndpoint() {
        routes.put("/v1/models", "{\"data\":[]}");
        assertTrue(manager.isAvailable());
    }

    @Test void unknownModelIsAbsentRatherThanNull() {
        routes.put("/api/v0/models", MODELS);
        assertTrue(manager.get("nope").isEmpty());
        assertTrue(manager.get(null).isEmpty());
    }

    @Test void unreachableServerDegradesInsteadOfThrowing() {
        LMStudioModelManager dead = new LMStudioModelManager("http://127.0.0.1:1");
        assertFalse(dead.isAvailable());
        assertTrue(dead.list().isEmpty());
        assertTrue(dead.get("anything").isEmpty());
    }

    @Test void theKeyIsSentAsABearerToken() {
        routes.put("/v1/models", "{\"data\":[]}");
        new LMStudioModelManager(manager.baseUrl(), "gsk_test").isAvailable();
        assertEquals("Bearer gsk_test", authorization, "a cloud endpoint refuses its model list without the key");
    }

    @Test void noKeySendsNoHeader() {
        routes.put("/v1/models", "{\"data\":[]}");
        new LMStudioModelManager(manager.baseUrl(), "N/A").isAvailable();
        assertNull(authorization);
    }

    /** A server that is not LM Studio has no /api/v0: its models come from the standard list, with nothing known about them. */
    @Test void modelsFallBackToTheStandardList() {
        routes.put("/v1/models", "{\"object\":\"list\",\"data\":[{\"id\":\"openai/gpt-oss-120b\",\"object\":\"model\",\"owned_by\":\"OpenAI\"}]}");
        LMStudioModelManager.Model m = manager.get("openai/gpt-oss-120b").orElseThrow();
        assertNull(m.state());
        assertTrue(m.capabilities().isEmpty());
    }

    @Test void onlyAServerOnThisMachineIsLocal() {
        assertTrue(manager.isLocal());
        assertTrue(new LMStudioModelManager("http://localhost:1234").isLocal());
        assertFalse(new LMStudioModelManager("https://api.groq.com/openai").isLocal());
    }

    /** The lms CLI loads models on this machine, so it must never run for a model some other server holds. */
    @Test void aRemoteServerNeverLoadsThroughTheCli() {
        LMStudioModelManager remote = new LMStudioModelManager("http://192.0.2.1:1");
        assertFalse(remote.load("openai/gpt-oss-120b", null, null));
        assertFalse(remote.unload("openai/gpt-oss-120b"));
    }

    /** Gemini's OpenAI-compatible base carries its own version; adding /v1 to it is a 404, but Groq's does not carry one. */
    @Test void aVersionedBaseUrlIsFollowedDirectly() {
        assertEquals("/models", new LMStudioModelManager("https://api.openai.com/v1").apiPath("/models"));
        assertEquals("/chat/completions", new LMStudioModelManager("https://generativelanguage.googleapis.com/v1beta/openai/").apiPath("/chat/completions"));
        assertEquals("/v1/chat/completions", new LMStudioModelManager("https://api.groq.com/openai").apiPath("/chat/completions"));
        assertEquals("/v1/models", new LMStudioModelManager("http://localhost:1234").apiPath("/models"));
    }

    @Test void aVersionedBaseUrlListsModelsFromItsOwnPath() {
        routes.put("/v1beta/openai/models", "{\"data\":[{\"id\":\"gemini-2.5-flash\"}]}");
        LMStudioModelManager gemini = new LMStudioModelManager(manager.baseUrl() + "/v1beta/openai");
        assertTrue(gemini.isAvailable());
        assertTrue(gemini.get("gemini-2.5-flash").isPresent());
    }

    @Test void trailingSlashInBaseUrlIsHarmless() {
        assertEquals("http://localhost:1234", new LMStudioModelManager("http://localhost:1234/").baseUrl());
        assertEquals("http://localhost:1234", new LMStudioModelManager(null).baseUrl());
    }
}

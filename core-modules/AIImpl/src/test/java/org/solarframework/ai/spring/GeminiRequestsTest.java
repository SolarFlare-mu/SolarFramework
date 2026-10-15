package org.solarframework.ai.spring;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class GeminiRequestsTest {

    private static String adapted(String json) { return new String(GeminiRequests.adapt(json.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8); }

    /** The regression: Gemini 3 answers 400 to a tool call in the history that carries no signature. */
    @Test void aToolCallWithoutASignatureIsGivenThePlaceholder() {
        String out = adapted("{\"messages\":[{\"role\":\"user\",\"content\":\"hi\"},{\"role\":\"assistant\",\"tool_calls\":[{\"id\":\"1\",\"type\":\"function\",\"function\":{\"name\":\"f\",\"arguments\":\"{}\"}}]}]}");
        assertTrue(out.contains("\"extra_content\":{\"google\":{\"thought_signature\":\"skip_thought_signature_validator\"}}"));
        assertTrue(out.contains("\"name\":\"f\""), "the call itself is untouched");
    }

    @Test void aRealSignatureIsNeverOverwritten() {
        String out = adapted("{\"messages\":[{\"role\":\"assistant\",\"tool_calls\":[{\"id\":\"1\",\"extra_content\":{\"google\":{\"thought_signature\":\"real\"}}}]}]}");
        assertTrue(out.contains("\"real\"") && !out.contains("skip_thought_signature_validator"));
    }

    /** Gemini answers 400 "Unknown name frequency_penalty" however small the value. */
    @Test void penaltiesAreDroppedAndTheRestKept() {
        String out = adapted("{\"model\":\"m\",\"temperature\":0.4,\"frequency_penalty\":0.25,\"presence_penalty\":0.25,\"messages\":[]}");
        assertFalse(out.contains("penalty"));
        assertTrue(out.contains("\"temperature\":0.4") && out.contains("\"model\":\"m\""));
    }

    @Test void aBodyWithNothingToAdaptComesBackEquivalent() {
        assertEquals("{\"model\":\"m\",\"messages\":[{\"role\":\"user\",\"content\":\"hi\"}]}", adapted("{\"model\":\"m\",\"messages\":[{\"role\":\"user\",\"content\":\"hi\"}]}"));
        assertEquals("{}", adapted("{}"));
    }
}

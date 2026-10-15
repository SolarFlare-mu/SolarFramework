package org.solarframework.ai.spring;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.solarframework.ai.AITool;
import org.solarframework.ai.obj.ChatMessage;
import org.solarframework.ai.obj.Conversation;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** Proves a tool-using answer survives Gemini's second request, the one that carries the tool call back. Self-skips without GEMINI_API_KEY. */
@Tag("live")
class GeminiLiveTest {

    private static AIService service;

    @BeforeAll static void connect() {
        assumeTrue(System.getenv("GEMINI_API_KEY") != null, "GEMINI_API_KEY is not set");
        service = TestAI.service("gemini-test", "https://generativelanguage.googleapis.com/v1beta/openai", "gemini-flash-latest");
        service.setApiKey("${GEMINI_API_KEY}");
    }

    @Test void aToolCallGoesBackToGeminiAndTheAnswerUsesItsResult() {
        Vault vault = new Vault();
        Conversation c = service.createChatbot().systemPrompt("Use your tools, never guess a value a tool can give you.").tools(vault).maxSteps(4).build().startConversation();
        String reply = c.send("What is the access code for the archive room?");

        assertEquals(1, vault.calls.get(), "the tool must have been called once");
        assertTrue(reply.contains(Vault.CODE), "the code can only come from the tool; got: " + reply);
        assertTrue(c.getMessages().stream().anyMatch(ChatMessage::hasToolCalls), "the tool call is in the history that was sent back");
    }

    public static class Vault {
        static final String CODE = "ZX-4417-QQ";
        final AtomicInteger calls = new AtomicInteger();

        @AITool(description = "Returns the secret access code for a named room. The code cannot be known any other way.")
        public String getAccessCode(String room) { calls.incrementAndGet(); return CODE; }
    }
}

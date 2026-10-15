package org.solarframework.ai.spring;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Makes a request Spring AI's OpenAI client writes acceptable to Gemini's OpenAI-compatible endpoint, which differs in two ways:
 * it refuses {@code frequency_penalty} and {@code presence_penalty} as unknown fields, and Gemini 3 answers 400 to a tool call
 * in the history that has no "thought signature" (the client has no field to keep the real one). Google documents a
 * placeholder that tells it to skip that check, so every tool call going out without a signature is given it.
 * Only attached for Google's endpoint, see {@link AIService#chatModel()}.
 */
class GeminiRequests implements ClientHttpRequestInterceptor {

    private static final JsonObject SKIP_CHECK = JsonParser.parseString("{\"google\":{\"thought_signature\":\"skip_thought_signature_validator\"}}").getAsJsonObject();

    @Override public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        byte[] adapted = adapt(body);
        request.getHeaders().setContentLength(adapted.length);
        return execution.execute(request, adapted);
    }

    static byte[] adapt(byte[] body) {
        JsonElement parsed = JsonParser.parseString(new String(body, StandardCharsets.UTF_8));
        if (!parsed.isJsonObject()) return body;
        JsonObject request = parsed.getAsJsonObject();
        request.remove("frequency_penalty");
        request.remove("presence_penalty");
        JsonElement messages = request.get("messages");
        if (messages != null && messages.isJsonArray()) for (JsonElement message : messages.getAsJsonArray()) signToolCalls(message.getAsJsonObject());
        return request.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static void signToolCalls(JsonObject message) {
        JsonElement calls = message.get("tool_calls");
        if (calls != null && calls.isJsonArray()) for (JsonElement call : calls.getAsJsonArray()) if (!call.getAsJsonObject().has("extra_content")) call.getAsJsonObject().add("extra_content", SKIP_CHECK.deepCopy());
    }
}

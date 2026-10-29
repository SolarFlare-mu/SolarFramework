package org.solarframework.ai.spring;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.solarframework.ai.AITool;
import org.solarframework.ai.AIToolSpec;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.execution.ToolExecutionException;
import org.springframework.ai.tool.method.MethodToolCallback;
import org.springframework.ai.util.json.schema.JsonSchemaGenerator;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.solarframework.json.JSONItem.SimpleGSON;

/**
 * Turns objects into Spring AI tool callbacks.
 * <p>An {@link AIToolSpec} is one tool, described by its own data. Methods annotated {@link AITool} are described from the annotation and their signature. An
 * object with none is handed to Spring AI untouched, so an object using its native {@code @Tool}
 * annotation keeps working — that fallback is the only reason a consumer would ever need the
 * Spring AI classpath.
 */
public class ToolAdapter {

    public List<ToolCallback> callbacks(Object... toolObjects) {
        List<ToolCallback> out = new ArrayList<>();
        if (toolObjects == null) return out;
        for (Object o : toolObjects) if (o != null) out.addAll(callbacksOf(o));
        return out;
    }

    private List<ToolCallback> callbacksOf(Object o) {
        if (o instanceof AIToolSpec spec) return List.of(callbackFor(spec));
        List<ToolCallback> own = new ArrayList<>();
        for (Method m : o.getClass().getMethods()) {
            AITool a = m.getAnnotation(AITool.class);
            if (a != null) own.add(callbackFor(o, m, a));
        }
        if (!own.isEmpty()) return own;
        try {
            return List.of(ToolCallbacks.from(o));
        } catch (RuntimeException e) {
            return List.of(); // Spring AI throws when an object declares no tools; that is not an error here
        }
    }

    private ToolCallback callbackFor(Object target, Method method, AITool a) {
        ToolDefinition definition = ToolDefinition.builder()
                .name(a.name().isBlank() ? method.getName() : a.name())
                .description(a.description())
                .inputSchema(JsonSchemaGenerator.generateForMethodInput(method))
                .build();
        return MethodToolCallback.builder().toolDefinition(definition).toolMethod(method).toolObject(target).build();
    }

    private ToolCallback callbackFor(AIToolSpec spec) {
        JsonObject properties = new JsonObject(), schema = new JsonObject(), text = new JsonObject();
        text.addProperty("type", "string");
        for (String p : spec.parameters()) properties.add(p, text);
        schema.addProperty("type", "object");
        schema.add("properties", properties);
        schema.add("required", SimpleGSON.toJsonTree(spec.parameters()));
        ToolDefinition definition = ToolDefinition.builder().name(spec.name()).description(spec.description()).inputSchema(schema.toString()).build();
        return new ToolCallback() {
            @Override public ToolDefinition getToolDefinition() { return definition; }
            @Override public String call(String toolInput) {
                try {
                    JsonObject given = toolInput == null || toolInput.isBlank() ? new JsonObject() : JsonParser.parseString(toolInput).getAsJsonObject();
                    Map<String, String> arguments = new HashMap<>();
                    for (String p : spec.parameters()) if (given.has(p) && !given.get(p).isJsonNull()) arguments.put(p, given.get(p).getAsString());
                    return spec.run().apply(arguments);
                } catch (RuntimeException e) {
                    throw new ToolExecutionException(definition, e);
                }
            }
        };
    }
}

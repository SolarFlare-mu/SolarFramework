package org.solarframework.ai;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * A tool the model may call, described by data instead of an {@link AITool} method: a name, what it is for, the names
 * of the text values the model hands it, and what runs. A tool object holds a fixed set of methods; this is for tools
 * whose number and content are only known at run time, such as ones an administrator writes on a page.
 * <p>Every parameter is text; the function gets them by name, with null for one the model left out.
 */
public record AIToolSpec(String name, String description, List<String> parameters, Function<Map<String, String>, String> run) {

    public AIToolSpec { parameters = List.copyOf(parameters); }

    /** Whether the tools held already include this one: objects of one class are the same tool set, but specs are many of one class. */
    public static boolean isHeldIn(List<Object> held, Object tool) {
        return tool instanceof AIToolSpec ? held.contains(tool) : held.stream().anyMatch(o -> o.getClass().equals(tool.getClass()));
    }
}

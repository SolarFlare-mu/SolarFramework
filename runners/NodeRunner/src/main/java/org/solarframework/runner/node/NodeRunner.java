package org.solarframework.runner.node;

import org.solarframework.runner.AppRunner;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

// A Node.js program: script(dir, "server.js") is node server.js, and npm(dir, "start") runs a package.json script, which is how most Node web apps are started
public class NodeRunner extends AppRunner {
    private static final boolean WINDOWS = System.getProperty("os.name").toLowerCase().contains("win");
    private final List<String> target;
    private final boolean npm;
    private String node = "node";

    private NodeRunner(Path dir, boolean npm, List<String> target) {
        super(dir);
        this.npm = npm;
        this.target = target;
    }

    public static NodeRunner script(Path dir, String script, String... args) { return new NodeRunner(dir, false, join(List.of(script), args)); }

    public static NodeRunner npm(Path dir, String script, String... args) { return new NodeRunner(dir, true, join(List.of("run", script, "--"), args)); }

    // Overrides the node executable, when it is not on the PATH; blank keeps the default
    public NodeRunner node(String executable) { node = executable == null || executable.isBlank() ? "node" : executable; return this; }

    // npm is a batch file on Windows, which a process cannot be started from by its bare name
    public String getLauncher() { return npm ? (WINDOWS ? "npm.cmd" : "npm") : node; }

    @Override protected List<String> command() { return join(List.of(getLauncher()), target.toArray(String[]::new)); }

    private static List<String> join(List<String> head, String... tail) {
        List<String> out = new ArrayList<>(head);
        out.addAll(List.of(tail));
        return out;
    }
}

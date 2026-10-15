package org.solarframework.runner;

import java.nio.file.Path;
import java.util.List;

// Any program given its whole command line, for a runtime with no runner of its own (node, a native executable); shell(...) takes one line the way a terminal would, e.g. "npm start"
public class CommandRunner extends AppRunner {
    private static final boolean WINDOWS = System.getProperty("os.name").toLowerCase().contains("win");
    private final List<String> command;

    public CommandRunner(Path dir, String... command) {
        super(dir);
        this.command = List.of(command);
    }

    public static CommandRunner shell(Path dir, String line) {
        return WINDOWS ? new CommandRunner(dir, "cmd", "/c", line) : new CommandRunner(dir, "sh", "-c", line);
    }

    @Override protected List<String> command() { return command; }
}

package org.solarframework.core.util;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ProcessUtils {

    public record Result(int code, String message) {
        public boolean ok() { return code == 0; }
    }

    /**
     * Runs an outside command and waits for it - {@code stdoutTo} and {@code stdinFrom} are the two directions, a
     * dump writing its file and a restore reading one. Secrets belong in {@code environment}: an argument is visible
     * to anyone who can list processes. A missing tool comes back as a failed {@link Result}, never an exception.
     */
    public static Result run(List<String> command, Map<String, String> environment, Path stdoutTo, Path stdinFrom, long timeoutSeconds) {
        try {
            // the streams stay separate: merged, a complaint from the tool would be written into the middle of the dump file it is writing
            ProcessBuilder builder = new ProcessBuilder(command);
            if (environment != null) builder.environment().putAll(environment);
            if (stdoutTo != null) builder.redirectOutput(stdoutTo.toFile());
            if (stdinFrom != null) builder.redirectInput(stdinFrom.toFile());
            Process process = builder.start();
            // read before waiting: a process whose output nobody drains fills its pipe and stops
            String said = stdoutTo != null ? "" : new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            String complained = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
            if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return new Result(-1, command.getFirst() + " did not finish within " + timeoutSeconds + " seconds and was stopped.");
            }
            return new Result(process.exitValue(), (said + complained).trim());
        } catch (Exception e) {
            return new Result(-1, command.isEmpty() ? e.getMessage() : command.getFirst() + " could not be run: " + e.getMessage());
        }
    }

    /**
     * Relaunches the jar {@code mainClass} was loaded from, reusing this JVM's own options, then stops this process.
     * Used when code the JVM already loaded has been regenerated on disk and only a fresh start can pick it up.
     */
    public static void restartApplication(Class<?> mainClass, String... args) throws Exception {
        List<String> command = new ArrayList<>();
        command.add(System.getProperty("java.home") + File.separator + "bin" + File.separator + "java");
        command.addAll(ManagementFactory.getRuntimeMXBean().getInputArguments());
        command.add("-jar");
        command.add(new File(mainClass.getProtectionDomain().getCodeSource().getLocation().toURI()).getAbsolutePath());
        if (args != null) command.addAll(Arrays.asList(args));
        new ProcessBuilder(command).inheritIO().start();
        System.exit(0);
    }
}

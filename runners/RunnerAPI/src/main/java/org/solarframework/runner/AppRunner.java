package org.solarframework.runner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BooleanSupplier;
import java.util.stream.Stream;

/**
 * One outside program kept running for as long as its host: started, waited on until it is ready, and stopped
 * with everything it spawned. A plain object, like the AI and database stacks, so it also runs where there is no
 * application context. Each runtime only says what its command line is.
 */
public abstract class AppRunner {
    private static final Logger log = LoggerFactory.getLogger(AppRunner.class);
    protected final Path dir;
    private final Map<String, String> env = new LinkedHashMap<>();
    private volatile Process process;
    private volatile boolean stopping;

    protected AppRunner(Path dir) { this.dir = dir.toAbsolutePath().normalize(); }

    /** The command line, program first. */
    protected abstract List<String> command();

    /** The short name its log lines carry; the folder's name unless a runtime knows better. */
    public String name() { return dir.getFileName().toString(); }

    public AppRunner env(String key, String value) { env.put(key, value); return this; }

    /**
     * Its output is logged line by line under this runner's logger, tagged with {@link #name()}, so it has timestamps and
     * reaches the host's log files. An exit nobody asked for is logged with its code. Does nothing while it is already running.
     */
    public AppRunner start() throws IOException {
        if (isAlive()) return this;
        String cmd = String.join(" ", command());
        log.info("Running {} in {}...", cmd, dir);
        ProcessBuilder b = new ProcessBuilder(command()).directory(dir.toFile()).redirectErrorStream(true);
        b.environment().putAll(env);
        stopping = false;
        Process p = b.start();
        process = p;
        log.info("Started {} (pid {})", name(), p.pid());
        Thread.ofVirtual().name("runner-" + name() + "-output").start(() -> relayOutput(p));
        p.onExit().thenAccept(this::logExit);
        return this;
    }

    private void relayOutput(Process p) {
        Logger out = LoggerFactory.getLogger(getClass());
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            for (String line; (line = reader.readLine()) != null; ) if (!line.isBlank()) out.info("[{}] {}", name(), line);
        } catch (IOException ignored) {} // the stream closes when the program ends
    }

    private void logExit(Process p) {
        if (stopping) return;
        int code = p.exitValue();
        if (code == 0) log.info("{} exited (pid {}, code 0)", name(), p.pid());
        else log.warn("{} exited unexpectedly (pid {}, code {}); its last output is above", name(), p.pid(), code);
    }

    /** Asks {@code ready} every half second; false when the program exits first or the time runs out. */
    public boolean awaitReady(BooleanSupplier ready, Duration timeout) {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            if (!isAlive()) return false;
            if (ready.getAsBoolean()) return true;
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    /**
     * Stops the program and what it started: a launcher such as a venv's python.exe runs the real program as its child.
     * The tree is taken once, before anything dies, since an orphaned child no longer shows among the descendants.
     * Where termination is graceful (not Windows) each gets 10 seconds to shut down before it is killed.
     */
    public void stop() {
        Process p = process;
        if (p == null || !p.isAlive()) return;
        stopping = true;
        log.info("Stopping {} (pid {})...", name(), p.pid());
        List<ProcessHandle> tree = Stream.concat(p.descendants(), Stream.of(p.toHandle())).toList();
        tree.forEach(ProcessHandle::destroy);
        try {
            CompletableFuture.allOf(tree.stream().map(ProcessHandle::onExit).toArray(CompletableFuture[]::new)).get(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException | TimeoutException ignored) {}
        List<ProcessHandle> stubborn = tree.stream().filter(ProcessHandle::isAlive).toList();
        if (!stubborn.isEmpty()) log.warn("{}: {} process(es) still running after 10 s, killed", name(), stubborn.size());
        stubborn.forEach(ProcessHandle::destroyForcibly);
        log.info("Stopped {}", name());
    }

    public boolean isAlive() { return process != null && process.isAlive(); }

    /** Null while it runs or before it was started. */
    public Integer getExitCode() { return process == null || process.isAlive() ? null : process.exitValue(); }

    public Path getDirectory() { return dir; }
}

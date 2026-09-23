package org.solarframework.runner;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class AppRunnerTest {
    private static final String JAVA = Path.of(System.getProperty("java.home"), "bin", "java").toString();
    /** Starts the same program with an argument, so the child does not start one of its own. */
    private static final String CHILD = "if (a.length == 0) new ProcessBuilder(\"" + JAVA.replace("\\", "\\\\") + "\", \"P.java\", \"child\").start();";

    /** A single-file Java program: the one runtime every machine building this has. */
    private static AppRunner program(Path dir, String body) throws Exception {
        Files.writeString(dir.resolve("P.java"), "class P { public static void main(String[] a) throws Exception { " + body + " } }");
        return new AppRunner(dir) {
            @Override protected List<String> command() { return List.of(JAVA, "P.java"); }
        };
    }

    @Test
    void stopEndsTheProgramAndWhatItStarted(@TempDir Path dir) throws Exception {
        AppRunner r = program(dir, CHILD + " Thread.sleep(60000);").start();
        assertTrue(r.awaitReady(() -> ProcessHandle.current().descendants().count() >= 2, Duration.ofSeconds(30)), "the program and its child should be running");
        List<ProcessHandle> started = ProcessHandle.current().descendants().toList();
        r.stop();
        Thread.sleep(500);
        assertTrue(started.stream().noneMatch(ProcessHandle::isAlive));
        assertFalse(r.isAlive());
    }

    /** The parent exits on SIGTERM, the child's shutdown hook hangs: the child is orphaned and must still be killed. */
    @Test
    void stopKillsAChildThatOutlivesItsParent(@TempDir Path dir) throws Exception {
        assumeTrue(ProcessHandle.current().supportsNormalTermination(), "destroy is already forcible here");
        AppRunner r = program(dir, CHILD + " else { Runtime.getRuntime().addShutdownHook(new Thread(() -> { try { Thread.sleep(60000); } catch (InterruptedException e) {} })); new java.io.File(\"ready\").createNewFile(); } Thread.sleep(60000);").start();
        assertTrue(r.awaitReady(() -> Files.exists(dir.resolve("ready")), Duration.ofSeconds(30)), "the child should have its hook in place");
        List<ProcessHandle> started = ProcessHandle.current().descendants().toList();
        r.stop();
        Thread.sleep(500);
        assertTrue(started.stream().noneMatch(ProcessHandle::isAlive));
    }

    @Test
    void waitingGivesUpWhenTheProgramExits(@TempDir Path dir) throws Exception {
        AppRunner r = program(dir, "System.exit(3);").start();
        assertFalse(r.awaitReady(() -> false, Duration.ofSeconds(30)));
        assertEquals(3, r.getExitCode());
    }
}

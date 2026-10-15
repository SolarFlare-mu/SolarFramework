package org.solarframework.runner;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class CommandRunnerTest {

    @Test
    void shellRunsTheLineInTheFolderThroughThePlatformsShell(@TempDir Path dir) throws Exception {
        AppRunner r = CommandRunner.shell(dir, "echo hi> made.txt && exit 3").start();
        assertFalse(r.awaitReady(() -> false, Duration.ofSeconds(30)));
        assertEquals(3, r.getExitCode());
        assertTrue(Files.exists(dir.resolve("made.txt")));
    }
}

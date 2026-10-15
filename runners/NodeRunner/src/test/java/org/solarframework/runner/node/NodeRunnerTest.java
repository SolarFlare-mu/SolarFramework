package org.solarframework.runner.node;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.solarframework.runner.AppRunner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class NodeRunnerTest {

    @Test
    void npmRunsThePackagesScriptInItsFolder(@TempDir Path dir) throws Exception {
        assumeTrue(nodeInstalled(), "node is not on the PATH");
        Files.writeString(dir.resolve("package.json"), "{\"scripts\":{\"start\":\"node -e \\\"require('fs').writeFileSync('made.txt','');process.exit(3)\\\"\"}}");
        AppRunner r = NodeRunner.npm(dir, "start").start();
        assertFalse(r.awaitReady(() -> false, Duration.ofSeconds(60)));
        assertEquals(3, r.getExitCode());
        assertTrue(Files.exists(dir.resolve("made.txt")));
    }

    private static boolean nodeInstalled() {
        try {
            return new ProcessBuilder("node", "--version").start().waitFor() == 0;
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }
}

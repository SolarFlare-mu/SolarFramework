package org.solarframework.runner.python;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PythonRunnerTest {

    @Test
    void theFoldersOwnVirtualEnvironmentIsPreferred(@TempDir Path dir) throws Exception {
        String bin = System.getProperty("os.name").toLowerCase().contains("win") ? "Scripts/python.exe" : "bin/python";
        Path python = dir.resolve(".venv").resolve(bin);
        Files.createDirectories(python.getParent());
        Files.writeString(python, "");
        assertEquals(python.toString(), PythonRunner.module(dir, "app.main").getInterpreter());
        assertEquals("C:/py/python.exe", PythonRunner.module(dir, "app.main").interpreter("C:/py/python.exe").getInterpreter());
    }

    @Test
    void withoutOneItFallsBackToThePath(@TempDir Path dir) {
        assertTrue(PythonRunner.script(dir, "main.py").getInterpreter().startsWith("python"));
    }
}

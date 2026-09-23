package org.solarframework.core.util;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** The backup module leans on these two answers: a tool that is not installed must be a refusal, and a dump file must hold the dump alone. */
class ProcessUtilsTest {

    private static final String JAVA = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";

    @Test
    void aMissingToolIsARefusalNamingIt() {
        ProcessUtils.Result result = ProcessUtils.run(List.of("mysqldump-that-is-not-installed", "--version"), null, null, null, 10);
        assertFalse(result.ok());
        assertTrue(result.message().contains("mysqldump-that-is-not-installed"), result.message());
    }

    @Test
    void readsBackWhatTheToolSaid() {
        ProcessUtils.Result result = ProcessUtils.run(List.of(JAVA, "-version"), null, null, null, 60);
        assertTrue(result.ok());
        assertTrue(result.message().toLowerCase().contains("version"), result.message());
    }

    /** Merged streams would write a tool's complaint into the middle of the dump file it is producing. */
    @Test
    void aComplaintNeverLandsInTheOutputFile() throws Exception {
        Path out = Files.createTempFile("tool-output", ".txt");
        ProcessUtils.Result result = ProcessUtils.run(List.of(JAVA, "-version"), Map.of(), out, null, 60);
        assertTrue(result.message().toLowerCase().contains("version"), "what it said comes back through the result");
        assertEquals("", Files.readString(out).trim(), "and not into the file");
        Files.deleteIfExists(out);
    }
}

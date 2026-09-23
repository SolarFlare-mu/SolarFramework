package org.solarframework.core.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

class HostsFileTest {
    @TempDir Path directory;

    private HostsFile hostsAt(Path path) { return new HostsFile(path, "# test-owner", LoggerFactory.getLogger(HostsFileTest.class)); }


    @Test void addingHostsPreservesExistingEntriesAndListsOnlyOwnedHosts() throws Exception {
        Path path = directory.resolve("hosts");
        Files.writeString(path, "127.0.0.1 localhost\n");
        HostsFile hosts = hostsAt(path);
        hosts.add("127.0.0.2", List.of("first.test", "second.test"));
        hosts.add("127.0.0.3", List.of("first.test"));
        assertEquals(List.of("first.test", "second.test"), hosts.getHosts());
        assertEquals(List.of("127.0.0.1 localhost", "", "127.0.0.2 first.test # test-owner", "127.0.0.2 second.test # test-owner"), Files.readAllLines(path));
    }
    @Test void removalKeepsEntriesTheCallerDoesNotOwn() throws Exception {
        Path path = directory.resolve("hosts");
        Files.writeString(path, "127.0.0.1 localhost\n127.0.0.1 example.test # test-owner\n127.0.0.2 example.test\n");
        hostsAt(path).remove(List.of("example.test"));
        assertEquals(List.of("127.0.0.1 localhost", "127.0.0.2 example.test"), Files.readAllLines(path));
    }
    @Test void clearingKeepsOtherOwnersAndComments() throws Exception {
        Path path = directory.resolve("hosts");
        Files.writeString(path, "# machine defaults\n127.0.0.1 mine.test # test-owner\n127.0.0.2 theirs.test # someone-else\n");
        hostsAt(path).clear();
        assertEquals(List.of("# machine defaults", "127.0.0.2 theirs.test # someone-else"), Files.readAllLines(path));
    }
    /** Extraction keeps the existing substring rule until hostname parsing is deliberately changed. */
    @Test void aSubstringMatchStillPreventsAddingAndAllowsRemovingATaggedLine() throws Exception {
        Path path = directory.resolve("hosts");
        String original = "127.0.0.1 sub.example.test # test-owner\n";
        Files.writeString(path, original);
        HostsFile hosts = hostsAt(path);
        hosts.add("127.0.0.2", List.of("example.test"));
        assertEquals(original, Files.readString(path));
        hosts.remove(List.of("example.test"));
        assertTrue(Files.readAllLines(path).isEmpty());
    }
    @Test void aMissingFileReadsEmptyButWritesReportFailure() {
        Path path = directory.resolve("missing");
        HostsFile hosts = hostsAt(path);
        assertTrue(hosts.getHosts().isEmpty());
        assertThrows(IOException.class, () -> hosts.add("127.0.0.1", List.of("example.test")));
        assertThrows(IOException.class, () -> hosts.remove(List.of("example.test")));
        assertThrows(IOException.class, hosts::clear);
        assertFalse(Files.exists(path));
    }
    /** An empty file currently has no last line from which the separator can be chosen. */
    @Test void addingToAnEmptyFileKeepsTheExistingFailure() throws Exception {
        Path path = Files.createFile(directory.resolve("empty"));
        assertThrows(NoSuchElementException.class, () -> hostsAt(path).add("127.0.0.1", List.of("example.test")));
        assertEquals(0, Files.size(path));
    }
}

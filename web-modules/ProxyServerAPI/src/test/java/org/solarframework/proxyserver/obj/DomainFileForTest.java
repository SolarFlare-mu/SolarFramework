package org.solarframework.proxyserver.obj;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.solarframework.proxyserver.obj.domains.Domain;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/** Every implementation serves a domain's folder through {@code fileFor}, so what it refuses, none of them serves. */
class DomainFileForTest {

    @TempDir Path root;
    private Domain site;

    @BeforeEach void makeSite() throws Exception {
        Path folder = Files.createDirectories(root.resolve("site"));
        write(folder, "index.html");
        write(folder, "docs/index.html");
        write(folder, "a..b.txt");
        write(folder, ".env");
        write(folder, ".git/config");
        write(folder, "sub/.hidden");
        write(folder, ".well-known/acme-challenge/token");
        write(root, "secret.txt");
        site = new Domain("127.0.0.1", "mysite.com", folder.toString());
    }

    private static void write(Path folder, String relative) throws Exception {
        Path file = folder.resolve(relative);
        Files.createDirectories(file.getParent());
        Files.writeString(file, relative);
    }

    private String served(String path) throws Exception {
        Optional<Path> file = site.fileFor(path);
        return file.isPresent() ? Files.readString(file.get()) : null;
    }

    @Test void aSlashIsTheFoldersIndex() throws Exception {
        assertEquals("index.html", served("/"));
        assertEquals("index.html", served(""));
        assertEquals("docs/index.html", served("/docs/"));
    }

    @Test void aNameWithTwoDotsInsideItIsAName() throws Exception {
        assertEquals("a..b.txt", served("/a..b.txt"));
    }

    @Test void theWellKnownFolderIsServed() throws Exception {
        assertEquals(".well-known/acme-challenge/token", served("/.well-known/acme-challenge/token"));
    }

    /** {@code .git/config} is the case a check of the file's own name misses: the dot is on the folder. */
    @ParameterizedTest @ValueSource(strings = {"/.env", "/.git/config", "/sub/.hidden"})
    void aDotSegmentIsNeverServed(String path) throws Exception {
        assertNull(served(path));
    }

    @ParameterizedTest @ValueSource(strings = {"/../secret.txt", "/sub/../../secret.txt", "/docs", "/missing.txt"})
    void nothingButAFileOfTheFolderIsServed(String path) throws Exception {
        assertNull(served(path));
    }

    @Test void aDomainThatForwardsHasNoFiles() {
        assertTrue(new Domain("127.0.0.1", "app.com", "http://localhost:8080").fileFor("/").isEmpty());
    }
}

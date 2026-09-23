package org.solarframework.proxyserver.obj;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.solarframework.json.JSONItem;
import org.solarframework.proxyserver.obj.ratelimit.ProxyRateLimitsFile;
import org.solarframework.proxyserver.obj.ratelimit.RateLimitRule;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The proxy's hand-entered request limits are config, not data rows, so a restart must rebuild them from their own JSON file. */
class ProxyRateLimitsFileTest {

    private Path file;

    @BeforeEach void makeFile() throws Exception {
        file = Files.createTempFile("proxy-rate-limits", ".json");
        Files.delete(file);
    }
    @AfterEach void removeFile() throws Exception { Files.deleteIfExists(file); }


    @Test void savesAndReloadsEveryRateLimitRule() {
        new ProxyRateLimitsFile(List.of(new RateLimitRule("*", "/api/**", 5, 20, 0, 1, 64), new RateLimitRule("portal.local", "/auth/**", 0.5, 3, 1, 5, 60))).Write(file.toString());

        List<RateLimitRule> reloaded = JSONItem.ReadJSON(file.toString(), ProxyRateLimitsFile.class).rules();
        assertEquals(2, reloaded.size());
        assertEquals("/api/**", reloaded.getFirst().getPathPattern());
        assertEquals("portal.local", reloaded.getLast().getHost());
        assertEquals(3, reloaded.getLast().getBurst());
    }

    /** No file yet must not fail the boot - it is the ordinary first run with nothing configured. */
    @Test void aMissingFileLoadsAsEmptyAndWritesItself() {
        ProxyRateLimitsFile loaded = JSONItem.ReadOrCreate(file.toString(), ProxyRateLimitsFile.class, new ProxyRateLimitsFile(List.of()));
        assertTrue(loaded.rules().isEmpty());
        assertTrue(Files.exists(file));
    }
}

package org.solarframework.core.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class SolarHomeTest {

    @BeforeEach
    @AfterEach
    void forgetWhatEarlierTestsSet() {
        System.clearProperty(SolarHome.PROPERTY);
        SolarHome.forgetDataFolder();
    }
    private void pointAt(String folder) {
        System.setProperty(SolarHome.PROPERTY, folder);
        SolarHome.forgetDataFolder();
    }
    private Path workingDirectory() {
        return Path.of(".").toAbsolutePath().normalize();
    }


    @Test
    void withNothingSetTheFolderIsTheWorkingDirectory() {
        assertEquals(workingDirectory(), SolarHome.dataFolder());
    }

    @Test
    void theFlagNamesTheFolder() {
        pointAt("target/pretend-data-folder");
        assertEquals(Path.of("target/pretend-data-folder").toAbsolutePath().normalize(), SolarHome.dataFolder());
    }

    @Test
    void aBlankFlagFallsBackRatherThanResolvingToNothing() {
        pointAt("   ");
        assertEquals(workingDirectory(), SolarHome.dataFolder());
    }

    @Test
    void theFolderIsRememberedRatherThanReadEveryTime() {
        Path first = SolarHome.dataFolder();
        System.setProperty(SolarHome.PROPERTY, "target/somewhere-else");
        assertEquals(first, SolarHome.dataFolder());
    }


    @Test
    void aRelativeNameIsResolvedUnderTheFolder() {
        pointAt("target/pretend-data-folder");
        assertEquals(SolarHome.dataFolder().resolve("config").resolve("plugins.json"), SolarHome.pathTo("config", "plugins.json"));
        assertEquals(SolarHome.dataFolder().resolve(Path.of("config/data/database.json")), SolarHome.pathTo("config/data/database.json"));
    }

    @Test
    void aNameWrittenWithALeadingDotIsTidiedRatherThanNested() {
        pointAt("target/pretend-data-folder");
        assertEquals(SolarHome.pathTo("config/api/entities.json"), SolarHome.pathTo("./config/api/entities.json"));
    }

    @Test
    void aNameThatIsAlreadyAbsoluteIsLeftAlone() {
        pointAt("target/pretend-data-folder");
        String absolute = Path.of("somewhere", "config.json").toAbsolutePath().toString();
        assertEquals(Path.of(absolute), SolarHome.pathTo(absolute));
    }
}

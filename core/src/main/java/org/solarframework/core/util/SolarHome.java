package org.solarframework.core.util;

import java.nio.file.Path;

/**
 * Where the installation keeps everything it writes — config, plugins, entities, backups and the database.
 * <p>
 * It cannot come from {@code .env} or any Spring property: that file is itself found relative to the working
 * directory, and what this resolves is read by static initialisers running before any application context. So it
 * comes from {@code -Dsolar.home} or {@code SOLAR_HOME}, falling back to the working directory as before.
 */
public class SolarHome {

    public static final String PROPERTY = "solar.home";
    public static final String VARIABLE = "SOLAR_HOME";

    private static Path dataFolder;

    /** The folder the installation writes to, absolute, worked out once and remembered. */
    public static Path dataFolder() {
        if (dataFolder == null) dataFolder = Path.of(settingOrWorkingDirectory()).toAbsolutePath().normalize();
        return dataFolder;
    }
    /** The JVM flag wins over the environment variable, and neither being set leaves the working directory. */
    private static String settingOrWorkingDirectory() {
        String flag = System.getProperty(PROPERTY);
        if (flag != null && !flag.isBlank()) return flag.trim();
        String variable = System.getenv(VARIABLE);
        return variable == null || variable.isBlank() ? "." : variable.trim();
    }
    /** Forgets the remembered folder so a test can point the next call somewhere else. */
    public static void forgetDataFolder() {
        dataFolder = null;
    }

    /** The path a name written relative to the data folder stands for; one already absolute is handed back untouched. Normalised, because the names are written by hand and several open "./config/…". */
    public static Path pathTo(String first, String... more) {
        return dataFolder().resolve(Path.of(first, more)).normalize();
    }
}

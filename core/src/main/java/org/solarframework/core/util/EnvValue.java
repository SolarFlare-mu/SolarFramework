package org.solarframework.core.util;

/**
 * A setting written into a config file that may point at where its real value lives, so a secret such as an API key
 * or a password never has to be written into a file that is committed or shared.
 * <p>{@code "${NAME}"} stands for the JVM flag {@code -DNAME}, else the environment variable {@code NAME}, the same
 * order as {@link SolarHome}. Anything else is its own value. Resolve at the moment of use and keep the reference
 * itself in the setting, so saving the config writes {@code "${NAME}"} back rather than the secret.
 */
public class EnvValue {

    /** The value {@code setting} stands for; null when it names a variable that is not set, or is null itself. */
    public static String resolve(String setting) {
        String name = nameIn(setting);
        if (name == null) return setting;
        String flag = System.getProperty(name);
        return flag != null && !flag.isBlank() ? flag.trim() : trimmedOrNull(System.getenv(name));
    }

    /** The variable {@code setting} names, or null when it is a plain value. */
    public static String nameIn(String setting) {
        if (setting == null) return null;
        String s = setting.trim();
        return s.startsWith("${") && s.endsWith("}") ? trimmedOrNull(s.substring(2, s.length() - 1)) : null;
    }

    private static String trimmedOrNull(String v) { return v == null || v.isBlank() ? null : v.trim(); }
}

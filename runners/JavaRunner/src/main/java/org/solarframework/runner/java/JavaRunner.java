package org.solarframework.runner.java;

import org.solarframework.runner.AppRunner;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** A Java application jar, {@code java [options] -jar app.jar [args]}, run from the jar's folder on the host's own JVM unless told otherwise. */
public class JavaRunner extends AppRunner {
    private final Path jar;
    private final List<String> args, options = new ArrayList<>();
    private String java;

    public JavaRunner(Path jar, String... args) {
        super(jar.toAbsolutePath().getParent());
        this.jar = jar.toAbsolutePath().normalize();
        this.args = List.of(args);
    }

    /** A JVM option, e.g. {@code -Xmx512m} or {@code -Dserver.port=8081}. */
    public JavaRunner option(String option) { options.add(option); return this; }

    /** Overrides the java executable; blank keeps the default. */
    public JavaRunner java(String executable) { java = executable == null || executable.isBlank() ? null : executable; return this; }

    /** The one set, else the JVM this host runs on, so the jar gets the same Java version. */
    public String getJava() { return java != null ? java : Path.of(System.getProperty("java.home"), "bin", "java").toString(); }

    @Override protected List<String> command() {
        List<String> out = new ArrayList<>(List.of(getJava()));
        out.addAll(options);
        out.addAll(List.of("-jar", jar.toString()));
        out.addAll(args);
        return out;
    }
}

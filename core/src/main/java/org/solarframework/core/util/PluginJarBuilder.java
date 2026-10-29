package org.solarframework.core.util;

import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Stream;

/**
 * Compiles Java source written at run time and packs it as a PF4J plugin jar: the sources, a {@code plugin.properties}
 * and, because annotation processing is on, the extension index the plugin loader finds {@code @Extension} classes through.
 * The classpath is the caller's, since only the caller knows what the generated code may refer to. Parameter names are kept in
 * the classes ({@code -parameters}): a description of a method built by reflection, such as the one an AI model is given for a
 * tool, would otherwise name its arguments arg0 and arg1.
 */
public final class PluginJarBuilder {
    public record Source(String className, String code) {}

    /** The compiler refused the code; {@link #output()} is exactly what it printed, for a caller that wants to put it in its own words. */
    public static class CompileException extends IOException {
        private final String output;

        public CompileException(String pluginId, String output) {
            super("compilation failed for plugin \"" + pluginId + "\":\n" + output);
            this.output = output;
        }

        public String output() { return output; }
    }

    private PluginJarBuilder() {}

    /**
     * Builds the jar at that path, or throws and writes nothing.
     *
     * @param dependencies the plugin ids this one needs, comma separated, or null for none
     */
    public static void build(Path jarFile, String id, String version, String dependencies, String classpath, List<Source> sources) throws IOException {
        Path src = Files.createTempDirectory("plugin-src"), out = Files.createTempDirectory("plugin-out");
        try {
            writeSources(src, sources);
            compile(id, src, out, classpath);
            Files.writeString(out.resolve("plugin.properties"), "plugin.id=%s%nplugin.version=%s%n".formatted(id, version) + (dependencies == null ? "" : "plugin.dependencies=%s%n".formatted(dependencies)));
            pack(out, jarFile);
        } finally {
            FileUtils.deleteTree(src);
            FileUtils.deleteTree(out);
        }
    }

    private static void writeSources(Path src, List<Source> sources) throws IOException {
        for (Source s : sources) {
            Path file = src.resolve(s.className().replace('.', '/') + ".java");
            Files.createDirectories(file.getParent());
            Files.writeString(file, s.code());
        }
    }

    private static void compile(String id, Path src, Path out, String classpath) throws IOException {
        try (Stream<Path> files = Files.walk(src)) {
            String[] args = Stream.concat(Stream.of("-proc:full", "-parameters", "-encoding", "UTF-8", "-cp", classpath, "-d", out.toString()), files.map(Path::toString).filter(f -> f.endsWith(".java"))).toArray(String[]::new);
            ByteArrayOutputStream errors = new ByteArrayOutputStream();
            if (ToolProvider.getSystemJavaCompiler().run(null, null, errors, args) != 0) throw new CompileException(id, errors.toString(StandardCharsets.UTF_8));
        }
    }

    private static void pack(Path out, Path jarFile) throws IOException {
        try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(jarFile)); Stream<Path> files = Files.walk(out)) {
            for (Path p : (Iterable<Path>) files.filter(Files::isRegularFile)::iterator) {
                jar.putNextEntry(new JarEntry(out.relativize(p).toString().replace('\\', '/')));
                Files.copy(p, jar);
                jar.closeEntry();
            }
        }
    }
}

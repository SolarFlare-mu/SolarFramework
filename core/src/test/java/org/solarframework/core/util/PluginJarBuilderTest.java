package org.solarframework.core.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.JarFile;

import static org.junit.jupiter.api.Assertions.*;

class PluginJarBuilderTest {
    @TempDir
    Path folder;

    private static PluginJarBuilder.Source hello(String body) {
        return new PluginJarBuilder.Source("demo.Hello", "package demo; public class Hello { public String say() { return " + body + "; } }");
    }

    @Test
    void packsTheCompiledClassAndADescriptorNamingItsDependency() throws Exception {
        Path jar = folder.resolve("demo.jar");
        PluginJarBuilder.build(jar, "demo", "1.0.0", "other-plugin", System.getProperty("java.class.path"), List.of(hello("\"hi\"")));
        try (JarFile file = new JarFile(jar.toFile())) {
            assertNotNull(file.getEntry("demo/Hello.class"));
            String properties = new String(file.getInputStream(file.getEntry("plugin.properties")).readAllBytes());
            assertTrue(properties.contains("plugin.id=demo") && properties.contains("plugin.version=1.0.0") && properties.contains("plugin.dependencies=other-plugin"));
        }
    }

    @Test
    void keepsParameterNamesSoAReflectionBuiltDescriptionIsNotArg0() throws Exception {
        Path jar = folder.resolve("named.jar");
        PluginJarBuilder.build(jar, "named", "1.0.0", null, System.getProperty("java.class.path"), List.of(new PluginJarBuilder.Source("demo.Named", "package demo; public class Named { public String find(String table, String text) { return table + text; } }")));
        try (java.net.URLClassLoader loader = new java.net.URLClassLoader(new java.net.URL[]{jar.toUri().toURL()}, getClass().getClassLoader())) {
            java.lang.reflect.Parameter[] parameters = Class.forName("demo.Named", true, loader).getMethod("find", String.class, String.class).getParameters();
            assertEquals("table", parameters[0].getName());
            assertEquals("text", parameters[1].getName());
        }
    }

    @Test
    void aPluginWithNoDependencyWritesNoDependencyLine() throws Exception {
        Path jar = folder.resolve("alone.jar");
        PluginJarBuilder.build(jar, "alone", "1.0.0", null, System.getProperty("java.class.path"), List.of(hello("\"hi\"")));
        try (JarFile file = new JarFile(jar.toFile())) {
            assertFalse(new String(file.getInputStream(file.getEntry("plugin.properties")).readAllBytes()).contains("dependencies"));
        }
    }

    @Test
    void aCompileErrorCarriesTheCompilersOutputAndWritesNoJar() {
        Path jar = folder.resolve("bad.jar");
        PluginJarBuilder.CompileException error = assertThrows(PluginJarBuilder.CompileException.class, () -> PluginJarBuilder.build(jar, "bad", "1.0.0", null, System.getProperty("java.class.path"), List.of(hello("missingValue"))));
        assertTrue(error.output().contains("missingValue"));
        assertTrue(error.getMessage().startsWith("compilation failed for plugin \"bad\""));
        assertFalse(Files.exists(jar));
    }
}

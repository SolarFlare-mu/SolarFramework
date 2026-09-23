package org.solarframework.runner.csharp;

import org.solarframework.runner.AppRunner;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * A C# application through the dotnet CLI: {@code project(dir)} runs the project from source, {@code dotnet run --project dir},
 * and {@code assembly(dll)} runs one already built, {@code dotnet app.dll}.
 */
public class CSharpRunner extends AppRunner {
    private final List<String> target;
    private String dotnet = "dotnet";

    private CSharpRunner(Path dir, List<String> target) {
        super(dir);
        this.target = target;
    }

    public static CSharpRunner project(Path projectDir, String... args) {
        List<String> target = new ArrayList<>(List.of("run", "--project", projectDir.toAbsolutePath().normalize().toString(), "--"));
        target.addAll(List.of(args));
        return new CSharpRunner(projectDir, target);
    }

    public static CSharpRunner assembly(Path dll, String... args) {
        List<String> target = new ArrayList<>(List.of(dll.toAbsolutePath().normalize().toString()));
        target.addAll(List.of(args));
        return new CSharpRunner(dll.toAbsolutePath().getParent(), target);
    }

    /** Overrides the dotnet executable, when it is not on the PATH; blank keeps the default. */
    public CSharpRunner dotnet(String executable) { dotnet = executable == null || executable.isBlank() ? "dotnet" : executable; return this; }

    @Override protected List<String> command() {
        List<String> out = new ArrayList<>(List.of(dotnet));
        out.addAll(target);
        return out;
    }
}

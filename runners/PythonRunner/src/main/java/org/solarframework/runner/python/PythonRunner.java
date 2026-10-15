package org.solarframework.runner.python;

import org.solarframework.runner.AppRunner;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * A Python program, run with the folder's own virtual environment when it has one, so its packages are the ones it
 * was installed with. {@code module(dir, "app.main")} is {@code python -m app.main} from {@code dir}.
 */
public class PythonRunner extends AppRunner {
    private static final boolean WINDOWS = System.getProperty("os.name").toLowerCase().contains("win");
    private final List<String> target;
    private String interpreter;

    private PythonRunner(Path dir, List<String> target) {
        super(dir);
        this.target = target;
    }

    public static PythonRunner module(Path dir, String module, String... args) { return new PythonRunner(dir, join(List.of("-m", module), args)); }

    public static PythonRunner script(Path dir, String script, String... args) { return new PythonRunner(dir, join(List.of(script), args)); }

    /** Overrides the interpreter; blank keeps the default. */
    public PythonRunner interpreter(String executable) { interpreter = executable == null || executable.isBlank() ? null : executable; return this; }

    /** The one set, else the folder's .venv or venv, else python from the PATH, which may lack the program's packages. */
    public String getInterpreter() { return interpreter != null ? interpreter : venv(dir).map(Path::toString).orElse(WINDOWS ? "python" : "python3"); }

    public static Optional<Path> venv(Path dir) {
        return Stream.of(".venv", "venv").map(dir::resolve).map(v -> v.resolve(WINDOWS ? "Scripts/python.exe" : "bin/python")).filter(Files::isRegularFile).findFirst();
    }

    /** The module or script it runs, such as app.main. */
    @Override public String name() { return target.getFirst().equals("-m") ? target.get(1) : Path.of(target.getFirst()).getFileName().toString(); }

    @Override protected List<String> command() { return join(List.of(getInterpreter()), target.toArray(String[]::new)); }

    private static List<String> join(List<String> head, String... tail) {
        List<String> out = new ArrayList<>(head);
        out.addAll(List.of(tail));
        return out;
    }
}

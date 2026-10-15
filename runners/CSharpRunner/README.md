# CSharpRunner

`org.solarframework.mu:csharp-runner` — a C# application through the dotnet CLI, on [`RunnerAPI`](../RunnerAPI/README.md).

- `CSharpRunner.project(dir, args...)` — runs the project from source, `dotnet run --project dir`.
- `CSharpRunner.assembly(dll, args...)` — runs one already built, `dotnet app.dll`.
- `dotnet(executable)` overrides the CLI.

`dotnet run` is a launcher that starts the real process as its child; `stop()` takes both down.

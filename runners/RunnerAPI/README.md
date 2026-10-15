# RunnerAPI

`org.solarframework.mu:runner-api` — keeps an outside program running for as long as its host. Each runtime is its own module, so a host takes only what it runs: [`JavaRunner`](../JavaRunner/README.md), [`PythonRunner`](../PythonRunner/README.md), [`CSharpRunner`](../CSharpRunner/README.md), [`NodeRunner`](../NodeRunner/README.md); each brings this module with it.

**`AppRunner`** is one program: `start()`, `awaitReady(check, timeout)`, `stop()` — which stops **everything it spawned**, not just the direct child, so a launcher such as `npm` or `dotnet run` never leaves its real server behind. Also `env(key, value)`, `isAlive()`, `getExitCode()`, `getDirectory()`. A plain object like the AI and database stacks, so it runs with no application context. A runtime only says what its command line is (`command()`).

**`CommandRunner`** is any program given its whole command line, for a runtime with no runner of its own: `new CommandRunner(dir, "app.exe", "--port", "9000")`, or `CommandRunner.shell(dir, "npm start")` to take one line the way a terminal would.

```java
PythonRunner api = PythonRunner.module(Path.of("AI"), "app.main");
api.start();
api.awaitReady(() -> isAnswering(), Duration.ofMinutes(3));
api.stop();
```

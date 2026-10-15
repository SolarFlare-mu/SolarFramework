# NodeRunner

`org.solarframework.mu:node-runner` — a Node.js program on [`RunnerAPI`](../RunnerAPI/README.md).

- `NodeRunner.script(dir, "server.js", args...)` — `node server.js`.
- `NodeRunner.npm(dir, "start", args...)` — runs a `package.json` script, which is how most Node web apps are started.
- `node(executable)` overrides the launcher.

`npm` is a launcher that starts the real server as its child; `stop()` takes both down.

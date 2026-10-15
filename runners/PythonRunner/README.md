# PythonRunner

`org.solarframework.mu:python-runner` — a Python program on [`RunnerAPI`](../RunnerAPI/README.md), **run with the folder's own virtual environment when it has one**, so its packages are the ones it was installed with (`venv(dir)` finds it).

- `PythonRunner.module(dir, "app.main", args...)` — `python -m app.main` from `dir`.
- `PythonRunner.script(dir, "server.py", args...)` — `python server.py`.
- `interpreter(executable)` overrides the interpreter.

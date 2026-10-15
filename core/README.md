# core

`org.solarframework.mu:core` — the utilities every other module leans on, with no Spring and no outside dependency. Check here before writing a private helper anywhere in the framework or in a host.

## What is in it
- **`util/`** — `StringUtils`, `NumberUtils`, `Converters`, `TimeUtils`, `FileUtils`, `ProcessUtils`, `ThreadUtils`, `TerminalUtils`, `ClassUtils`, `JarReader`, `ImageUtils`, `OtherUtils`.
- **`SolarHome`** — where an installation keeps everything it writes (config, plugins, entities, backups, the database). **It cannot come from `.env` or a Spring property**: static initialisers read it before any context exists, so it resolves from `-Dsolar.home`, then `SOLAR_HOME`, then the working directory.
- **`EnvValue`** — a config-file value that may be `"${NAME}"`, read from `-DNAME` then the environment variable `NAME`, so a secret never has to be written into a shared file.
- **`RateLimiter`** — a token bucket per caller; a caller that keeps knocking while refused waits a pause that doubles each time.
- **`SensitiveColumns`** — which columns must never leave over an API, decided from the column's name rather than a list that would drift from the schema.
- **`IpUtils`** (allow-list matching), **`HostsFile`** (owns its own entries in the machine's hosts file), **`PDFUtils`** (what a PDF's bytes alone prove).
- **`img/`** — `ImageBuilder`, `GifSequenceWriter`.
- **`Constants`**.

Tests: 15 classes under `src/test`, no backend needed.

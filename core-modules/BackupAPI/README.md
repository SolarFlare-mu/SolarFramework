# BackupAPI

`org.solarframework.mu:backup-api` — the backup interface. The implementation is [`BackupImpl`](../BackupImpl/README.md); add both.

- **`IBackupService`** — copies of everything an installation would need to be itself again: every data source, and the folders named beside them (`getIncludedPaths`). **One backup is one folder** under `getDirectory()` holding a dump per source and a manifest, so a person can open it, read it and copy it onto a memory stick, and a restore needs none of this code running. `take(note)`, `list()`, `get(id)`, `restore(id)`, `delete(id)`, `prune()` (by `keep` count and `keepDays`), `LoadFromFile` / `SaveAsFile`.
- **`BackupEntry`** — one backup as its folder describes itself. Its `skipped` list names every source that could not be copied and why: a backup that quietly left one out would be found wanting only at the restore.
- **`BackupRegistry`** — `SolarBackup`.

```java
import static org.solarframework.backup.spring.BackupRegistry.SolarBackup;
SolarBackup.take("before the upgrade");
```

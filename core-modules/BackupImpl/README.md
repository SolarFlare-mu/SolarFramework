# BackupImpl

`org.solarframework.mu:backup-impl` — [`BackupAPI`](../BackupAPI/README.md) over the data sources `IDatabaseManager` holds.

**No stereotype**, so it runs with no application context; `BackupConfig` is the same object as a bean. Settings come from `solar.backup.directory` (default `backups`), `solar.backup.keep` (10) and `solar.backup.keep-days` (7) first; `config/backup/settings.json` (`BackupSettingsFile`) has the last word after that, as the mailer's mailboxes do. Each backup's manifest is `BackupFile`, written inside its own folder so the folder explains itself wherever it is copied.

**Each database is copied by the means its own engine provides, never by copying its file**: a file taken mid-write is a corrupt database that looks fine until the restore.

Tests: 3 classes on SQLite. `BackupLiveTest` works on a copy of a real installation database (`-Dsolar.test.database=<path to a .db>`) and runs only with `-Plive`.

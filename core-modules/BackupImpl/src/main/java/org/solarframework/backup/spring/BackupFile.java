package org.solarframework.backup.spring;

import org.solarframework.backup.dto.BackupEntry;
import org.solarframework.json.JSONItem;

/** One backup's manifest, written inside its own folder so the folder explains itself wherever it is copied. */
class BackupFile extends JSONItem<BackupFile> {
    final BackupEntry entry;
    public BackupFile(BackupEntry entry) { this.entry = entry; }
}

package org.solarframework.backup.spring;

import org.solarframework.backup.IBackupService;
import org.solarframework.json.JSONItem;

import java.util.ArrayList;
import java.util.List;

/** Backup settings as stored in `config/backup/settings.json`. */
class BackupSettingsFile extends JSONItem<BackupSettingsFile> {
    final String directory;
    final int keep;
    final int keepDays;
    final List<String> includedPaths;

    public BackupSettingsFile(IBackupService service) {
        directory = service.getDirectory();
        keep = service.getKeep();
        keepDays = service.getKeepDays();
        includedPaths = new ArrayList<>(service.getIncludedPaths());
    }

    @Override public String file() { return BackupConfig.FILE; }

    public static BackupSettingsFile read(String path, IBackupService service) { return ReadOrCreate(path, BackupSettingsFile.class, new BackupSettingsFile(service)); }
}

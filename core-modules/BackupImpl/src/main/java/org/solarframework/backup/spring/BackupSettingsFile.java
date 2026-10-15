package org.solarframework.backup.spring;

import org.solarframework.backup.IBackupService;
import org.solarframework.json.JSONItem;

import java.util.ArrayList;
import java.util.List;

/** Backup settings as stored in `config/backup/settings.json`. */
class BackupSettingsFile extends JSONItem<BackupSettingsFile> {
    private final String directory;
    private final int keep;
    private final int keepDays;
    private final List<String> includedPaths;

    public BackupSettingsFile(IBackupService service) {
        directory = service.getDirectory();
        keep = service.getKeep();
        keepDays = service.getKeepDays();
        includedPaths = new ArrayList<>(service.getIncludedPaths());
    }

    @Override public String file() { return BackupConfig.FILE; }
    public String getDirectory() { return directory; }
    public int getKeep() { return keep; }
    public int getKeepDays() { return keepDays; }
    public List<String> getIncludedPaths() { return includedPaths; }

}

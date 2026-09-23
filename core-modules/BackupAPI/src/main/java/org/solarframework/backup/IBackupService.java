package org.solarframework.backup;

import org.solarframework.backup.dto.BackupEntry;

import java.util.List;

/**
 * Copies of everything an installation would need to be itself again: every data source, and the folders named
 * beside them. One backup is one folder under {@link #getDirectory()} holding a dump per source and a manifest, so a
 * person can open it, read it and copy it onto a memory stick, and a restore needs none of this code running.
 */
public interface IBackupService {

    /** Relative to the working directory unless absolute, so an external drive is simply a different path. */
    String getDirectory();
    void setDirectory(String directory);

    /** How many to keep. 0 or less keeps every backup ever taken. */
    int getKeep();
    void setKeep(int keep);

    /** How many days back to keep, which is the promise a "restore to any point in the last week" makes. 0 or less keeps them by count alone. */
    int getKeepDays();
    void setKeepDays(int keepDays);

    /** Folders copied alongside the databases - configuration, uploads, anything not in a database. */
    List<String> getIncludedPaths();
    void setIncludedPaths(List<String> paths);

    /** {@code note} says why, and is shown in the list: "before the upgrade" is worth more later than a timestamp. */
    BackupEntry take(String note);

    List<BackupEntry> list();

    BackupEntry get(String id);

    /**
     * Databases first, then the folders. The application must be restarted afterwards: the pools, the loaded plugins
     * and every cached row still hold what was there a moment ago.
     * @return the trouble met, in plain words, or null when everything went back
     */
    String restore(String id);

    boolean delete(String id);

    /** Runs itself after every {@link #take}. */
    int prune();

    void LoadFromFile(String path);

    void SaveAsFile(String path);
}

package org.solarframework.backup.dto;

import java.time.Instant;
import java.util.List;

/**
 * One backup, as its own folder describes itself.
 *
 * @param skipped the data sources this could not copy and why - a backup that quietly left one out would be found
 *                wanting only at the restore, which is the worst moment to learn it
 */
public record BackupEntry(String id, Instant takenAt, String note, long sizeBytes, List<String> sources, List<String> paths, List<String> skipped) {

    public BackupEntry {
        sources = sources == null ? List.of() : List.copyOf(sources);
        paths = paths == null ? List.of() : List.copyOf(paths);
        skipped = skipped == null ? List.of() : List.copyOf(skipped);
    }

    public boolean isComplete() { return skipped.isEmpty(); }

    @Override public String toString() { return id + " (" + sources.size() + " database(s), " + paths.size() + " folder(s)" + (isComplete() ? "" : ", " + skipped.size() + " skipped") + ")"; }
}

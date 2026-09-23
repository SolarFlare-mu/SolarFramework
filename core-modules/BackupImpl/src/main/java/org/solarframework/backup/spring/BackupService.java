package org.solarframework.backup.spring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solarframework.backup.IBackupService;
import org.solarframework.backup.dto.BackupEntry;
import org.solarframework.core.util.ProcessUtils;
import org.solarframework.core.util.SolarHome;
import org.solarframework.db.api.DatabaseType;
import org.solarframework.db.api.IDatabaseService;
import org.solarframework.json.JSONItem;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Stream;

import static org.solarframework.core.util.FileUtils.*;
import static org.solarframework.db.spring.DatabaseRegistry.DefaultDBService;
import static org.solarframework.db.spring.DatabaseRegistry.SolarDBManager;

/**
 * {@link IBackupService} over the data sources the database manager holds. No stereotype, so it also runs with no
 * application context; {@link BackupConfig} is the same object as a bean.
 * <p>Each database is copied by the means its own engine provides rather than by copying its file, which taken
 * mid-write is a corrupt database that looks fine until it is needed: <b>SQLite</b> through {@code VACUUM INTO},
 * <b>MariaDB and MySQL</b> through {@code mysqldump}, which must be on the PATH. Anything else is left out and the
 * manifest says so.
 */
public class BackupService implements IBackupService {

    private static final Logger log = LoggerFactory.getLogger(BackupService.class);
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private static final String MANIFEST = "backup.json";
    private static final long TOOL_TIMEOUT_SECONDS = 600;

    private String directory = "backups";
    private int keep = 10;
    private int keepDays;
    private List<String> includedPaths = new ArrayList<>(List.of("config"));

    public BackupService() { BackupRegistry.SolarBackup = this; }

    @Override public String getDirectory() { return directory; }
    @Override public void setDirectory(String directory) { this.directory = directory; }
    @Override public int getKeep() { return keep; }
    @Override public void setKeep(int keep) { this.keep = keep; }
    @Override public int getKeepDays() { return keepDays; }
    @Override public void setKeepDays(int keepDays) { this.keepDays = keepDays; }
    @Override public List<String> getIncludedPaths() { return includedPaths; }
    @Override public void setIncludedPaths(List<String> paths) { this.includedPaths = paths == null ? new ArrayList<>() : new ArrayList<>(paths); }


    @Override public BackupEntry take(String note) {
        String id = LocalDateTime.now().format(STAMP);
        Path folder = SolarHome.pathTo(directory, id);
        List<String> sources = new ArrayList<>(), paths = new ArrayList<>(), skipped = new ArrayList<>();
        try {
            Files.createDirectories(folder);
            copyEveryDataSource(folder, sources, skipped);
            copyEveryIncludedPath(folder, paths, skipped);
            BackupEntry entry = new BackupEntry(id, Instant.now(), note, sizeOf(folder), sources, paths, skipped);
            new BackupFile(entry).Write(folder.resolve(MANIFEST).toString());
            prune();
            log.info("Took backup {}", entry);
            return entry;
        } catch (Exception e) {
            log.error("Backup {} could not be taken: {}", id, e.getMessage());
            return null;
        }
    }

    private void copyEveryDataSource(Path folder, List<String> done, List<String> skipped) {
        for (IDatabaseService source : everySource()) {
            String trouble = copyDataSource(source, folder);
            if (trouble == null) done.add(source.getName());
            else skipped.add(source.getName() + ": " + trouble);
        }
    }

    /**
     * The default is taken from the registry rather than trusted to be in the manager's set, because it may
     * legitimately be missing from it: {@code DatabaseManager.addSource} refuses a source with no username or
     * password, and SQLite - what an installation runs on until somebody points it at a server - has neither.
     */
    private List<IDatabaseService> everySource() {
        Map<String, IDatabaseService> byName = new LinkedHashMap<>();
        if (DefaultDBService != null) byName.put(DefaultDBService.getName(), DefaultDBService);
        if (SolarDBManager != null) for (IDatabaseService source : SolarDBManager.getSources()) byName.putIfAbsent(source.getName(), source);
        return List.copyOf(byName.values());
    }

    /** @return null when the source was copied, otherwise what stopped it, in words a person can act on. */
    private String copyDataSource(IDatabaseService source, Path folder) {
        DatabaseType type = source.getDatabaseType();
        if (type == null) return "no database type is set on this source.";
        return switch (type) {
            case SQLITE -> copySqlite(source, folder.resolve(source.getName() + ".db"));
            case MARIADB, MYSQL -> runMysqldump(source, folder.resolve(source.getName() + ".sql"));
            default -> type.getDisplayName() + " is not one this can copy. Back it up with its own tools.";
        };
    }

    /** On a connection of its own: every write through {@link IDatabaseService} is wrapped in a transaction, and SQLite refuses to VACUUM inside one. */
    private String copySqlite(IDatabaseService source, Path target) {
        return onSqlite(source, "VACUUM INTO '" + quoted(target) + "'", "SQLite refused to write the copy");
    }

    /**
     * SQLite's own restore, poured into the open database through its backup interface. Copying the file over itself
     * is what a person would try first and cannot work: Windows refuses to overwrite a file the running pool holds.
     */
    private String putSqliteBack(IDatabaseService source, Path dump) {
        String trouble = onSqlite(source, "restore from '" + quoted(dump) + "'", "SQLite could not put the file back");
        if (trouble != null) return trouble;
        // the write-ahead log holds changes made after the backup was taken, and SQLite would replay it over the restore on the next open
        Path file = SolarHome.pathTo(source.getConnectionString().substring(DatabaseType.SQLITE.getJdbcPrefix().length()));
        for (String sidecar : List.of("-wal", "-shm", "-journal")) try {
            Files.deleteIfExists(Path.of(file + sidecar));
        } catch (IOException _) {}
        return null;
    }

    private String onSqlite(IDatabaseService source, String sql, String whatFailed) {
        try (Connection connection = DriverManager.getConnection(source.getConnectionString()); Statement statement = connection.createStatement()) {
            statement.execute(sql);
            return null;
        } catch (Exception e) {
            return whatFailed + " (" + e.getMessage() + ").";
        }
    }
    private static String quoted(Path path) { return path.toAbsolutePath().toString().replace("'", "''"); }

    private String runMysqldump(IDatabaseService source, Path target) {
        JdbcTarget at = JdbcTarget.of(source.getConnectionString());
        if (at == null) return "its address could not be read from " + source.getConnectionString() + ".";
        List<String> command = List.of("mysqldump", "--host=" + at.host(), "--port=" + at.port(), "--user=" + orEmpty(source.getUsername()), "--single-transaction", "--routines", "--databases", at.database());
        return toolTrouble(ProcessUtils.run(command, passwordFor(source), target, null, TOOL_TIMEOUT_SECONDS), "mysqldump could not copy it.");
    }

    private String runMysqlClient(IDatabaseService source, Path dump) {
        JdbcTarget at = JdbcTarget.of(source.getConnectionString());
        if (at == null) return "its address could not be read from " + source.getConnectionString() + ".";
        List<String> command = List.of("mysql", "--host=" + at.host(), "--port=" + at.port(), "--user=" + orEmpty(source.getUsername()));
        return toolTrouble(ProcessUtils.run(command, passwordFor(source), null, dump, TOOL_TIMEOUT_SECONDS), "the mysql client could not put it back.");
    }

    /** In the environment rather than the command: an argument is visible to anyone who can list processes. */
    private Map<String, String> passwordFor(IDatabaseService source) { return Map.of("MYSQL_PWD", orEmpty(source.getPassword())); }

    private String toolTrouble(ProcessUtils.Result result, String whatFailed) {
        if (result.ok()) return null;
        return whatFailed + " " + (result.message().isBlank() ? "It may not be installed on this machine - it comes with the database client tools." : result.message());
    }

    private void copyEveryIncludedPath(Path folder, List<String> done, List<String> skipped) {
        for (String named : includedPaths) {
            Path from = SolarHome.pathTo(named);
            if (!Files.exists(from)) continue;
            try {
                copyTree(from, folder.resolve("files").resolve(from.getFileName().toString()));
                done.add(named);
            } catch (Exception e) {
                skipped.add(named + ": " + e.getMessage());
            }
        }
    }


    /** Read from the manifests rather than from anything held in memory, so a backup copied from another machine lists too. */
    @Override public List<BackupEntry> list() {
        Path root = SolarHome.pathTo(directory);
        if (!Files.isDirectory(root)) return List.of();
        try (Stream<Path> folders = Files.list(root)) {
            return folders.filter(Files::isDirectory).map(this::readManifest).filter(Objects::nonNull).sorted(Comparator.comparing(BackupEntry::id).reversed()).toList();
        } catch (IOException e) {
            log.error("Could not read the backup folder {}: {}", directory, e.getMessage());
            return List.of();
        }
    }

    @Override public BackupEntry get(String id) { return readManifest(SolarHome.pathTo(directory, id)); }

    /** A folder with no readable manifest is not a backup this took, and is left alone rather than listed or deleted. */
    private BackupEntry readManifest(Path folder) {
        BackupFile file = JSONItem.ReadJSON(folder.resolve(MANIFEST).toString(), BackupFile.class);
        return file == null ? null : file.entry;
    }


    @Override public String restore(String id) {
        BackupEntry entry = get(id);
        if (entry == null) return "There is no backup called " + id + " in " + directory + ".";
        Path folder = SolarHome.pathTo(directory, id);
        List<String> trouble = new ArrayList<>();
        for (IDatabaseService source : everySource()) {
            String failed = putDataSourceBack(source, folder);
            if (failed != null) trouble.add(source.getName() + ": " + failed);
        }
        for (String named : entry.paths()) {
            Path from = folder.resolve("files").resolve(SolarHome.pathTo(named).getFileName().toString());
            try {
                if (Files.exists(from)) copyTree(from, SolarHome.pathTo(named));
            } catch (Exception e) {
                trouble.add(named + ": " + e.getMessage());
            }
        }
        log.warn("Restored backup {} - the application must be restarted before anything reads from it", id);
        return trouble.isEmpty() ? null : String.join(" ", trouble);
    }

    /** @return null when the source was put back or the backup does not hold it, otherwise what stopped it. */
    private String putDataSourceBack(IDatabaseService source, Path folder) {
        DatabaseType type = source.getDatabaseType();
        if (type == null) return null;
        Path dump = folder.resolve(source.getName() + (type == DatabaseType.SQLITE ? ".db" : ".sql"));
        if (!Files.exists(dump)) return null;
        return switch (type) {
            case SQLITE -> putSqliteBack(source, dump);
            case MARIADB, MYSQL -> runMysqlClient(source, dump);
            default -> null;
        };
    }


    @Override public boolean delete(String id) {
        if (get(id) == null) return false;
        deleteTree(SolarHome.pathTo(directory, id));
        log.info("Deleted backup {}", id);
        return true;
    }

    /**
     * A backup goes only when it is both beyond the count and older than the window, so the window is a promise -
     * "any point in the last week" - that a busy day cannot break by pushing ten backups past the count. The newest
     * is never deleted, whatever the settings say.
     */
    @Override public int prune() {
        if (keep <= 0) return 0;
        List<BackupEntry> all = list();
        Instant cutoff = keepDays <= 0 ? null : Instant.now().minus(keepDays, ChronoUnit.DAYS);
        int removed = 0;
        for (int at = Math.max(1, keep); at < all.size(); at++) {
            BackupEntry entry = all.get(at);
            if (cutoff != null && entry.takenAt() != null && entry.takenAt().isAfter(cutoff)) continue;
            deleteTree(SolarHome.pathTo(directory, entry.id()));
            removed++;
        }
        return removed;
    }


    @Override public void LoadFromFile(String path) {
        BackupSettingsFile settings = BackupSettingsFile.read(path, this);
        directory = settings.directory;
        keep = settings.keep;
        keepDays = settings.keepDays;
        includedPaths = new ArrayList<>(settings.includedPaths);
        log.info("Backups go to {}, keeping {}{}, with {}", directory, keep <= 0 ? "every one" : keep, keepDays > 0 ? " and everything from the last " + keepDays + " day(s)" : "", includedPaths);
    }

    @Override public void SaveAsFile(String path) {
        if (new BackupSettingsFile(this).Write(path) == null) log.error("Failed to write backup settings file: {}", path);
    }

    /** What a database tool needs out of {@code jdbc:mariadb://host:port/database?options}. */
    protected record JdbcTarget(String host, String port, String database) {
        public static JdbcTarget of(String connectionString) {
            if (connectionString == null || !connectionString.contains("//")) return null;
            String after = connectionString.substring(connectionString.indexOf("//") + 2);
            String address = after.contains("/") ? after.substring(0, after.indexOf('/')) : after;
            String database = after.contains("/") ? after.substring(after.indexOf('/') + 1) : "";
            if (database.contains("?")) database = database.substring(0, database.indexOf('?'));
            if (database.isBlank()) return null;
            return new JdbcTarget(address.contains(":") ? address.substring(0, address.indexOf(':')) : address, address.contains(":") ? address.substring(address.indexOf(':') + 1) : "3306", database);
        }
    }


    private static String orEmpty(String value) { return value == null ? "" : value; }

    @Override public String toString() { return "BackupService[" + directory + ", keeping " + keep + "]"; }
}

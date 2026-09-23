package org.solarframework.backup.spring;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.solarframework.backup.dto.BackupEntry;
import org.solarframework.core.util.FileUtils;
import org.solarframework.db.api.DatabaseType;
import org.solarframework.db.spring.DatabaseManager;
import org.solarframework.db.spring.DatabaseService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * The same round trip as {@link BackupSqliteTest}, against a real installation's database rather than a table built
 * here - a file with its own schema, its own size and possibly a write-ahead log beside it.
 * <p>Point it at one with {@code -Dsolar.test.database=<path to a .db>}; it skips when that is not given. It always
 * works on a copy: the real file is never opened for writing.
 */
@Tag("live")
class BackupLiveTest {

    private Path work;
    private DatabaseService source;
    private BackupService service;

    @BeforeEach void copyTheRealDatabase() throws Exception {
        Path real = Path.of(System.getProperty("solar.test.database", ""));
        assumeTrue(!real.toString().isBlank() && Files.isRegularFile(real), "no installation database given");
        work = Files.createTempDirectory("backup-live");
        Files.copy(real, work.resolve("Default.db"));
        source = new DatabaseService();
        source.setName("Default");
        source.setDatabaseType(DatabaseType.SQLITE);
        source.setConnectionString("jdbc:sqlite:" + work.resolve("Default.db"));
        new DatabaseManager(source);
        service = new BackupService();
        service.setDirectory(work.resolve("backups").toString());
        service.setIncludedPaths(List.of());
    }
    @AfterEach void removeCopy() { if (work != null) FileUtils.deleteTree(work); }


    @Test void backsUpAndRestoresARealDatabase() {
        int tablesBefore = tableCount();
        assertTrue(tablesBefore > 0, "the copied database should hold the installation's tables");

        BackupEntry entry = service.take("live check");
        assertNotNull(entry);
        assertTrue(entry.isComplete(), "nothing should have been skipped: " + entry.skipped());
        Path dump = Path.of(service.getDirectory(), entry.id(), "Default.db");
        assertTrue(Files.exists(dump));
        assertTrue(entry.sizeBytes() > 0);

        source.doUpdate("CREATE TABLE backup_probe (ID INTEGER PRIMARY KEY)");
        assertEquals(tablesBefore + 1, tableCount());

        assertNull(service.restore(entry.id()));
        assertEquals(tablesBefore, tableCount(), "the table made after the backup is gone again");
        assertFalse(hasTable("backup_probe"));
    }

    private int tableCount() { return source.doQueryValueNoCache(Integer.class, "SELECT COUNT(*) FROM sqlite_master WHERE type = 'table'").orElse(0); }
    private boolean hasTable(String name) { return tableCount() > 0 && source.doQueryValueNoCache(Integer.class, "SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name = ?", name).orElse(0) > 0; }
}

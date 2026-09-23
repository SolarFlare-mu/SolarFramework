package org.solarframework.backup.spring;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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

/**
 * The database side, against a real SQLite file. This is the case the whole module exists for: the copy is written by
 * SQLite itself through {@code VACUUM INTO} while the pool is open, because a file copied mid-write is a corrupt
 * database that looks perfectly fine until the day it is needed.
 */
class BackupSqliteTest {

    private Path work;
    private DatabaseService source;
    private BackupService service;

    @BeforeEach void makeDatabase() throws Exception {
        work = Files.createTempDirectory("backup-sqlite");
        source = new DatabaseService();
        source.setName("Default");
        source.setDatabaseType(DatabaseType.SQLITE);
        source.setConnectionString("jdbc:sqlite:" + work.resolve("Default.db"));
        // no credentials, exactly as the ERP's own SQLite source has none - which is why the manager's set will not hold it
        new DatabaseManager(source);
        source.doUpdate("CREATE TABLE note (ID INTEGER PRIMARY KEY, Message TEXT)");
        source.doUpdate("INSERT INTO note (ID, Message) VALUES (1, 'before the backup')");
        service = new BackupService();
        service.setDirectory(work.resolve("backups").toString());
        service.setIncludedPaths(List.of());
    }
    @AfterEach void removeFolder() { FileUtils.deleteTree(work); }


    @Test void copiesEveryDataSourceAndNamesItInTheManifest() {
        BackupEntry entry = service.take("with a database");
        assertNotNull(entry);
        assertTrue(entry.isComplete(), "nothing should have been skipped: " + entry.skipped());
        assertTrue(entry.sources().contains("Default"), "the source must be listed as copied");
        assertTrue(Files.exists(Path.of(service.getDirectory(), entry.id(), "Default.db")));
    }

    @Test void putsTheRowsBackAsTheyWere() {
        BackupEntry entry = service.take("before the mistake");
        source.doUpdate("UPDATE note SET Message = 'after the mistake' WHERE ID = 1");
        assertEquals("after the mistake", message());

        assertNull(service.restore(entry.id()));
        // the pool still holds the old file underneath, which is why a restart is not optional - a fresh source reads what was written back
        assertEquals("before the backup", messageThroughAFreshConnection());
    }

    private String message() { return source.doQueryValueNoCache(String.class, "SELECT Message FROM note WHERE ID = 1").orElse(null); }
    private String messageThroughAFreshConnection() {
        DatabaseService reopened = new DatabaseService();
        reopened.setName("Reopened");
        reopened.setDatabaseType(DatabaseType.SQLITE);
        reopened.setConnectionString(source.getConnectionString());
        return reopened.doQueryValueNoCache(String.class, "SELECT Message FROM note WHERE ID = 1").orElse(null);
    }
}

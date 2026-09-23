package org.solarframework.backup.spring;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.solarframework.backup.dto.BackupEntry;
import org.solarframework.core.util.FileUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The folder side of a backup: what is copied, what the manifest says, and what a restore puts back. */
class BackupServiceTest {

    private Path root, kept;
    private BackupService service;

    @BeforeEach void makeFolders() throws Exception {
        root = Files.createTempDirectory("backups");
        kept = Files.createTempDirectory("config");
        Files.writeString(kept.resolve("settings.json"), "{\"first\":true}");
        Files.createDirectories(kept.resolve("area"));
        Files.writeString(kept.resolve("area").resolve("nested.txt"), "nested");
        service = new BackupService();
        service.setDirectory(root.toString());
        service.setIncludedPaths(List.of(kept.toString()));
    }
    @AfterEach void removeFolders() {
        FileUtils.deleteTree(root);
        FileUtils.deleteTree(kept);
    }
    /** The id is a timestamp to the second, so two backups taken in the same second would land in one folder. */
    private BackupEntry takeDistinct(String note) throws Exception {
        Thread.sleep(1100);
        return service.take(note);
    }


    @Test void copiesTheFoldersItWasGivenAndWritesAManifest() {
        BackupEntry entry = service.take("first");
        assertNotNull(entry);
        assertTrue(entry.paths().contains(kept.toString()));
        Path copied = root.resolve(entry.id()).resolve("files").resolve(kept.getFileName().toString());
        assertTrue(Files.exists(copied.resolve("settings.json")));
        assertTrue(Files.exists(copied.resolve("area").resolve("nested.txt")), "a folder inside the folder must come too");
        assertTrue(Files.exists(root.resolve(entry.id()).resolve("backup.json")));
        assertTrue(entry.sizeBytes() > 0);
    }

    /** The manifest is read back from the folder, not from anything held in memory, so a backup copied from another machine lists too. */
    @Test void listsFromTheFoldersThemselvesNewestFirst() throws Exception {
        BackupEntry older = service.take("older");
        BackupEntry newer = takeDistinct("newer");
        List<BackupEntry> found = new BackupService() {{ setDirectory(root.toString()); }}.list();
        assertEquals(2, found.size());
        assertEquals(newer.id(), found.getFirst().id());
        assertEquals(older.id(), found.getLast().id());
        assertEquals("older", found.getLast().note());
    }

    @Test void keepsOnlyAsManyAsAsked() throws Exception {
        service.setKeep(2);
        service.take("one");
        takeDistinct("two");
        BackupEntry newest = takeDistinct("three");
        assertEquals(2, service.list().size());
        assertEquals(newest.id(), service.list().getFirst().id(), "the oldest goes, never the newest");
    }

    /** NFR-16 asks for a restore to any point in the last week, so the window outranks the count: a busy day taking ten backups must not push yesterday's out. */
    @Test void keepsEverythingInsideTheWindowEvenBeyondTheCount() throws Exception {
        BackupEntry older = service.take("older");
        takeDistinct("newer");
        service.setKeep(1);
        service.setKeepDays(7);
        assertEquals(0, service.prune());
        assertEquals(2, service.list().size());

        age(older, 30);
        assertEquals(1, service.prune(), "beyond the count and older than the window, so it goes");
        assertEquals(1, service.list().size());
    }

    /** Rewrites a manifest as if it had been taken that many days ago - the only way to test a window without waiting a week. */
    private void age(BackupEntry entry, int days) {
        BackupEntry aged = new BackupEntry(entry.id(), Instant.now().minus(days, ChronoUnit.DAYS), entry.note(), entry.sizeBytes(), entry.sources(), entry.paths(), entry.skipped());
        new BackupFile(aged).Write(root.resolve(entry.id()).resolve("backup.json").toString());
    }

    @Test void keepingNothingKeepsEverything() throws Exception {
        service.setKeep(0);
        service.take("one");
        takeDistinct("two");
        assertEquals(2, service.list().size());
    }

    @Test void putsTheFilesBackAsTheyWere() throws Exception {
        BackupEntry entry = service.take("before the edit");
        Files.writeString(kept.resolve("settings.json"), "{\"first\":false}");
        Files.writeString(kept.resolve("area").resolve("nested.txt"), "changed");

        assertNull(service.restore(entry.id()), "a restore that went through reports no trouble");
        assertEquals("{\"first\":true}", Files.readString(kept.resolve("settings.json")));
        assertEquals("nested", Files.readString(kept.resolve("area").resolve("nested.txt")));
    }

    /** Asking for one that is not there is an ordinary mistake, not a crash - the message is shown to whoever asked. */
    @Test void saysSoWhenThereIsNoSuchBackup() {
        String trouble = service.restore("2020-01-01_00-00-00");
        assertNotNull(trouble);
        assertTrue(trouble.contains("2020-01-01_00-00-00"));
        assertFalse(service.delete("2020-01-01_00-00-00"));
        assertNull(service.get("2020-01-01_00-00-00"));
    }

    @Test void deleteRemovesTheWholeFolder() {
        BackupEntry entry = service.take("throwaway");
        assertTrue(service.delete(entry.id()));
        assertFalse(Files.exists(root.resolve(entry.id())));
        assertTrue(service.list().isEmpty());
    }

    /** The settings a screen may change have to survive a restart, the same way the mailboxes do. */
    @Test void settingsSurviveTheFile() throws Exception {
        Path file = root.resolve("settings.json");
        service.setKeep(3);
        service.SaveAsFile(file.toString());

        service.setKeepDays(7);
        service.SaveAsFile(file.toString());

        BackupService reloaded = new BackupService();
        reloaded.LoadFromFile(file.toString());
        assertEquals(3, reloaded.getKeep());
        assertEquals(7, reloaded.getKeepDays());
        assertEquals(root.toString(), reloaded.getDirectory());
        assertEquals(List.of(kept.toString()), reloaded.getIncludedPaths());
    }

    @Test void readsTheAddressOutOfAJdbcString() {
        BackupService.JdbcTarget at = BackupService.JdbcTarget.of("jdbc:mariadb://db.example.com:3307/solarerp?useSSL=false");
        assertEquals("db.example.com", at.host());
        assertEquals("3307", at.port());
        assertEquals("solarerp", at.database());
        assertEquals("3306", BackupService.JdbcTarget.of("jdbc:mysql://localhost/solarerp").port(), "no port written means the usual one");
        assertNull(BackupService.JdbcTarget.of("jdbc:mariadb://localhost:3306/"), "an address naming no database cannot be dumped");
        assertNull(BackupService.JdbcTarget.of("jdbc:sqlite:Default.db"));
    }
}

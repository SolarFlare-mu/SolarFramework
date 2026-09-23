package org.solarframework.db.test;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.solarframework.core.util.FileUtils;
import org.solarframework.db.api.DatabaseType;
import org.solarframework.db.spring.DatabaseManager;
import org.solarframework.db.spring.DatabaseService;
import org.solarframework.db.test.obj.Sale;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@code exportCsv} against a real SQLite file - the engine SolarERP runs on by default, and the one whose storage decides
 * what an export prints: it keeps no DECIMAL or TIMESTAMP of its own, so what comes back is what Hibernate chose to bind.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CsvExportTest {

    private Path work;
    private DatabaseManager manager;

    @BeforeAll void exportOnce() throws Exception {
        work = Files.createTempDirectory("csv-export");
        DatabaseService source = new DatabaseService();
        source.setName("Default");
        source.setDatabaseType(DatabaseType.SQLITE);
        source.setConnectionString("jdbc:sqlite:" + work.resolve("Default.db"));
        // SQLite ignores them, but the manager only keeps - and scans entities for - a source that has both
        source.setUsername("sa");
        source.setPassword("test");
        manager = new DatabaseManager(source);
        source.createSchemaIfMissing(List.of(Sale.class));
        new Sale(2L, "B-2", new BigDecimal("10.00"), 0.00001, LocalDateTime.of(2017, 10, 2, 10, 56, 33)).Upsert();
        new Sale(1L, "A-1", new BigDecimal("1234.56"), 1.5, null).Upsert();

        source.doUpdate("CREATE TABLE note (b TEXT, a INTEGER, c TEXT)");
        source.doUpdate("INSERT INTO note VALUES ('zeta', 1, 'plain'), ('alpha', 2, 'has, comma'), ('mid', 3, 'say \"hi\"'), ('nil', NULL, NULL)");
        source.doUpdate("INSERT INTO note VALUES ('beta', 4, ?)", "two\nlines");
        source.doUpdate("CREATE TABLE pair (x INTEGER, y TEXT, v REAL, PRIMARY KEY (y, x))");
        source.doUpdate("INSERT INTO pair VALUES (2, 'b', 0.00000123), (1, 'b', 1e21), (9, 'a', 12.5)");
        source.doUpdate("CREATE TABLE counter (id INTEGER PRIMARY KEY AUTOINCREMENT, n INTEGER)");
        source.doUpdate("INSERT INTO counter (n) VALUES (7)");
        source.doUpdate("CREATE VIEW note_view AS SELECT * FROM note");

        source.doUpdate("CREATE TABLE typed_by_hand (id INTEGER PRIMARY KEY, at TIMESTAMP)");
        source.doUpdate("INSERT INTO typed_by_hand VALUES (1, '2017-10-02 10:56:33'), (2, 'not a date')");

        source.exportCsv(work.resolve("out"));
    }
    @AfterAll void removeFolder() {
        manager.close();
        FileUtils.deleteTree(work);
    }

    private String read(String table) throws Exception {
        return Files.readString(work.resolve("out").resolve(table + ".csv"), StandardCharsets.UTF_8);
    }


    @Test void writesOneFilePerTableButNoViewAndNoSqliteInternalTable() {
        Path out = work.resolve("out");
        for (String t : List.of("csv_sale", "note", "pair", "counter")) assertTrue(Files.exists(out.resolve(t + ".csv")), t + ".csv is missing");
        assertFalse(Files.exists(out.resolve("note_view.csv")), "a view is not a table");
        assertFalse(Files.exists(out.resolve("sqlite_sequence.csv")), "AUTOINCREMENT's bookkeeping table is SQLite's, not the data's");
    }

    @Test void ordersByTheWholePrimaryKeyInKeyOrderElseByTheFirstColumn() throws Exception {
        assertEquals("x,y,v\n9,a,12.5\n1,b,1000000000000000000000\n2,b,0.00000123\n", read("pair"), "(y, x) is the key, so y sorts first although x is declared first");
        assertTrue(read("note").startsWith("b,a,c\nalpha,"), "no key: sorted by the first column, b, not by insertion or by a");
    }

    @Test void quotesCommaQuoteAndNewlineAndLeavesNullEmpty() throws Exception {
        assertEquals("b,a,c\nalpha,2,\"has, comma\"\nbeta,4,\"two\nlines\"\nmid,3,\"say \"\"hi\"\"\"\nnil,,\nzeta,1,plain\n", read("note"));
    }

    @Test void writesNoByteOrderMarkAndNoCarriageReturn() throws Exception {
        byte[] bytes = Files.readAllBytes(work.resolve("out").resolve("note.csv"));
        assertEquals('b', bytes[0]);
        for (byte b : bytes) assertNotEquals('\r', b);
    }

    @Test void anEntityRowKeepsItsColumnNamesAndPrintsDecimalsAndTimestampsPlainly() throws Exception {
        String csv = read("csv_sale");
        List<String> lines = csv.lines().toList();
        List<String> header = List.of(lines.getFirst().split(","));
        assertTrue(header.containsAll(List.of("ID", "sale_code", "price", "weight", "purchased_at", "CreatedAt", "UpdatedAt", "DeletedAt")), "column names exactly as mapped: " + header);
        String[] first = lines.get(1).split(",", -1), second = lines.get(2).split(",", -1);
        assertEquals("1", first[header.indexOf("ID")], "ordered by the key, not by insertion");
        assertEquals("", first[header.indexOf("purchased_at")], "NULL is an empty field");
        assertEquals("", first[header.indexOf("DeletedAt")]);
        assertEquals("1234.56", first[header.indexOf("price")]);
        assertEquals("1.5", first[header.indexOf("weight")]);
        assertEquals("0.00001", second[header.indexOf("weight")], "never 1.0E-5");
        assertEquals("10", second[header.indexOf("price")], "numeric(38,2) has NUMERIC affinity in SQLite: 10.00 is stored as the integer 10, 1234.56 as a REAL");
        assertEquals("2017-10-02 10:56:33", second[header.indexOf("purchased_at")], "stored as epoch millis, printed as the wall-clock time the entity held");
        assertTrue(second[header.indexOf("CreatedAt")].matches("\\d{4}-\\d\\d-\\d\\d \\d\\d:\\d\\d:\\d\\d(\\.\\d+)?"), second[header.indexOf("CreatedAt")]);
    }

    @Test void aTimestampColumnHoldingTextIsPrintedAsStoredWhenTheDriverCannotReadIt() throws Exception {
        String csv = read("typed_by_hand");
        assertTrue(csv.startsWith("id,at\n1,2017-10-02 10:56:33"), csv);
        assertTrue(csv.endsWith("\n2,not a date\n"), csv);
    }
}

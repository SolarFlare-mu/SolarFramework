package org.solarframework.db.test;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.solarframework.db.api.dto.TableStats;
import org.solarframework.db.test.obj.Order;

import static org.junit.jupiter.api.Assertions.*;
import static org.solarframework.db.spring.DatabaseRegistry.SolarDBManager;

/**
 * What the schema itself reports, read back off a real database rather than off a hand-built object.
 *
 * <p>Hibernate creates this H2 schema from the same mappings the platform uses everywhere, so the foreign
 * key on {@code orders.UserID} is a real constraint here - which is the only way to exercise the
 * {@code getImportedKeys} path. It is worth knowing that SQLite reports nothing of the sort: it cannot add
 * a constraint to a table that already exists, so no SolarERP installation running on SQLite declares one.
 */
@SolarH2Test
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TableStatsSchemaTest {

    @BeforeAll
    void createSchema() {
        SolarDBManager.createAllSchemasIfMissing();
        SolarDBManager.verifyEntities();
    }

    private static TableStats orders() {
        return SolarDBManager.getTableStats(Order.class);
    }

    @Test
    void hibernateColumnCommentsAreReturnedAsDatabaseRemarks() {
        assertEquals("Time the record was created.", orders().getColumn("CreatedAt").getRemarks());
    }


    /** A key read off the schema names the table it points at, so nothing has to infer it from how the column is spelled. */
    @Test
    void theSchemaReportsTheForeignKeyItDeclares() {
        TableStats.Reference key = orders().getReferences().stream().filter(r -> "userid".equalsIgnoreCase(r.getColumn())).findFirst().orElse(null);
        assertNotNull(key, "orders.UserID is mapped @ManyToOne, so the schema should carry a constraint for it: " + orders().getReferences());
        assertEquals("user", key.getTable());
        assertEquals("id", key.getReferencedColumn().toLowerCase());
    }

    /** The same key, reached from the column rather than from the table's list. */
    @Test
    void theColumnHoldingAKeyKnowsWhereItPoints() {
        TableStats.Column userId = orders().getColumn("UserID");
        assertNotNull(userId, "the column is asked for by the entity's casing, not the database's");
        assertTrue(userId.isForeignKey());
        assertEquals("user", userId.getReferencesTable());
        assertFalse(orders().getColumn("Item").isForeignKey());
    }

    /** Read once from the driver so no screen has to guess either: where a column sits, and what it was really created as. */
    @Test
    void everyColumnCarriesItsPositionAndItsRealType() {
        for (TableStats.Column column : orders().getColumns())
            assertTrue(column.getOrdinalPosition() > 0, column.getName() + " should know where it sits in the table");
        assertTrue(orders().getColumn("Item").getStorageType().contains("CHAR"), orders().getColumn("Item").getStorageType());
        assertFalse(orders().getColumn("Amount").getStorageType().contains("("), "a whole-number column has no width worth reading: " + orders().getColumn("Amount").getStorageType());
    }
}

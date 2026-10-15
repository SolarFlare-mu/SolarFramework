package org.solarframework.db.test;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.solarframework.db.test.obj.User;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.solarframework.db.spring.DatabaseRegistry.SolarDBManager;

/** The data API reads a column list, not "*"; a date column named in that list must still reach its field. */
@SolarH2Test
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PartialColumnReadTest {

    @BeforeAll void createSchema() {
        SolarDBManager.createAllSchemasIfMissing();
        SolarDBManager.verifyEntities();
    }

    @Test void aColumnListKeepsTheCreationTime() {
        new User(7L, "Loic", "loic@gmail.com").Upsert();
        User read = SolarDBManager.getById("ID, Name, CreatedAt, UpdatedAt", User.class, 7L).orElseThrow();
        assertEquals("Loic", read.getName());
        assertNotNull(read.getCreatedAt(), "CreatedAt was in the column list");
        assertNotNull(read.getUpdatedAt());
    }
}

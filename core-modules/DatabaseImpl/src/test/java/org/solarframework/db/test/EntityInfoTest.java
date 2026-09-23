package org.solarframework.db.test;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.junit.jupiter.api.Test;
import org.solarframework.db.api.IEntityInfo;
import org.solarframework.db.api.dto.TableStats;
import org.solarframework.db.spring.EntityInfo;
import org.solarframework.db.test.obj.Order;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** What an entity can answer about itself once it is scanned. A reader holding only an IEntityInfo has no Field left to reflect on, so anything it needs has to be taken off the field while the scan still can. */
class EntityInfoTest {

    private static final IEntityInfo ORDER = new EntityInfo(Order.class, "test");

    private static IEntityInfo.FieldInfo column(String columnName) {
        return ORDER.getFields().stream().filter(f -> f.getColumnName().equals(columnName)).findFirst().orElseThrow();
    }


    @Test
    void anEntityCarriesItsTableDescription() {
        assertEquals("Orders placed by users.", ORDER.getTableComment());
    }

    /** The dual-pattern key: the column UserID holds the value and the @ManyToOne mapped beside it is called user, so one names the other. */
    @Test
    void aKeyColumnNamesTheRelationMappedBesideIt() {
        assertEquals("user", column("UserID").getRelationProperty());
        assertEquals("", column("Item").getRelationProperty());
        assertEquals("", column("ID").getRelationProperty(), "a bare ID is the table's own key, not a key into another table");
        assertEquals("user", ORDER.getRelationOf(column("UserID")).getVariableName());
        assertNull(ORDER.getRelationOf(column("Item")));
    }

    /** A key is followed to a table, so the relation names that table itself rather than leaving every reader to look the class up in the registry. */
    @Test
    void aRelationNamesTheTableAndTheClassAtItsOtherEnd() {
        IEntityInfo.Relation user = ORDER.getRelationOf(column("UserID"));
        assertEquals("user", user.getForeignTableName());
        assertEquals("User", user.getForeignSimpleClassName());
        assertEquals("org.solarframework.db.test.obj.User", user.getForeignClassName());
    }

    /** A column the database has not got is answered for out of the declaration alone, and says so rather than reading as though it were there. */
    @Test
    void aColumnFallsBackToItsDeclarationWhereTheSchemaHasNotGotIt() {
        IEntityInfo.FieldInfo field = column("UserID");
        IEntityInfo.ColumnInfo declaredOnly = new IEntityInfo.ColumnInfo(field, null, ORDER.getRelationOf(field));
        assertFalse(declaredOnly.isInDatabase());
        assertNull(declaredOnly.getStorageType());
        assertEquals("Long", declaredOnly.getSimpleTypeName());
        assertEquals("java.lang.Long", declaredOnly.getJavaTypeName());
        assertEquals("user", declaredOnly.getReferencesTable(), "no constraint is declared, so the mapping beside the column answers");
        assertEquals(ORDER.getFields().size(), ORDER.getColumns().size());
    }

    /** Where the two disagree the schema wins, because it is the truth of what is stored - but only for what it can actually answer. */
    @Test
    void theSchemaOutranksTheDeclarationWhereItHasAnAnswer() {
        IEntityInfo.FieldInfo field = column("UserID");
        TableStats.Column schema = new TableStats.Column();
        schema.name = "userid"; schema.type = "varchar"; schema.size = 64; schema.nullable = false; schema.isUnique = true;
        IEntityInfo.ColumnInfo both = new IEntityInfo.ColumnInfo(field, schema, ORDER.getRelationOf(field));
        assertTrue(both.isInDatabase());
        assertEquals("VARCHAR(64)", both.getStorageType());
        assertFalse(both.isNullable(), "the @Column annotation leaves it nullable; the schema says otherwise and is believed");
        assertTrue(both.isUnique(), "nothing on the field declares it unique; the schema does");
        assertEquals("Long", both.getSimpleTypeName(), "the declaration still answers for what only the class knows");
    }

    /**
     * An entity is inlined straight into a page's JS state - the Database Manager does exactly that with the
     * built-in entities - so every public getter it grows lands in that payload. A getter that reads the schema
     * would put a second copy of the whole TableStats in there and query the database to build it, so anything
     * beyond the list below has to be kept out deliberately.
     */
    @Test
    void everyGetterOnAnEntityIsEitherMeantForThePayloadOrKeptOutOfIt() {
        Set<String> serialised = Set.of("getClassName", "getSimpleClassName", "getTableName", "getTableComment", "getClassLoader", "getFields", "getRelations", "getBinaryColumns", "getEntityClass", "getTableStats");
        for (Method method : IEntityInfo.class.getDeclaredMethods()) {
            if (!Modifier.isPublic(method.getModifiers()) || method.getParameterCount() != 0 || !method.getName().startsWith("get")) continue;
            if (serialised.contains(method.getName())) continue;
            assertNotNull(method.getAnnotation(JsonIgnore.class), method.getName() + " is new on IEntityInfo: either add it to the serialised set in this test on purpose, or mark it @JsonIgnore");
        }
    }

    /** One place spells a column's type, so no screen assembles one of its own - and a width is only shown where it means something to a reader. */
    @Test
    void aColumnSpellsItsOwnTypeAndOnlyShowsAWidthThatMeansSomething() {
        assertEquals("VARCHAR(255)", detail("varchar", 255, 0).getStorageType());
        assertEquals("DECIMAL(10,2)", detail("decimal", 10, 2).getStorageType());
        assertEquals("BIGINT", detail("bigint", 19, 0).getStorageType(), "a BIGINT's width says nothing anyone wants to read");
        assertEquals("VARCHAR(64)", detail("VARCHAR(64)", 64, 0).getStorageType(), "SQLite hands back the whole declaration already");
    }

    /** A migration normalises temporal values rather than copying them across, so missing one corrupts the copy. MariaDB reports sub-second precision in the type name itself. */
    @Test
    void aTemporalColumnIsRecognisedEvenWhenItsTypeCarriesAPrecision() {
        TableStats.Column withPrecision = detail("DATETIME(3)", 0, 0);
        withPrecision.dataType = java.sql.Types.TIMESTAMP;
        assertTrue(withPrecision.isTemporal(), "the name alone would miss this one; the Types code does not");
        assertTrue(detail("timestamp", 0, 0).isTemporal(), "a detail built by hand carries no Types code, so the name still has to answer");
        assertFalse(detail("varchar", 255, 0).isTemporal());
    }

    private static TableStats.Column detail(String type, int size, int decimalDigits) {
        TableStats.Column column = new TableStats.Column();
        column.name = "c"; column.type = type; column.size = size; column.decimalDigits = decimalDigits;
        return column;
    }

    /** A column reading only VARCHAR hides the whole rule, and the constants are gone once the scan has finished. */
    @Test
    void anEnumColumnCarriesItsConstantsAndOthersCarryNone() {
        assertEquals("Status", column("Status").getSimpleTypeName());
        assertEquals(List.of("DONE", "PENDING"), column("Status").getEnumConstants());
        assertEquals("String", column("Item").getSimpleTypeName());
        assertTrue(column("Item").getEnumConstants().isEmpty());
    }
}

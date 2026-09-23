package org.solarframework.db.test;

import jakarta.persistence.Column;
import org.junit.jupiter.api.Test;
import org.solarframework.db.api.IEntityInfo;
import org.solarframework.db.api.dto.TableStats;
import org.solarframework.db.spring.DatabaseObject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ColumnCommentTest {
    @Column(name = "DisplayName", comment = "Name shown to other users.")
    String name;

    @Test
    void declarationDescribesColumnsWithoutDatabaseRemarks() throws Exception {
        var field = new IEntityInfo.FieldInfo(getClass().getDeclaredField("name"));
        assertEquals("Name shown to other users.", new IEntityInfo.ColumnInfo(field, null, null).getComment());
        var column = new TableStats.Column();
        column.remarks = " ";
        assertEquals(field.getComment(), new IEntityInfo.ColumnInfo(field, column, null).getComment());
        column.remarks = "Database description.";
        assertEquals(column.remarks, new IEntityInfo.ColumnInfo(field, column, null).getComment());
    }
    @Test
    void inheritedColumnsHaveDescriptions() throws Exception {
        assertFalse(new IEntityInfo.FieldInfo(DatabaseObject.ID_OBJ.class.getDeclaredField("ID")).getComment().isBlank());
        assertFalse(new IEntityInfo.FieldInfo(DatabaseObject.RECORD_OBJ.class.getDeclaredField("createdAt")).getComment().isBlank());
    }
}

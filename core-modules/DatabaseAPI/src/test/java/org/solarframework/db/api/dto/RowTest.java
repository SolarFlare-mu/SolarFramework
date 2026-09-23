package org.solarframework.db.api.dto;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RowTest {

    private static Row row() {
        Map<String, Object> columns = new LinkedHashMap<>();
        columns.put("ID", 7L);
        columns.put("SuperiorID", null);
        return new Row(columns);
    }

    /** A NULL cell used to throw from findFirst() and take the whole listing down with it. */
    @Test
    void valueOfAnswersNullForANullCell() {
        assertNull(row().valueOf("SuperiorID"));
    }

    @Test
    void valueOfIgnoresTheCaseTheDriverAnswered() {
        assertEquals(7L, row().valueOf("id"));
    }

    @Test
    void valueOfAnswersNullForAColumnTheRowHasNot() {
        assertNull(row().valueOf("Missing"));
    }
}

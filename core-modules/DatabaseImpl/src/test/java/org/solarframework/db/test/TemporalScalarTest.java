package org.solarframework.db.test;

import org.junit.jupiter.api.Test;
import org.solarframework.db.spring.DatabaseService;

import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** A DATETIME column read by column name (the data API's partial select) reaches the mapper as a Timestamp; an Instant field must not be left null. */
class TemporalScalarTest {

    private Object convert(Object value, Class<?> type) throws Exception {
        Method method = DatabaseService.class.getDeclaredMethod("convertScalar", Object.class, Class.class);
        method.setAccessible(true);
        return method.invoke(new DatabaseService(), value, type);
    }

    @Test void anOffsetDateTimeBecomesTheSameInstant() throws Exception { // what Hibernate hands back for a DATETIME on H2 and MariaDB
        OffsetDateTime stamp = OffsetDateTime.parse("2026-09-29T10:15:30.123456+04:00");
        assertEquals(stamp.toInstant(), convert(stamp, Instant.class));
    }

    @Test void aTimestampBecomesTheInstantAndTheLocalDateTimeItWas() throws Exception {
        Timestamp stamp = Timestamp.valueOf("2026-09-29 10:15:30.123456");
        assertEquals(stamp.toInstant(), convert(stamp, Instant.class));
        assertEquals(LocalDateTime.of(2026, 9, 29, 10, 15, 30, 123456000), convert(stamp, LocalDateTime.class));
    }
}

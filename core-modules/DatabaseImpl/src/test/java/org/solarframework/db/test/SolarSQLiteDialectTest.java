package org.solarframework.db.test;

import org.junit.jupiter.api.Test;
import org.solarframework.db.api.DatabaseType;
import org.solarframework.db.spring.SolarSQLiteDialect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Guards the fix for real SQLite having no "comment on column/table" statement, unlike Hibernate's
 * community SQLiteDialect which claims it does (org.hibernate.community.dialect.SQLiteDialect.supportsCommentOn()
 * = true) and so tries to run one for every @Column/@Table comment on a brand-new table, failing with
 * "[SQLITE_ERROR] SQL error or missing database (near "comment": syntax error)" on every schema sync.
 */
class SolarSQLiteDialectTest {
    @Test
    void doesNotClaimCommentOnSupport() {
        assertFalse(new SolarSQLiteDialect().supportsCommentOn());
    }
    @Test
    void databaseTypeUsesTheFixedDialect() {
        assertEquals(SolarSQLiteDialect.class.getName(), DatabaseType.SQLITE.getDialectClass());
    }
}

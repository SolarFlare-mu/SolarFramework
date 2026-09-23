package org.solarframework.db.spring;

import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;

/**
 * Hibernate's community SQLiteDialect claims supportsCommentOn() = true, so it emits "comment on column/table
 * ... is '...'" DDL for an @Column/@Table comment - a statement real SQLite has never had, so every brand-new
 * table with a commented column fails its comment step (harmlessly logged, but noisy) on every schema sync.
 */
public class SolarSQLiteDialect extends org.hibernate.community.dialect.SQLiteDialect {
    public SolarSQLiteDialect() { super(); }
    public SolarSQLiteDialect(DialectResolutionInfo info) { super(info); }
    public SolarSQLiteDialect(DatabaseVersion version) { super(version); }

    @Override
    public boolean supportsCommentOn() { return false; }
}

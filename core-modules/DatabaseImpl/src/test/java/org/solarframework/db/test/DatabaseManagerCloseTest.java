package org.solarframework.db.test;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.solarframework.db.api.DatabaseType;
import org.solarframework.db.spring.DatabaseManager;
import org.solarframework.db.spring.DatabaseService;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

/** A host that closes its context in-process and starts another must get the old pools back, or every restart keeps one more of each. */
class DatabaseManagerCloseTest {

    @Test
    void closingTheManagerClosesThePoolItOpenedAndTheNextUseOpensAFreshOne() throws SQLException {
        DatabaseService source = new DatabaseService();
        source.setConnectionString("jdbc:h2:mem:solarclose;DB_CLOSE_DELAY=-1");
        source.setUsername("sa");
        source.setPassword("test");
        source.setDatabaseType(DatabaseType.H2);
        DatabaseManager manager = new DatabaseManager(source);
        HikariDataSource pool = source.getDataSource().unwrap(HikariDataSource.class);
        assertFalse(pool.isClosed());

        manager.close();
        assertTrue(pool.isClosed(), "the pool the manager opened is still running after close()");
        assertNotSame(pool, source.getDataSource().unwrap(HikariDataSource.class), "a source asked again after close() must not hand back the closed pool");
    }
}

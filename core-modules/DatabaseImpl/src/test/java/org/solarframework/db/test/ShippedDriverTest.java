package org.solarframework.db.test;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.solarframework.db.api.DatabaseType;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverPropertyInfo;
import java.util.Properties;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.solarframework.db.spring.DatabaseRegistry.SolarDBManager;

/**
 * A JDBC driver that arrives inside a module rather than on the host's own classpath.
 *
 * <p>The host's classloader is the parent of every plugin's, so it can never load such a class by name -
 * which is what both the pool and DriverManager would try. The only way through is the loaders the plugins
 * registered here, and these tests hold that route open: they use a driver name that is deliberately absent
 * from this module's classpath, so anything that quietly falls back to loading by name fails them.
 */
@SolarH2Test
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ShippedDriverTest {

    private static final String SHIPPED = "org.solarframework.shipped.NoSuchDriver";
    private static final String UNINSTALLED = "org.solarframework.shipped.DriverNobodyCarries";

    @AfterAll
    void forgetTheModule() {
        SolarDBManager.getEntityClassloaders().remove("shipped-driver-test");
    }

    /** Stands in for the jar inside a module: it answers for one class and, like a plugin's loader, is invisible from the host's. */
    private static ClassLoader moduleCarrying(String driverClass) {
        return new ClassLoader(null) {
            @Override
            public Class<?> loadClass(String name) throws ClassNotFoundException {
                if (driverClass.equals(name)) return ShippedDriver.class;
                throw new ClassNotFoundException(name);
            }
        };
    }

    @Test
    void aDriverNoModuleCarriesIsNotFound() {
        assertNull(SolarDBManager.findDriver(UNINSTALLED), "nothing installed carries this driver, so nothing should be built");
    }
    @Test
    void aDriverIsBuiltFromTheModuleThatCarriesIt() {
        SolarDBManager.addEntityClassLoader("shipped-driver-test", moduleCarrying(SHIPPED));
        Driver found = SolarDBManager.findDriver(SHIPPED);
        assertInstanceOf(ShippedDriver.class, found, "the driver has to come from the module's own loader - the host cannot load this name at all");
        assertSame(found, SolarDBManager.findDriver(SHIPPED), "built once and kept, rather than a new driver per pool");
    }


    /** What the database type dropdown is built from: offering a type whose driver was never installed only produces a connection that cannot open. */
    @Test
    void onlyTypesWithAnInstalledDriverAreOffered() {
        assertTrue(DatabaseType.H2.isDriverAvailable(), "H2 is on this module's classpath");
        assertFalse(DatabaseType.ORACLE.isDriverAvailable(), "nothing here ships Oracle's driver, and it cannot be redistributed with the platform either");
        assertTrue(DatabaseType.available().contains(DatabaseType.H2));
        assertFalse(DatabaseType.available().contains(DatabaseType.ORACLE));
    }

    public static class ShippedDriver implements Driver {
        @Override public Connection connect(String url, Properties info) { return null; }
        @Override public boolean acceptsURL(String url) { return true; }
        @Override public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) { return new DriverPropertyInfo[0]; }
        @Override public int getMajorVersion() { return 1; }
        @Override public int getMinorVersion() { return 0; }
        @Override public boolean jdbcCompliant() { return false; }
        @Override public Logger getParentLogger() { return Logger.getGlobal(); }
    }
}

package org.solarframework.db.test.transactional;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.solarframework.db.api.IEntityInfo;
import org.solarframework.db.spring.*;
import org.solarframework.db.test.SolarH2Test;
import org.solarframework.db.test.obj.PluginLoadedRecord;
import org.solarframework.db.test.obj.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.solarframework.db.spring.DatabaseRegistry.SolarDBManager;

@SolarH2Test
class JpaSourceRegistrarTest {

    @Autowired
    ConfigurableApplicationContext context;

    @Test
    void registersAFunctionalEntityManagerFactory() {
        SolarDBManager.createAllSchemasIfMissing();
        DatabaseService source = (DatabaseService) SolarDBManager.getDefaultService();

        JpaSourceRegistrar.JpaSourceBeans beans = JpaSourceRegistrar.register(source, context);

        assertNotNull(beans.entityManagerFactory());
        assertTrue(beans.entityManagerFactory().isOpen());
        assertSame(beans, source.getJpaBeans(), "DatabaseService must hold onto the beans it was just given");

        assertTrue(context.getBeanFactory().containsSingleton(source.jpaBeanName() + "_emf"));
        assertTrue(context.getBeanFactory().containsSingleton(source.jpaBeanName() + "_txManager"));

        try (EntityManager em = beans.entityManagerFactory().createEntityManager()) {
            em.getTransaction().begin();
            User u = new User(9001L, "Registrar", "registrar@example.com");
            u.setCreatedAt(java.time.Instant.now());
            u.setUpdatedAt(java.time.Instant.now());
            em.persist(u);
            em.getTransaction().commit();
        }

        assertTrue(SolarDBManager.getById(User.class, 9001L).isPresent(), "a row written through the new EMF must be visible through the existing native-SQL read path too - same table, same connection");
    }

    @Test
    void transactionManagerRecognizesPluginLoadedEntities() throws Exception {
        DatabaseService source = (DatabaseService) SolarDBManager.getDefaultService();
        List<IEntityInfo> originalEntities = List.copyOf(source.getEntities());
        ClassLoader pluginLoader = new ChildFirstEntityLoader(PluginLoadedRecord.class);
        Class<?> pluginRecord = pluginLoader.loadClass(PluginLoadedRecord.class.getName());
        try {
            SolarDBManager.addEntityClassLoader("plugin-test", pluginLoader);
            source.setEntities(List.of(new EntityInfo(pluginRecord, "plugin-test")));
            source.createSchemaIfMissing(List.of(pluginRecord));

            JpaSourceRegistrar.JpaSourceBeans beans = JpaSourceRegistrar.register(source, context);
            TransactionTemplate tx = new TransactionTemplate(beans.transactionManager());
            Constructor<?> constructor = pluginRecord.getConstructor(Long.class, String.class);

            tx.execute(status -> {
                try {
                    ((DatabaseObject<?>) constructor.newInstance(777L, "Plugin row")).Upsert();
                    return null;
                } catch (ReflectiveOperationException e) {
                    throw new RuntimeException(e);
                }
            });

            assertEquals(1, source.doQueryValue(Long.class, "SELECT COUNT(*) FROM plugin_loaded_record WHERE ID = ?", 777L).orElseThrow());
        } finally {
            source.setEntities(originalEntities);
        }
    }

    @Test
    void unregisterClosesTheFactoryAndRemovesTheBeans() {
        DatabaseService source = (DatabaseService) SolarDBManager.getDefaultService();
        JpaSourceRegistrar.JpaSourceBeans beans = JpaSourceRegistrar.register(source, context);
        String emfName = source.jpaBeanName() + "_emf";
        String txName = source.jpaBeanName() + "_txManager";

        JpaSourceRegistrar.unregister(source, context);

        assertFalse(beans.entityManagerFactory().isOpen(), "unregister must close the factory, not just drop the bean reference");
        assertFalse(context.getBeanFactory().containsSingleton(emfName));
        assertFalse(context.getBeanFactory().containsSingleton(txName));
        // Not asserting source.getJpaBeans() == null here on purpose: that getter lazily rebuilds on access
        // (see registrationIsLazyUntilSomethingActuallyAsksForIt below), so calling it would immediately
        // build a fresh one rather than observe the torn-down state - TransactionResolver.isBound, which
        // reads the raw field, is the correct way to observe "nothing built yet" from outside this package.
        assertFalse(TransactionResolver.isBound(source), "no transaction is open, so this must read as unbound regardless of whether beans exist");
    }

    @Test
    void registrationIsLazyUntilSomethingActuallyAsksForIt() {
        DatabaseService source = (DatabaseService) SolarDBManager.getDefaultService();
        JpaSourceRegistrar.unregister(source, context); // start from a clean slate regardless of prior tests in this class

        assertFalse(TransactionResolver.isBound(source), "isBound must never itself trigger the lazy build - a source nothing uses should stay unregistered");
        assertFalse(context.getBeanFactory().containsSingleton(source.jpaBeanName() + "_emf"), "no EntityManagerFactory should exist yet");

        JpaSourceRegistrar.JpaSourceBeans beans = source.getJpaBeans(); // the one call that IS allowed to build it

        assertNotNull(beans);
        assertTrue(context.getBeanFactory().containsSingleton(source.jpaBeanName() + "_emf"), "getJpaBeans() must have built it on this call");
    }

    private static class ChildFirstEntityLoader extends ClassLoader {
        private final String entityName;

        ChildFirstEntityLoader(Class<?> entity) {
            super(entity.getClassLoader());
            this.entityName = entity.getName();
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (!name.equals(entityName)) return super.loadClass(name, resolve);
            Class<?> loaded = findLoadedClass(name);
            if (loaded == null) loaded = findClass(name);
            if (resolve) resolveClass(loaded);
            return loaded;
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            String resource = "/" + name.replace('.', '/') + ".class";
            try (InputStream in = PluginLoadedRecord.class.getResourceAsStream(resource)) {
                if (in == null) throw new ClassNotFoundException(name);
                byte[] bytes = in.readAllBytes();
                return defineClass(name, bytes, 0, bytes.length);
            } catch (IOException e) {
                throw new ClassNotFoundException(name, e);
            }
        }
    }
}

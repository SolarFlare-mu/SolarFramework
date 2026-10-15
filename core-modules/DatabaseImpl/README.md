# DatabaseImpl

`org.solarframework.mu:database-impl` — [`DatabaseAPI`](../DatabaseAPI/README.md) over Hibernate, Hikari and Caffeine. Drivers on the classpath: MariaDB, H2, SQLite (with `SolarSQLiteDialect`).

## Two ways in
**No class carries a Spring stereotype**, because the stack also runs inside a plugin with no application context. `DatabaseConfig` is the one `@Configuration` for a host that wants beans; `Database_PlainMain` and `Database_Main` (under `src/test`) are the same script on each path.

```java
DatabaseService source = new DatabaseService();
source.setConnectionString("jdbc:h2:mem:demo;DB_CLOSE_DELAY=-1");
source.setUsername("sa"); source.setPassword("");
source.setDatabaseType(DatabaseType.H2);
new DatabaseManager(source);        // the whole setup; fills DatabaseRegistry
SolarDBManager.verifyEntities();
```

What only a context adds: `@Cacheable`/`@Transactional` interception, and the per-source JPA beans `JpaSourceRegistrar` registers. With none, `DatabaseManager.getContext()` is null, `TransactionResolver` always answers "not bound", and every read and write takes the SessionFactory path — the ordinary path anyway. Manual caching works either way: `DatabaseConfig.databaseCaches()` builds the three caches with no container, and the no-context constructor calls it.

## One `DatabaseService` is one datasource is one Hikari pool
`DatabaseService` owns its `HikariDataSource` privately, built lazily in `getDataSource()` and named `MyHikariPool-<source name>`. The framework is multi-source by design — SolarERP defines several schemas through its `Data-DatabaseManager` page — so pool settings are **per source**, configured two separate ways:

- **The default source** is the bean `DatabaseConfig` contributes, seeded from `application.properties` through `@Value("${spring.datasource.hikari.*}")`.
- **Every other source** is built by `DatabaseManager.makeNewSource` (not a bean, so every `@Value` field is null) or deserialised by `DataSourceFile` (`config/data/database.json`), and is configured **only** through the setters — which is what SolarERP's `DatabaseUpdater` calls, one per field. The pool fields are plain non-transient fields, so they round-trip through the JSON file.

Two consequences. **A getter's fallback is the real default for every non-default source**, so a fallback of "off" means off everywhere but the one source `application.properties` reaches — keep fallbacks sane rather than zero. And **anything added to the pool config must be added in four places or it silently does nothing**: the `@Value` field + getter + setter on `DatabaseService`, the getter + setter on `IDatabaseService`, the `config.setX(...)` call in `getDataSource()`, and the consumer's own form/DTO (for SolarERP: `DataSourceDTO.SourceDTO`, `DatabaseManager.toDto`, `DatabaseUpdater`, `manager.html`).

## Host applications
Spring Boot builds a **second, unused** `DataSource` + `EntityManagerFactory` over the same entities unless the host excludes it — `DatabaseService` is a bean but not a `DataSource`, so `@ConditionalOnMissingBean(DataSource.class)` misses it. Exclude `DataSourceAutoConfiguration`, `DataSourceTransactionManagerAutoConfiguration`, `HibernateJpaAutoConfiguration` and `DataJpaRepositoriesAutoConfiguration` (see `Database_Main`). That also makes any `@EntityScan` in the host dead: entities are listed explicitly via `MetadataSources.addAnnotatedClass` / `addManagedClassName`.

## Inside
- **`EntityIdentity`** — one Java object per (connection, entity, id), the first-level cache there is no persistence context to give.
- **`LazyMappedCollection`** — replaces a `@OneToMany` at PostLoad with a collection that loads itself once on first access, honouring the field's own type (`List` or `Set`); Hibernate's own self-faulting is unreachable outside a transaction.
- **`TransactionResolver`** / **`TransactionalAccess`** — the one branch point: is *this source's* transaction bound on this thread.
- **`QueryTranslation`**, **`HibernateNamingStrategy`**, **`DatabaseUtils`**, **`DBInstanceService`**.
- **`db/v1/`** — the older query-builder API (`QueryBuilder`, `TableRow`, `manipulator/`). Nothing else in the framework imports it any more.

Tests: 31 classes, on H2 and SQLite, no server needed. `BackupLiveTest` lives in BackupImpl.

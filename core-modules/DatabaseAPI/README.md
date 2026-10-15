# DatabaseAPI

`org.solarframework.mu:database-api` — the interfaces and the entity base classes. Carries only `jakarta.persistence-api` and Jackson annotations, so an entity module or a plugin can depend on it without pulling Hibernate, Hikari or Spring. The implementation is [`DatabaseImpl`](../DatabaseImpl/README.md); add both.

## What is in it
- **`IDatabaseManager`** — every data source an installation has, read and written as one JSON file.
- **`IDatabaseService`** — one data source: connection, pool settings, entities, and every read/write (`getById`, `getWhere`, `getAll`, `getAllWhere`, `doQuery*`, `doUpdate`, `Count`, `InsertBatch`/`UpsertBatch`, schema checks, stats, `exportCsv`). The `*NoCache` variants skip the cache.
- **`DatabaseRegistry`** — the statics `SolarDBManager` and `DefaultDBService`, filled by DatabaseImpl on both the Spring and the plain path.
- **`DatabaseObject<T>`** — the entity base. An entity extends `DatabaseObject.ID_OBJ<Id, T>` (an `ID` column), `ID_RECORD_OBJ<Id, T>` (ID plus record dates), `RECORD_OBJ<T>`, or `DatabaseObject<T>` itself, and gets `Write`, `WriteThenReturn`, `Upsert`, `UpsertThenReturn`, `UpsertAll`, `Update`, `UpdateOnly`, `IncrementColumn(s)`, `Delete`, `refetchAttribute`, `toJSON`.
- **`Lazy.UNLOADED`** — the third state a lazily read `byte[]` needs (bytes / NULL / never fetched): declare `private byte[] avatar = Lazy.UNLOADED;`.
- **`Identified`**, **`IEntityInfo`**, **`BundleEntities`**, **`DatabaseType`**, `dto/` (`Row`, `DatabaseStats`, `TableStats`), `DataMigrationException`.

```java
@Entity
@Table(name = "user")
public class User extends DatabaseObject.ID_RECORD_OBJ<Long, User> {
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    public List<Order> orders = new ArrayList<>();
    @Column(name = "Name") private String name;
}

user.Upsert();                       // insert, else update
User u = SolarDBManager.getById(User.class, 1).orElseThrow();
u.orders.size();                     // loaded on first access
```

**`IEntityInfo` is deliberately not abstract**: Gson refuses to instantiate an abstract class while deserialising the saved source file.

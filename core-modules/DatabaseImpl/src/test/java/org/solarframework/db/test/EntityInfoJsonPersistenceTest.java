package org.solarframework.db.test;

import com.google.gson.reflect.TypeToken;
import org.junit.jupiter.api.Test;
import org.solarframework.db.api.IEntityInfo;
import org.solarframework.db.spring.EntityInfo;
import org.solarframework.db.test.obj.Order;
import org.solarframework.json.JSONItem;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@code database.json} stores each source's entities as {@code List<IEntityInfo>}. Gson 2.14 refuses to
 * instantiate an abstract class at all when reading that back, and used to throw here every time - silently
 * caught by {@code JSONItem.ReadOrCreate}, which then overwrote the real file with an empty default. This
 * pins the actual mechanism down so a future Gson upgrade, or IEntityInfo turning abstract again, fails loudly
 * in a unit test instead of quietly erasing configured data sources on the next restart.
 */
class EntityInfoJsonPersistenceTest {

    @Test
    void aListOfEntityInfoRoundTripsThroughTheSharedGsonWithoutThrowing() {
        List<IEntityInfo> original = List.of(new EntityInfo(Order.class, "test"));
        String json = JSONItem.SimpleGSON.toJson(original);

        List<IEntityInfo> read = JSONItem.SimpleGSON.fromJson(json, new TypeToken<List<IEntityInfo>>(){}.getType());

        assertEquals(1, read.size());
        assertEquals("org.solarframework.db.test.obj.Order", read.get(0).getClassName());
        assertEquals("orders", read.get(0).getTableName());
        assertFalse(read.get(0).getFields().isEmpty());
    }

    @Test
    void aDeserialisedEntityInfoThrowsRatherThanLieAboutHavingALiveClass() {
        String json = JSONItem.SimpleGSON.toJson(List.of(new EntityInfo(Order.class, "test")));
        IEntityInfo read = JSONItem.SimpleGSON.<List<IEntityInfo>>fromJson(json, new TypeToken<List<IEntityInfo>>(){}.getType()).get(0);

        assertThrows(UnsupportedOperationException.class, read::getEntityClass);
        assertThrows(UnsupportedOperationException.class, read::getTableStats);
    }
}

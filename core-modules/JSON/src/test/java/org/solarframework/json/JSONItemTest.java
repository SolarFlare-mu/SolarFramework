package org.solarframework.json;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import jakarta.persistence.Transient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.*;

import static org.junit.jupiter.api.Assertions.*;

class JSONItemTest {

    static class Dummy extends JSONItem<Dummy> {
        String name;
        LocalDate date;
        LocalTime time;
        LocalDateTime dateTime;
        OffsetDateTime offsetDateTime;
        ZonedDateTime zonedDateTime;
        Instant instant;
        @Transient
        String transientField = "should-not-serialize";
        @JsonIgnore
        String ignoredField = "should-not-serialize-either";

        Dummy() {}
        Dummy(String name) {
            this.name = name;
        }
    }

    @TempDir
    Path tempDir;

    @Test
    void writeThenReadRoundTripsAllFields() {
        Dummy original = new Dummy("Loic");
        original.date = LocalDate.of(2024, 1, 15);
        original.time = LocalTime.of(10, 30, 0);
        original.dateTime = LocalDateTime.of(2024, 1, 15, 10, 30, 0);
        original.offsetDateTime = OffsetDateTime.of(2024, 1, 15, 10, 30, 0, 0, ZoneOffset.ofHours(1));
        original.zonedDateTime = ZonedDateTime.of(2024, 1, 15, 10, 30, 0, 0, ZoneId.of("Europe/Paris"));
        original.instant = Instant.ofEpochSecond(1700000000);

        Path file = tempDir.resolve("dummy.json");
        assertSame(original, original.Write(file.toString()));
        assertFalse(java.nio.file.Files.exists(file.resolveSibling("dummy.json.tmp")));

        Dummy read = JSONItem.ReadJSON(file.toString(), Dummy.class);

        assertNotNull(read);
        assertEquals(original.name, read.name);
        assertEquals(original.date, read.date);
        assertEquals(original.time, read.time);
        assertEquals(original.dateTime, read.dateTime);
        assertEquals(original.offsetDateTime, read.offsetDateTime);
        assertEquals(original.zonedDateTime, read.zonedDateTime);
        assertEquals(original.instant, read.instant);
    }

    @Test
    void transientAndJsonIgnoreFieldsAreExcludedFromOutput() {
        Dummy d = new Dummy("Loic");
        String json = JSONItem.SimpleGSON.toJson(d);
        assertFalse(json.contains("should-not-serialize"));
    }

    @Test
    void readJsonReturnsNullForMissingFile() {
        assertNull(JSONItem.ReadJSON(tempDir.resolve("does-not-exist.json").toString(), Dummy.class));
    }

    static class FixedPathDummy extends JSONItem<FixedPathDummy> {
        static Path file;
        String name = "fixed";
        @Override public String file() { return file.toString(); }
    }

    @Test
    void noArgWriteAndReadUseTheOverriddenFile() {
        FixedPathDummy.file = tempDir.resolve("fixed.json");
        FixedPathDummy original = new FixedPathDummy();
        original.name = "Loic";
        assertSame(original, original.Write());

        FixedPathDummy read = JSONItem.ReadJSON(FixedPathDummy.class);
        assertNotNull(read);
        assertEquals("Loic", read.name);
    }

    @Test
    void readOrCreateWithNoPathWritesAndReturnsTheFallbackWhenMissing() {
        FixedPathDummy.file = tempDir.resolve("does-not-exist.json");
        FixedPathDummy fallback = new FixedPathDummy();
        fallback.name = "fallback";

        FixedPathDummy result = JSONItem.ReadOrCreate(FixedPathDummy.class, fallback);
        assertSame(fallback, result);
        assertEquals("fallback", JSONItem.ReadJSON(FixedPathDummy.class).name);
        assertFalse(java.nio.file.Files.exists(tempDir.resolve("does-not-exist.json.corrupt")));
    }

    @Test
    void readOrCreateWithNoPathReturnsWhatWasAlreadyThereWithoutOverwritingIt() {
        FixedPathDummy.file = tempDir.resolve("already-there.json");
        FixedPathDummy existing = new FixedPathDummy();
        existing.name = "existing";
        existing.Write();

        FixedPathDummy fallback = new FixedPathDummy();
        fallback.name = "fallback";
        FixedPathDummy result = JSONItem.ReadOrCreate(FixedPathDummy.class, fallback);
        assertEquals("existing", result.name);
    }

    @Test void temporalAdaptersPreserveStoredFormatsAndRejectInvalidDates() {
        assertTemporalAdapter(LocalDate.of(2026, 9, 9), "2026-09-09", LocalDate.class, new JSONItem.LocalDateAdapter(), new JSONItem.LocalDateAdapter());
        assertTemporalAdapter(LocalTime.of(10, 30), "10:30:00", LocalTime.class, new JSONItem.LocalTimeAdapter(), new JSONItem.LocalTimeAdapter());
        assertTemporalAdapter(LocalTime.of(10, 30, 1, 123456789), "10:30:01.123456789", LocalTime.class, new JSONItem.LocalTimeAdapter(), new JSONItem.LocalTimeAdapter());
        assertTemporalAdapter(OffsetDateTime.of(2026, 9, 9, 10, 30, 0, 0, ZoneOffset.ofHours(4)), "2026-09-09T10:30:00+04:00", OffsetDateTime.class, new JSONItem.OffsetDateTimeAdapter(), new JSONItem.OffsetDateTimeAdapter());
        assertTemporalAdapter(ZonedDateTime.of(2026, 9, 9, 10, 30, 0, 0, ZoneId.of("Europe/Paris")), "2026-09-09T10:30:00+02:00[Europe/Paris]", ZonedDateTime.class, new JSONItem.ZonedDateTimeAdapter(), new JSONItem.ZonedDateTimeAdapter());
        assertTemporalAdapter(Instant.parse("2026-09-09T06:30:01.123456789Z"), "2026-09-09T06:30:01.123456789Z", Instant.class, new JSONItem.InstantAdapter(), new JSONItem.InstantAdapter());
    }
    /** Direct calls also exercise adapter null handling, which Gson skips for omitted fields. */
    private <T> void assertTemporalAdapter(T value, String text, Class<T> type, JsonSerializer<T> serializer, JsonDeserializer<T> deserializer) {
        assertEquals("\"" + text + "\"", JSONItem.SimpleGSON.toJson(value, type));
        assertEquals(value, JSONItem.SimpleGSON.fromJson("\"" + text + "\"", type));
        assertEquals(new JsonPrimitive(text), serializer.serialize(value, type, null));
        assertEquals(value, deserializer.deserialize(new JsonPrimitive(text), type, null));
        assertEquals(JsonNull.INSTANCE, serializer.serialize(null, type, null));
        assertNull(deserializer.deserialize(null, type, null));
        assertNull(deserializer.deserialize(JsonNull.INSTANCE, type, null));
        assertThrows(java.time.format.DateTimeParseException.class, () -> deserializer.deserialize(new JsonPrimitive("not-a-date"), type, null));
    }

    @Test
    void localDateTimeAdapterAcceptsBothIsoAndDbFormats() {
        LocalDateTime expected = LocalDateTime.of(2024, 1, 15, 10, 30, 0);
        Dummy isoRead = JSONItem.SimpleGSON.fromJson("{\"dateTime\":\"2024-01-15T10:30:00\"}", Dummy.class);
        Dummy dbRead = JSONItem.SimpleGSON.fromJson("{\"dateTime\":\"2024-01-15 10:30:00\"}", Dummy.class);
        assertEquals(expected, isoRead.dateTime);
        assertEquals(expected, dbRead.dateTime);
    }

    @Test
    void readOrCreateBacksUpAnUnparsableFileInsteadOfDestroyingIt() throws Exception {
        Path file = tempDir.resolve("corrupt.json");
        java.nio.file.Files.writeString(file, "{ not valid json");

        Dummy fallback = new Dummy("fallback");
        Dummy result = JSONItem.ReadOrCreate(file.toString(), Dummy.class, fallback);

        assertSame(fallback, result);
        assertEquals("{ not valid json", java.nio.file.Files.readString(file.resolveSibling("corrupt.json.corrupt")));
        assertEquals("fallback", JSONItem.ReadJSON(file.toString(), Dummy.class).name);
    }

}

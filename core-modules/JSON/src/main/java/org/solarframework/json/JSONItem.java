package org.solarframework.json;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.gson.*;
import jakarta.persistence.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solarframework.core.util.SolarHome;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.function.Function;

public abstract class JSONItem<T extends JSONItem<T>> implements Serializable {
    private static final Logger log = LoggerFactory.getLogger(JSONItem.class);
    public static final Gson SimpleGSON = new GsonBuilder()
            .registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
            .registerTypeAdapter(LocalTime.class, new LocalTimeAdapter())
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .registerTypeAdapter(OffsetDateTime.class, new OffsetDateTimeAdapter())
            .registerTypeAdapter(ZonedDateTime.class, new ZonedDateTimeAdapter())
            .registerTypeAdapter(Instant.class, new InstantAdapter())
            .setExclusionStrategies(new ExclusionStrategy() {
                @Override public boolean shouldSkipField(FieldAttributes f) {
                    return f.getAnnotation(Transient.class) != null || f.getAnnotation(JsonIgnore.class) != null;
                }
                @Override public boolean shouldSkipClass(Class<?> c) {
                    return c.getName().contains("HibernateProxy");
                }
            })
            .setPrettyPrinting()
            .create();

    /** Shares text conversion while public adapters retain their concrete method signatures. */
    private static class TemporalAdapter<T> implements JsonSerializer<T>, JsonDeserializer<T> {
        private final Function<T, String> formatter;
        private final Function<String, T> parser;

        TemporalAdapter(Function<T, String> formatter, Function<String, T> parser) {
            this.formatter = formatter;
            this.parser = parser;
        }
        @Override public JsonElement serialize(T value, Type type, JsonSerializationContext context) {
            return value == null ? JsonNull.INSTANCE : new JsonPrimitive(formatter.apply(value));
        }
        @Override public T deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
            return json == null || json.isJsonNull() ? null : parser.apply(json.getAsString());
        }
    }


    public static class LocalDateAdapter extends TemporalAdapter<LocalDate> {
        public LocalDateAdapter() { super(value -> value.format(DateTimeFormatter.ISO_LOCAL_DATE), text -> LocalDate.parse(text, DateTimeFormatter.ISO_LOCAL_DATE)); }
        @Override public JsonElement serialize(LocalDate value, Type type, JsonSerializationContext context) { return super.serialize(value, type, context); }
        @Override public LocalDate deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException { return super.deserialize(json, type, context); }
    }

    public static class LocalTimeAdapter extends TemporalAdapter<LocalTime> {
        public LocalTimeAdapter() { super(value -> value.format(DateTimeFormatter.ISO_LOCAL_TIME), text -> LocalTime.parse(text, DateTimeFormatter.ISO_LOCAL_TIME)); }
        @Override public JsonElement serialize(LocalTime value, Type type, JsonSerializationContext context) { return super.serialize(value, type, context); }
        @Override public LocalTime deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException { return super.deserialize(json, type, context); }
    }

    public static class LocalDateTimeAdapter implements JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime> {
        private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

        @Override
        public JsonElement serialize(LocalDateTime src, Type typeOfSrc, JsonSerializationContext context) {
            return src == null ? JsonNull.INSTANCE : new JsonPrimitive(src.format(FORMATTER));
        }

        @Override
        public LocalDateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {

            String value = json.getAsString();

            // allow DB format too
            if (value.contains(" ")) {
                return LocalDateTime.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            }

            return LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }
    }

    public static class OffsetDateTimeAdapter extends TemporalAdapter<OffsetDateTime> {
        public OffsetDateTimeAdapter() { super(value -> value.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME), text -> OffsetDateTime.parse(text, DateTimeFormatter.ISO_OFFSET_DATE_TIME)); }
        @Override public JsonElement serialize(OffsetDateTime value, Type type, JsonSerializationContext context) { return super.serialize(value, type, context); }
        @Override public OffsetDateTime deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException { return super.deserialize(json, type, context); }
    }

    public static class ZonedDateTimeAdapter extends TemporalAdapter<ZonedDateTime> {
        public ZonedDateTimeAdapter() { super(value -> value.format(DateTimeFormatter.ISO_ZONED_DATE_TIME), text -> ZonedDateTime.parse(text, DateTimeFormatter.ISO_ZONED_DATE_TIME)); }
        @Override public JsonElement serialize(ZonedDateTime value, Type type, JsonSerializationContext context) { return super.serialize(value, type, context); }
        @Override public ZonedDateTime deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException { return super.deserialize(json, type, context); }
    }

    public static class InstantAdapter extends TemporalAdapter<Instant> {
        public InstantAdapter() { super(Instant::toString, Instant::parse); }
        @Override public JsonElement serialize(Instant value, Type type, JsonSerializationContext context) { return super.serialize(value, type, context); }
        @Override public Instant deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException { return super.deserialize(json, type, context); }
    }

    /** Where this is kept, written relative to the installation's data folder — see {@link SolarHome}, which resolves it. */
    public String file() {
        throw new UnsupportedOperationException(getClass().getSimpleName() + " does not override file()");
    }

    public void beforeWrite() {}
    public void afterWrite() {}

    public T Write() {
        return Write(file());
    }
    public T Write(String path) {
        Path target = SolarHome.pathTo(path);
        try {
            beforeWrite();
            Path parent = target.getParent();
            if (parent != null) Files.createDirectories(parent);
        } catch (Exception _) {}
        // Written to a sibling temp file and moved into place so a kill mid-write can never leave a truncated, unparsable file at `target` - which is what used to make ReadOrCreate mistake real data for corruption and erase it.
        Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
        try (FileWriter writer = new FileWriter(tmp.toFile(), StandardCharsets.UTF_8)) {
            SimpleGSON.toJson(this, writer);
        } catch (Exception e) {
            return null;
        }
        try {
            try { Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (IOException _e) { Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING); }
            afterWrite();
            return (T) this;
        } catch (Exception e) {
            return null;
        }
    }

    public static <A> A ReadJSON(String path, Class<A> clazz) {
        try (FileReader reader = new FileReader(SolarHome.pathTo(path).toFile(), StandardCharsets.UTF_8)) {
            return SimpleGSON.fromJson(reader, clazz);
        } catch (IOException e) {
            return null;
        }
    }
    public static <A extends JSONItem<A>> A ReadJSON(Class<A> clazz) {
        try {
            return ReadJSON(clazz.getDeclaredConstructor().newInstance().file(), clazz);
        } catch (Exception e) {
            return null;
        }
    }

    public static <A extends JSONItem<A>> A ReadOrCreate(String filePath, Class<A> clazz, A fallback) {
        try {
            A read = ReadJSON(filePath, clazz);
            if (read != null) {
                log.info("Read {} from {}", clazz.getSimpleName(), SolarHome.pathTo(filePath));
                return read;
            }
        } catch (Exception e) {
            // ReadJSON only swallows a missing file; anything reaching here is an existing file that failed to parse, so it is renamed aside rather than destroyed - silently replacing it with `fallback` is what used to erase real data (e.g. configured database sources) with no trace of why.
            Path broken = SolarHome.pathTo(filePath);
            log.error("Could not parse {} ({}) - keeping it as {}.corrupt and starting over with defaults.", broken, e.toString(), broken);
            try { Files.move(broken, broken.resolveSibling(broken.getFileName() + ".corrupt"), StandardCopyOption.REPLACE_EXISTING); } catch (IOException _e) {}
        }
        fallback.Write(filePath);
        log.info("Created {} at {} from defaults", clazz.getSimpleName(), SolarHome.pathTo(filePath));
        return fallback;
    }
    public static <A extends JSONItem<A>> A ReadOrCreate(Class<A> clazz, A fallback) {
        return ReadOrCreate(fallback.file(), clazz, fallback);
    }

}

# JSON

`org.solarframework.mu:json` — **`JSONItem<T>`**, the base of every config file the framework writes (`DataSourceFile`, `AIConfigFile`, `MailConfigFile`, `BackupSettingsFile`, `ProxyDomainsFile`...).

A subclass names its file with `file()` (a path under `SolarHome`) and gets `Write()` / `Write(path)`, `ReadJSON(...)` and `ReadOrCreate(clazz, fallback)`; `beforeWrite()` / `afterWrite()` are its hooks. The Gson it uses carries adapters for `LocalDate`, `LocalTime`, `LocalDateTime`, `OffsetDateTime`, `ZonedDateTime` and `Instant`, written in ISO form.

```java
public class Settings extends JSONItem<Settings> {
    String theme = "dark";
    @Override public String file() { return "config/app/settings.json"; }
}
Settings s = JSONItem.ReadOrCreate(Settings.class, new Settings());
s.Write();
```

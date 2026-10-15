# Plugin

`org.solarframework.mu:plugin` — what a Spring host needs to load PF4J plugins that carry their own pages.

- **`PluginConfig`** — the `SpringPluginManager` bean.
- **`PluginTemplateResolver`** — a Thymeleaf resolver that finds a template inside whichever plugin holds it, and the `SpringTemplateEngine` that puts it ahead of the host's own `classpath:/templates/` resolver.

This is why the database, AI, mail and backup stacks never carry a Spring stereotype: inside a plugin there is no application context of their own.

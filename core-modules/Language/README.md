# Language

`org.solarframework.mu:language` — localisation helpers.

- **`L10N`** — the base of a module's translations: a subclass sets the `ResourceBundle RB` and calls `TL(key, values...)`. Keys are lower-cased with spaces turned into hyphens, `<br>` becomes a newline, and `<v1>`…`<v6>` are replaced by the values (with `$`, `{`, `}` and `^` stripped so a value can never act as a pattern). Discord's own `L10N` extends it.
- **`Nationalities`** — every country with its native name, ISO-2/ISO-3/numeric codes, flag emoji and Discord flag; `Nationalities.get(query)` finds one by any of them.

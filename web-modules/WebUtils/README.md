# WebUtils

`org.solarframework.mu:web-utils` — small helpers for a Spring MVC + Thymeleaf host (brings `thymeleaf-layout-dialect`).

- **`WebUtils.canAccessPage(model, principal, path)`** — asks SolarERP's account classes, **by reflection**, whether the logged user may open `path`, and adds the page essentials to the model when they may. It returns `false` in a host without SolarERP's `org.solarframework.auth` classes, rather than failing to compile there.
- **`RequestLoggerInterceptor`** — prints `[METHOD] uri?query from address` for every request.

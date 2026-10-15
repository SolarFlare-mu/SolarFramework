# SpringProxyServerImplV2

`org.solarframework.mu:spring-proxy-server-impl-v2` — the [`ProxyServerAPI`](../ProxyServerAPI/README.md) proxy on Spring Cloud Gateway (WebFlux). `DomainRegistry` turns each domain into a gateway route and publishes `RefreshRoutesEvent` when they change; `StaticSiteFilter` serves a domain's files with zero-copy writes. Built through `new ProxyBuilder().registerDomains(...).regenerateCerts().build()`, with no rate limits.

The host must be a reactive (WebFlux) application: Spring Cloud Gateway refuses to start on the servlet stack.

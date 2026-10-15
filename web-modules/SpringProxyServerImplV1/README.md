# SpringProxyServerImplV1

`org.solarframework.mu:spring-proxy-server-impl-v1` — the [`ProxyServerAPI`](../ProxyServerAPI/README.md) proxy inside a Spring MVC host: `ProxyController` is a `@RestController` that serves a domain's files (`index.html` for `/`, `..` refused), or for an `http` path fetches the same URI from that server as a resource — a plain fetch, not a full forward of method, headers and body. `ProxyConfig` sets up HTTPS from `config/certs/ssldomains.p12`. Built the same way as the others (`new ProxyBuilder().registerDomains(...).regenerateCerts().build()`), with no rate limits.

The servlet stack blocks a thread per request; [`SpringProxyServerImplV2`](../SpringProxyServerImplV2/README.md) and [`NettyProxyServerImpl`](../NettyProxyServerImpl/README.md) do not.

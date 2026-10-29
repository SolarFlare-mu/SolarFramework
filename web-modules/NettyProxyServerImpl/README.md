# NettyProxyServerImpl

`org.solarframework.mu:netty-proxy-server-impl` — the [`ProxyServerAPI`](../ProxyServerAPI/README.md) proxy on Reactor Netty, with no Spring web stack. The only implementation that applies `RateLimitRule`s (through core's `RateLimiter`).

```java
Domain site = new Domain(ProxyBuilder.LOCALHOST, "mysite.com");          // files from config/domain/mysite.com/_
site.addSubdomain("www");
Domain app = new Domain(ProxyBuilder.LOCALHOST, "myapp.com", "http://localhost:8080"); // forwarded
new ProxyBuilder().registerDomains(site, app).regenerateCerts().build();
```

`ProxyService` writes each domain into the machine's hosts file under its own comment tag (core's `HostsFile`), so it never touches the other entries. `regenerateCerts()` issues a locally trusted certificate for every registered name through [`CertificateGenerator`](../../misc-modules/CertificateGenerator/README.md)'s mkcert. `ProxyServer` is the Netty server itself: a domain's files through `BaseDomain.fileFor`, a forward with the caller stamped by `CallerHeaders` (scheme `https`, the only one it listens on) — see the API README for both.

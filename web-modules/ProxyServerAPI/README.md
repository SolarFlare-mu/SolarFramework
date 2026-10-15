# ProxyServerAPI

`org.solarframework.mu:proxy-server-api` — what a local HTTPS reverse proxy serves, shared by its three implementations: [`NettyProxyServerImpl`](../NettyProxyServerImpl/README.md), [`SpringProxyServerImplV1`](../SpringProxyServerImplV1/README.md) and [`SpringProxyServerImplV2`](../SpringProxyServerImplV2/README.md). Each takes the same objects through its own `ProxyBuilder`.

- **`Domain`** / **`Subdomain`** (`BaseDomain`) — a host name, the IP it resolves to, and what it serves: a folder of files, or another server's URL to forward to. `new Domain(ip, name)` or `new Domain(ip, name, path)`; `addSubdomain(name)` / `addSubdomain(name, path)`.
- **`ProxyDomainsFile`** — every domain, stored in `config/proxy/domains.json`. It replaced the old `web_domain`/`web_subdomain` tables: the admin screen already saved them as a full reload, so a file loses nothing a query gave.
- **`RateLimitRule`** / **`ProxyRateLimitsFile`** — request limits, kept apart from domains because they control traffic rather than routing. A `""` host limits every domain, a named host only that one.

With no path given, a domain's files live under `config/domain/`: the root domain (`mysite.com`) in `mysite.com/_`, a subdomain (`www.mysite.com`) in `mysite.com/_www`. A path starting with `http` is a server to forward to instead.

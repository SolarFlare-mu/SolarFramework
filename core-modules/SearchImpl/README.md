# SearchImpl

`org.solarframework.mu:search-impl` — [`SearchAPI`](../SearchAPI/README.md) over Elasticsearch's REST API. Only Gson and SLF4J on the classpath: no Elasticsearch client library.

`new ElasticSearchService(node)` takes the node's base URL (a trailing `/` is dropped). `ElasticQuery` builds the request body from a `SearchQuery`; `ElasticResponse` reads the answer back into `SearchResults`.

Tests: 4 classes need no node. `ElasticSearchServiceLiveTest` needs a running ES node and runs only with `-Plive`.

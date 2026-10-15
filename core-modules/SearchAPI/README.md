# SearchAPI

`org.solarframework.mu:search-api` — full-text search, with no engine attached. The Elasticsearch implementation is [`SearchImpl`](../SearchImpl/README.md).

- **`ISearchService`** — `isAvailable`, `createIndex(index, fieldTypes)`, `deleteIndex`, `index(doc)`, `indexAll(docs)`, `delete(index, id)`, `search(query)`.
- **`dto/`** — `SearchDocument`, `SearchQuery`, `SearchResults`, `SearchHit`.
- **`SearchRegistry.SolarSearch`** — starts as **`NoSearchService`**, which answers every call harmlessly, so code that searches works unchanged on an installation with no search engine. A host replaces it with a real service.

```java
SearchRegistry.SolarSearch = new ElasticSearchService("http://localhost:9200");
```

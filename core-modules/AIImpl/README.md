# AIImpl

`org.solarframework.mu:ai-impl` — [`AIAPI`](../AIAPI/README.md) over Spring AI's OpenAI client, which covers every OpenAI-compatible endpoint: LM Studio, OpenAI, Gemini.

## Two ways in
**No class carries a Spring stereotype**: the stack ships inside a SolarERP plugin, where there is no application context. `AIManager` and `AIService` are plain objects; `AIConfig` is the one `@Configuration` for a host that wants beans. `AI_PlainMain` and `AI_Main` (under `src/test`) are the same script on each path.

- **`new AIManager()`** starts **empty** and registers itself as `AIRegistry.SolarAIManager`. An installation that configured no endpoint holds no service and no default — the ordinary case.
- **`AIConfig`** seeds a `Default` service **only when `spring.ai.openai.base-url` is set** (with `spring.ai.openai.api-key`, `spring.ai.openai.chat.options.model`, `solar.ai.timeout-seconds`, default 300). It does not load the file: whoever starts the stack calls `LoadFromFile()` on `config/ai/agents.json` (`AIConfigFile`).
- **`makeNewService(name, …)`** — the shorter forms copy the endpoint and key from the default service and throw when there is none.

**Adding a service setting** means: the field + getter + setter on `AIService`, the getter + setter on `IAIService`, the field on `ServiceDefinition`, and `AIManager.LoadFromFile` / `SaveAsFile`; plus `AIConfig` if a property should seed it.

## Things that will bite
- **Spring's default HTTP timeout is far too short for a local model.** `gpt-oss-20b` takes ~4 s for a trivial prompt and much longer for a cold load or a tool loop. `AIService` therefore builds its own `OpenAiApi` over a `JdkClientHttpRequestFactory` with its own read timeout, instead of using the autoconfigured `ChatModel` — the same reason `DatabaseService` owns its pool.
- **LM Studio answers HTTP 200 on unknown endpoints**, putting the failure in an `{"error": ...}` body, so `LMStudioModelManager` parses the body and never trusts the status. It has **no REST load/unload** — those shell out to the `lms` CLI; without it models still load just-in-time.
- **Gemini's OpenAI endpoint is stricter**: it refuses `frequency_penalty`/`presence_penalty`, and Gemini 3 answers 400 to a historic tool call with no thought signature. `GeminiRequests` rewrites the request.
- **Structured output goes through `response_format`, not the prompt** (`StructuredOutput`): a schema written into the prompt gets echoed back by gpt-oss.

## Logging
Every model call logs at INFO on `AIService`: the outgoing prompt, the reply, latency and token usage, each line prefixed `[<service>][<id>]`. The id is the conversation when the call comes from one (`Conversation` puts it in the SLF4J MDC under `Conversation.LOG_TAG`), otherwise a per-call `C00001`. `AIManager` logs lifecycle (service/chatbot added or removed, file loaded), `Chatbot` one line per conversation start, `Conversation` one line per agent step. Message text goes through `ChatMessage.brief` (whitespace flattened, cut at 200 chars). Nothing needs `logging.level` raised.

## Inside
`ToolAdapter` turns `@AITool` methods into Spring AI tool callbacks and hands an object with none to Spring AI untouched, so a native `@Tool` still works. `MessageMapper` is the only place the two message type systems meet.

Tests: unit tests need no model. `AIServiceLiveTest`, `ConversationLiveTest`, `ToolUseLiveTest` need LM Studio on `localhost:1234` (~6.5 min); `GeminiLiveTest` needs `GEMINI_API_KEY`. All four run only with `-Plive`.

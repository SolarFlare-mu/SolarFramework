# AIAPI

`org.solarframework.mu:ai-api` — the AI interfaces, the conversation loop and its entities. No Spring AI here: the backend is [`AIImpl`](../AIImpl/README.md); add both. The full tutorial is [docs/AI.md](../../docs/AI.md).

## Service, chatbot, conversation
Three concepts, mirroring the database module:

- **`IAIService`** is one endpoint, one model, and every call over it — the AI counterpart of a data source. It owns its base URL, key, model, timeout, rate limit, *and everything about its model* (`getModelState`, `isModelSupportingTools`, `getUsableContext`, `loadModel`…).
- **`Chatbot`** is an agent using a service: persona, tools, options, memory policy, step bound. One chatbot serves every user; each `startConversation()` hands back its own `Conversation`.
- **`IAIManager`** holds every service and every named chatbot, and reads/writes them as a JSON file (`LoadFromFile` / `SaveAsFile`), exactly like `IDatabaseManager`.
- **`Conversation`** and **`ChatMessage`** (`ai.obj`) are `@Entity` classes. They are ordinary objects until `conversation.save()`, which upserts the conversation and `UpsertAll`s its messages. They can live in the API because they reach the model only through `IAIService` and the database only through `DatabaseRegistry`.

The conversation loop (step bound, tool approval, memory, transcript) is provider-neutral and lives on `Conversation`; the only thing a backend supplies is one turn, `IAIService.runTurn` (plus `streamTurn` and `structuredTurn`), returning a `TurnResult`. That is why chatbot, assistant and agent need no separate types: `send` is a turn that answers, `run` is the same loop reported step by step (`AgentRun`). It also means the loop is unit-tested with no model at all.

**A service does not talk.** It holds no persona, tools or history, so it has no `prompt`. Every call to a model is a conversation, and every conversation belongs to a `Chatbot` — `service.createChatbot().build().prompt("Hi")` for one turn, `bot.startConversation(...)` for several. The one-shot helpers (`prompt`, `promptAs`, `chooseBetween`, `complete`, `completeCode`, `askForInformation`, `stream`) live on `Chatbot`, each building a throwaway conversation through `Chatbot.oneShot(instruction, persona, options)` — same loop, approval, transcript, log lines and `save()` as any other. Task presets (`Prompts`) win over the bot's persona where the task is mechanical (completion, extraction, choosing); the bot's persona wins where it is conversational (`prompt`, `stream`, `askForInformation`).

```java
Chatbot bot = DefaultAIService.createChatbot().name("timekeeper")
        .systemPrompt("You answer time-related questions.")
        .tools(new Toolbox()).maxSteps(5).build();
Conversation c = bot.startConversation("conversation-1", "Loïc");
c.send("What is the current time?");
c.save();                            // only now does it touch the database

class Toolbox {
    @AITool(description = "Returns the current date and time.")
    public String now() { return LocalDateTime.now().toString(); }
}
```

## Also here
- **`@AITool`** — tools are declared with this, not Spring AI's `@Tool`, so a consumer's toolbox needs no Spring AI on its classpath.
- **`Memory`** — keeps the newest messages that fit the budget; given a service it summarises what falls out instead of forgetting it.
- **`AIOptions`** — immutable generation settings; every `with` returns a copy. There is **no input bound**: no backend accepts one, `maxOutputTokens` caps the reply only.
- **`AIRegistry`** — `DefaultAIService` and `SolarAIManager`, filled by AIImpl's `AIManager`, null in an application that never adds it.
- `dto/` (`ServiceDefinition`, `ChatbotDefinition`, `ModelFacts`, `TokenUsage`, `ToolCall`), `enums/`, `AIException`.

**Persistence is opt-in**: a chatbot that never calls `save()` needs no data source, even though the classes are entities.

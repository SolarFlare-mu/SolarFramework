# MailerAPI

`org.solarframework.mu:mailer-api` — the mail interfaces, with no mail library attached. The implementation is [`MailerImpl`](../MailerImpl/README.md); add both.

- **`IMailService`** — one mailbox: one SMTP endpoint, one sender address, and every letter posted through it — the mail counterpart of a data source. `send(to, subject, body)`, `isAvailable()`, host/port/credentials/`auth`/`startTls`/`from` getters and setters.
- **`IMailManager`** — every mailbox the installation posts from, read and written as one JSON file (`LoadFromFile` / `SaveAsFile`), as `IDatabaseManager` holds its data sources.
- **`MailboxDefinition`** — one mailbox flattened for that file.
- **`MailRegistry`** — `DefaultMailService` and `SolarMailManager`.

```java
import static org.solarframework.mail.spring.MailRegistry.DefaultMailService;
DefaultMailService.send("someone@example.com", "Subject", "Body");
```

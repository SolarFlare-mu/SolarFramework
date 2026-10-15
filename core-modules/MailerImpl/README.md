# MailerImpl

`org.solarframework.mu:mailer-impl` — [`MailerAPI`](../MailerAPI/README.md) over Spring's `JavaMailSenderImpl` and Jakarta Mail.

Same shape as the database and AI stacks: **no stereotype**, so it also runs inside a plugin. `new MailManager()` is the whole setup with no context; `MailConfig` is the same object as a bean. `MailService` owns its own sender, as `DatabaseService` owns its Hikari pool, and reads no properties of its own.

**The file has the last word.** `MailConfig` seeds the default mailbox from `spring.mail.*` only on a first run — when `config/mail/mailboxes.json` (`MailConfigFile`) does not exist yet. After that the file wins, which is how a mailbox added on a screen survives a restart and a removed one stays removed.

Tests: `MailConfigFileTest` (the file round trip), no SMTP server needed.

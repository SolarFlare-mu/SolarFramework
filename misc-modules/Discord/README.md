# Discord

`org.solarframework.mu:discord` — a Discord bot framework over JDA: every interaction is a class with an annotation, found by a classpath scan of one package.

```java
BotBuilder bot = new BotBuilder("TOKEN", "my.bot.interaction");  // the package holding the commands
bot.setBotGuild(930718276542136400L);           // the bot's own guild
bot.setLogChannel(1121096901681483807L);        // where its logs go, in that guild
bot.setTemporaryFilesChannel(1102661048269541406L);
bot.setAfterReadyAction(() -> { /* once ready */ return null; });
bot.build();
```

A command extends the base for its kind and carries the matching annotation: `SlashCMD` + `@SlashCommand`, `ButtonCMD` + `@ButtonCommand`, `ModalCMD` + `@ModalCommand`, `StringSelectCMD` / `EntitySelectCMD` + `@SelectCommand` / `@EntityCommand`, `MessageCMD`, `UserCMD`, and the guild-only `GSlashCMD`, `GMessageCMD`, `GUserCMD`. A component carries its metadata through `makeButton(...)`, so a button knows what it was made for without any state on the bot.

```java
@SlashCommand(name = "hello", description = "Says hello.")
public class SlashHello extends SlashCMD {
    @Override public void onSlash(SlashCommandInteractionEvent e) {
        e.reply("Hello, " + e.getUser().getAsMention() + "!").setComponents(ActionRow.of(makeButton(ClickMe.class, "some data"))).queue();
    }
}

@ButtonCommand(id = "click_me", label = "Click me!")
public class ClickMe extends ButtonCMD {
    @Override public void onPressed(ButtonInteractionEvent e, String[] metadata) { e.reply("Data: " + metadata[0]).queue(); }
}
```

## Also here
- **`Pager`** — arrow-button pagination. It replaced a select menu of pages that could never show more than 25; only the neighbouring pages are encoded, so a list pages on indefinitely.
- **`utils/`** — `MessageUtils`, `MemberUtils`, `UserUtils`, `DMUtils`, `ModalUtils`, `FileUtils`, `WebhookUtils` (+ `WebhookMessage`, a message plus the webhook identity it must be posted under), `MiscUtils`.
- **`obj/`** — the bot's entities on [`DatabaseAPI`](../../core-modules/DatabaseAPI/README.md): `Discord_GuildInfo`, `Discord_Profile`, `Discord_ChannelInfo`, `Discord_RoleInfo`, `Discord_MessageInfo`, `Discord_BotEmoji`, and the guild/profile variables.
- **`lang/L10N`** — the bot's translations (`lang/discord/system` bundles), on [`Language`](../../core-modules/Language/README.md)'s `L10N`.

Tests: 17 classes with Mockito, no Discord connection.

# SolarFramework

A framework built on Spring Boot 4 and Java 25 that gives an application ready-made stacks — database, AI, mail, backup, search, tournaments, a local HTTPS proxy, a Discord bot, outside-program runners — and the utilities around them.

**Every stack also runs with no Spring application context**, because SolarERP carries them inside PF4J plugins: no implementation class carries a Spring stereotype, each `*Impl` module has one `@Configuration` for a host that wants beans, and the registry statics (`DatabaseRegistry`, `AIRegistry`, `MailRegistry`, `BackupRegistry`, `SearchRegistry`, `TournamentRegistry`) are filled the same way on both paths.

## Modules
Each module documents itself in its own README. `*API` modules hold the interfaces, `*Impl` the implementation — add both of a pair.

| Area | Modules |
|---|---|
| Utilities | [core](core/README.md) · [JSON](core-modules/JSON/README.md) · [Language](core-modules/Language/README.md) · [Excel](core-modules/Excel/README.md) · [Plugin](core-modules/Plugin/README.md) |
| Database | [DatabaseAPI](core-modules/DatabaseAPI/README.md) · [DatabaseImpl](core-modules/DatabaseImpl/README.md) |
| AI | [AIAPI](core-modules/AIAPI/README.md) · [AIImpl](core-modules/AIImpl/README.md) — tutorial in [docs/AI.md](docs/AI.md) |
| Mail | [MailerAPI](core-modules/MailerAPI/README.md) · [MailerImpl](core-modules/MailerImpl/README.md) |
| Backup | [BackupAPI](core-modules/BackupAPI/README.md) · [BackupImpl](core-modules/BackupImpl/README.md) |
| Search | [SearchAPI](core-modules/SearchAPI/README.md) · [SearchImpl](core-modules/SearchImpl/README.md) |
| Tournaments | [TournamentAPI](misc-modules/TournamentAPI/README.md) · [TournamentImpl](misc-modules/TournamentImpl/README.md) |
| Web | [WebUtils](web-modules/WebUtils/README.md) · [DatabaseEditor](web-modules/DatabaseEditor/README.md) |
| HTTPS proxy | [ProxyServerAPI](web-modules/ProxyServerAPI/README.md) · [NettyProxyServerImpl](web-modules/NettyProxyServerImpl/README.md) · [SpringProxyServerImplV1](web-modules/SpringProxyServerImplV1/README.md) · [SpringProxyServerImplV2](web-modules/SpringProxyServerImplV2/README.md) · [CertificateGenerator](misc-modules/CertificateGenerator/README.md) |
| Discord | [Discord](misc-modules/Discord/README.md) |
| Runners | [RunnerAPI](runners/RunnerAPI/README.md) · [JavaRunner](runners/JavaRunner/README.md) · [PythonRunner](runners/PythonRunner/README.md) · [CSharpRunner](runners/CSharpRunner/README.md) · [NodeRunner](runners/NodeRunner/README.md) |

A C# port lives in its own repository, `CSolarFramework`.

## Requirements
- Java 25 or higher
- A Spring Boot project, for the Spring path
- A database the stack has a driver for (MariaDB, H2, SQLite)
- An OpenAI-compatible endpoint for AI (LM Studio, OpenAI, Gemini)

## Installation
SolarFramework is published to GitHub Packages. It is public, but GitHub still asks for a classic token with
`read:packages` and access to the repository, in `~/.m2/settings.xml`:

```xml
<server>
  <id>github</id>
  <username>${env.GITHUB_ACTOR}</username>
  <password>${env.GITHUB_TOKEN}</password>
</server>
```

Then add the repository and the parent to your Spring project:

```xml
<parent>
  <groupId>org.solarframework.mu</groupId>
  <artifactId>solar-framework</artifactId>
  <version>1.0</version>
  <relativePath/>
</parent>

<repositories>
  <repository>
    <id>github</id>
    <url>https://maven.pkg.github.com/solarflare-mu/solarframework</url>
  </repository>
</repositories>
```

ArtifactIds are lowercase kebab case (GitHub Packages rejects capitals) while the folders keep their CamelCase names; each module's README starts with its own coordinates:
```xml
<artifactId>core</artifactId>
<artifactId>database-api</artifactId>      <artifactId>database-impl</artifactId>
<artifactId>ai-api</artifactId>            <artifactId>ai-impl</artifactId>
<artifactId>search-api</artifactId>        <artifactId>search-impl</artifactId>
<artifactId>mailer-api</artifactId>        <artifactId>mailer-impl</artifactId>
<artifactId>backup-api</artifactId>        <artifactId>backup-impl</artifactId>
<artifactId>tournament-api</artifactId>    <artifactId>tournament-impl</artifactId>
<artifactId>proxy-server-api</artifactId>  <artifactId>netty-proxy-server-impl</artifactId>
<artifactId>spring-proxy-server-impl-v1</artifactId>  <artifactId>spring-proxy-server-impl-v2</artifactId>
<artifactId>excel</artifactId>             <artifactId>json</artifactId>
<artifactId>plugin</artifactId>            <artifactId>language</artifactId>
<artifactId>web-utils</artifactId>         <artifactId>database-editor</artifactId>
<artifactId>discord</artifactId>           <artifactId>certificate-generator</artifactId>
<artifactId>runner-api</artifactId>        <artifactId>java-runner</artifactId>         <artifactId>python-runner</artifactId>
<artifactId>csharp-runner</artifactId>     <artifactId>node-runner</artifactId>
```

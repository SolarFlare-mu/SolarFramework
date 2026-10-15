# TournamentImpl

`org.solarframework.mu:tournament-impl` — the format engines and the bracket renderer behind [`TournamentAPI`](../TournamentAPI/README.md).

## Two ways in
**Nothing here is a Spring bean**: `new BracketRenderer()` (which registers itself as `TournamentRegistry.SolarBrackets`) and the `PhaseEngines` ServiceLoader are the whole setup, which is how a plugin carrying this module uses it. `TournamentConfig` is the `@Configuration` for a host that wants beans. `Tournament_PlainMain` and `Tournament_Main` (under `src/test`) run a 2v2 double elimination from registration to champion on each path.

```java
new BracketRenderer();
Tournament t = Tournament.create("Weekend Cup", PhaseType.DOUBLE_ELIMINATION);
t.setTeamSize(2);
t.openRegistration();
t.registerTeam("Red Dragons", List.of("Alice", "Bob"));
// ...
t.closeRegistration();
t.start();
for (Match m : t.getPlayableMatches()) m.reportResult(2, 1);
TournamentRegistry.SolarBrackets.writeHtml(t.getPhases().getFirst(), BracketTheme.dark(), new File("bracket.html"));
```

## Engines (`engine/`, declared in `META-INF/services`)
- **`SingleEliminationEngine`** — the field padded to the next power of two, empty slots become byes, seeds 1 and 2 can only meet in the final.
- **`DoubleEliminationEngine`** — winners and losers brackets and a grand final; the losers bracket alternates minor rounds (survivors play each other) and major rounds (survivors meet the freshly dropped), which keeps the two in step.
- **`GroupEngine`** / **`RoundRobinEngine`** — fixtures by the circle method, a rotating bye for an odd group. Round robin is a group phase of one group, a named subclass only so `ServiceLoader` can build it.
- **`SwissEngine`** — a fixed number of rounds pairing equal scores without repeats; only the current round exists, the next is drawn when its last result lands.
- `AbstractPhaseEngine` — match creation, progression links, byes and numbering.

## Renderer (`render/`)
`BracketLayout` turns a phase into one `RenderModel` (round one evenly spaced, every later match at the midpoint of its feeders), and `PngPainter`, `SvgPainter` and `HtmlPainter` all walk that same model, **so the three outputs cannot drift apart**. The HTML page is self-contained and lights up one entrant's whole run on hover.

Tests: 12 classes, no database needed.

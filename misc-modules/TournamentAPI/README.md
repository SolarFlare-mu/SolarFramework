# TournamentAPI

`org.solarframework.mu:tournament-api` — tournaments as entities: the rules, the phases, the matches and the standings. The format engines and the bracket renderer are in [`TournamentImpl`](../TournamentImpl/README.md); add both.

## Entities (`obj/`)
- **`Tournament`** — the root: the ruleset every phase shares (points, tiebreakers, best-of, team size), the registration window, and the podium once play is over. Fields and mapping live here; the behaviour is abstract and implemented by the one concrete `Tournament` in TournamentImpl.
- **`Phase`** — one stage. A two-phase event is typically a `GROUP` phase feeding a `SINGLE_ELIMINATION` playoff, qualifiers carried over by `getAdvanceTotal()`. Every rule defaults to the tournament's and can be overridden per phase.
- **`Match`** / **`MatchGame`** — `score1`/`score2` are the *series* score (games won), so BO1 and BO5 are the same object; a longer series keeps its games in `MatchGame` and `recomputeFromGames()` folds them back. A team game's score can be built from member contributions.
- **`Participant`** / **`ParticipantMember`** — an entrant is a player in 1v1 and the team otherwise, its roster in `ParticipantMember`. **Matches always reference a `Participant`**, never a member, so the same match code serves every team size.
- **`Standing`** — an entrant's slot and final placement in one phase. **The record, points and tiebreakers are derived live from the matches, never stored.**

## The rest
- **Enums** — `PhaseType`, `SeedingMethod`, `Tiebreaker`, `MatchDecisionMode`, `BracketSide`, and the statuses; persisted as their `name()` through the `obj/convert/` converters (`TournamentEnums` parses null- and case-tolerantly).
- **`IPhaseEngine`** / **`PhaseEngines`** — format behaviour, looked up by phase type. Engines hold no state, and are found through `ServiceLoader` on first use; this lives in the API because the entities need it.
- **`IBracketRenderer`** + **`BracketTheme`** — draws a phase as PNG, SVG, or a self-contained HTML page. Theme colours are `#RRGGBB` strings, so the theme stays free of AWT and serialises straight to JSON.
- **`TournamentRegistry.SolarBrackets`**, **`TournamentException`** (a move the rules refuse).
- Helpers: `Brackets` (pure bracket arithmetic), `Seeder`, `StandingsCalculator`, `Ids` (epoch-millis ids that never repeat, since generation creates hundreds of rows in one millisecond).

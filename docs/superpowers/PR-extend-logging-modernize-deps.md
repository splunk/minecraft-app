# Extend Splunk logging coverage + modernize dependencies (Spigot/Paper plugin)

**Branch:** `feature/extend-logging-modernize-deps` (off `develop` @ `0903ca4`)
**Scope:** `shared-mc`, `spigot`, `logtosplunk-plugin`, parent `pom.xml`. `forge` module dropped from the reactor (unrelated mod, dead 11+ years).

## Summary

- Adds four new opt-in logging categories so server operators can get visibility into combat/survival, item economy, player progression, and session/server-lifecycle detail in Splunk — on top of the existing player/block/death events.
- Modernizes the build: Java 21 target, dependency versions centralized in the parent POM, `json-simple` replaced with `gson`, stale/vulnerable plugin versions bumped, OWASP `dependency-check-maven` wired in as a CVSS 7.0 gate.
- All new logging is **disabled by default** — nothing changes for existing deployments until an operator opts in via `splunk.craft.enable.*` properties.

## Why

Before this change, the plugin only logged player connect/disconnect/chat/move/death and block break/place. Server operators asked for more granular telemetry (combat, items, progression, server health) to build richer Splunk dashboards, but adding it without a flood-control mechanism would have overwhelmed Splunk HEC with high-frequency events (e.g. damage ticks, food level changes). Dependency modernization was needed before deploying this plugin to other/newer Minecraft servers — the old Java 8 target and stale deps (junit 4.8.2, json-simple, shade 2.4.1) carry known CVEs and won't run on current Paper builds without a Java bump.

## What changed

### Logging (shared-mc + spigot)

| Category | Toggle | Events | Throttled? |
|---|---|---|---|
| Combat/survival | `splunk.craft.enable.combat` | damage, kill, heal, hunger, respawn | damage/heal/hunger per-player (high frequency); respawn not |
| Item economy | `splunk.craft.enable.item` | pickup, drop | pickup throttled (e.g. XP orbs/arrows); drop not |
| Progression | `splunk.craft.enable.progression` | xp change, level change, enchant, craft, fish, command | not throttled (low frequency) |
| Server lifecycle | `splunk.craft.enable.server` | server start, weather change | n/a |
| Performance | `splunk.craft.enable.performance` (interval via `splunk.craft.performance.interval_ticks`, default 600) | online players, loaded chunks | polled, not event-driven |
| Session detail | `splunk.craft.enable.session_detail` | teleport, gamemode change, bed enter, world change | not throttled |
| Session IP | `splunk.craft.enable.session_ip` | client IP on connect | n/a — explicitly not treated as PII in this project |

New shared-mc data carriers: `LoggableCombatEvent`, `LoggableItemEvent`, `LoggableProgressionEvent`, `LoggableServerEvent`, `LoggablePerformanceEvent`. New spigot listeners: `CombatEventLogger`, `ItemEventLogger`, `ProgressionEventLogger`, `ServerEventLogger`, plus `ScheduledMetricLogger`/`PerformanceSampler` for poll-based metrics. New `util/EventThrottle` (guava `Cache`-backed, per-key time window, injectable clock for tests) prevents flooding Splunk HEC.

Two correctness issues caught by final whole-branch review and fixed before merge:
1. **Duplicate kill logging** — `CombatEventLogger` originally also logged `EntityDeathEvent`, double-counting kills already logged by the pre-existing `DeathEventLogger`. Removed.
2. **Dead toggle** — `ENABLE_SESSION_DETAIL` was defined but never read; the four session-detail handlers ran unconditionally. Added the guard.

### Dependencies / build

- `maven.compiler.release` bumped 8 → 21 (parent `pom.xml`), `maven-compiler-plugin` → 3.14.0.
- `json-simple` replaced with `gson` for the Splunk HEC JSON envelope (`SingleSplunkConnection.buildHecEnvelope`).
- Dependency versions centralized in parent `dependencyManagement` (gson, guava, httpcore5, httpclient5); `junit` 4.8.2 → 4.13.2.
- `maven-shade-plugin` 2.4.1 → 3.6.0.
- `forge` module removed from the reactor (dead/unmaintained, unrelated mod — directory kept, not deleted).
- Added OWASP `dependency-check-maven` (CVSS 7.0 fail threshold) + `owasp-suppressions.xml` template. **Not yet executed** — anonymous NVD API rate-limits the scan in this environment; needs `NVD_API_KEY` in CI to run for real.

## Known limitations / deferred decisions

- **TPS/MSPT and player protocol version are dropped, not implemented.** `Bukkit.getTPS()`, `Bukkit.getAverageTickTime()`, and `Player.getProtocolVersion()` are Paper-only APIs absent from vanilla `spigot-api` (confirmed via `javap` against the cached jar). The fields/setters exist in `LoggablePerformanceEvent`/`LoggablePlayerEvent` but are currently unused. **To restore: switch the spigot module's dependency from `spigot-api` to `paper-api`.** Deliberately not done here to keep this change buildable against the existing dependency.
- **OWASP scan config is in place but unexecuted.** Run `mvn dependency-check:check -DnvdApiKey=$NVD_API_KEY` in a networked environment to actually enforce the CVSS gate.
- Manually smoke-tested against a live Paper 26.1.2 server with all toggles enabled; full CI run pending.

## Test plan

- [x] `shared-mc` unit tests: 15/15 green (TDD — `EventThrottle`, all new `Loggable*Event` carriers, `AbstractLoggableEvent` NPE fix, `SingleSplunkConnection.buildHecEnvelope`).
- [x] `mvn -o clean package` — full reactor builds clean on Java 21.
- [x] Manual live-server test (Paper 26.1.2): jar deployed, all `splunk.craft.enable.*` toggles set `true`, server restarted, verified `CombatEvent`/`ItemEvent`/`ProgressionEvent`/`ServerEvent`/`PerformanceEvent`/session-detail `PlayerEvent`s land in Splunk with expected fields.
- [ ] OWASP CVE gate run in CI with `NVD_API_KEY`.
- [ ] Decide on `paper-api` swap to restore TPS/MSPT/protocol-version.

# Design: Extend Logging Capabilities + Modernize Dependencies

**Date:** 2026-06-22
**Status:** Approved (design)
**Scope:** `shared-mc`, `spigot` modules. `forge` excluded (dead 11 years). `logtosplunk-plugin` packaging follows.

## Problem

The Splunk-for-Minecraft plugin logs only a thin slice of server activity (player
connect/disconnect/move/chat/advancement, block place/break, death). Before deploying to
other Minecraft servers we need:

1. **Richer telemetry** about the server and players (combat, survival, economy,
   progression, session detail, server lifecycle/performance).
2. **A clean, modern, CVE-free dependency baseline** — the build currently mixes ancient
   and current libraries and targets Java 1.8.

## Goals

- Add new loggable event categories without changing the existing event contract.
- Keep every new high-volume event opt-in and throttled so the plugin is safe to run on
  busy public servers.
- Land a modern, scanned, reproducible dependency set targeting Java 21.

## Non-goals

- Reviving the `forge` module (Minecraft ~1.7-era, untouched 11 years). It stays in the
  tree but is excluded from the reactor build. A separate future milestone may address it.
- A generic, config-driven event framework (rejected — YAGNI for ~15 event types).
- Changing the Splunk transport (`SingleSplunkConnection`) or HEC protocol.

## Platform decisions

| Decision | Choice | Reason |
|---|---|---|
| Target platform | Spigot/Paper (`spigot-api 1.21.10`) | Only live adapter; deploy target |
| Compile target | Java 21 | Paper 1.21.x requires Java 21 runtime; no value in 17 |
| Forge | Excluded from build | Dead 11 years; near-total rewrite to revive |

## Architecture

Preserve the existing pattern. shared-mc holds platform-agnostic data carriers
(`Loggable*Event` extending `AbstractLoggableEvent`); spigot holds Bukkit `Listener`
loggers extending `AbstractEventLogger`. Each logger is registered in
`LogToSplunkPlugin.onEnable` and gated by a `splunk.properties` toggle.

One new abstraction: a **`ScheduledMetricLogger`** base in shared-mc (or spigot) for
metrics that are not event-driven (TPS, MSPT, online player count). It is driven by the
Bukkit scheduler rather than the event bus.

### Workstream 1 — Dependency modernization (poms + build)

Largely independent of Workstream 2; touches build files and one shared-mc serialization
change.

1. **CVE baseline.** Add and run OWASP `dependency-check-maven` across the reactor.
   Capture a report; record any High/Critical findings.
2. **Java 1.8 → 21.** Update `maven-compiler-plugin` to a current version and set
   `<release>21</release>` in the parent pom. Verify no source incompatibilities.
3. **Unify `splunk-library-javalogging`.** Replace `1.0.1` in `logtosplunk-plugin` with
   `1.11.8` (already used by `shared-mc`). Manage the version centrally in parent
   `dependencyManagement`. `forge` carries the same stale `1.0.1` but is out of the
   reactor, so its pom is left untouched (cleaned up only if forge is ever revived).
4. **Drop `json-simple 1.1`.** Replace its use in `SplunkCimLogEvent` / `toJson()` with
   the already-present `gson 2.13.2`. This is the only Workstream-1 change that touches
   `shared-mc` Java source and the one ordering dependency for Workstream 2.
5. **Bump build plugins.** `maven-compiler-plugin`, `maven-shade-plugin`,
   `maven-clean-plugin`, `maven-install-plugin`, `exec-maven-plugin`, `junit` (4.8.2 →
   4.13.2) to current stable versions.
6. **Exclude forge.** Remove `<module>forge</module>` from the parent reactor (leave the
   directory and its pom intact).

**Exit gate:** `mvn clean package` green; dependency-check shows no unsuppressed
High/Critical CVEs; the shaded plugin jar builds.

### Workstream 2 — Logging extension (shared-mc + spigot)

New `LoggableEventType` enum values and data classes in shared-mc, paired with Bukkit
`Listener` loggers in spigot.

| Category | New `LoggableEventType` | New shared-mc class(es) | Bukkit hooks |
|---|---|---|---|
| Server lifecycle / performance | `SERVER`, `PERFORMANCE` | `LoggableServerEvent`, `LoggablePerformanceEvent` | `ServerLoadEvent`, plugin disable, `WeatherChangeEvent`; scheduled TPS / MSPT / player-count sampler |
| Combat & survival | `COMBAT` | `LoggableCombatEvent` | `EntityDamageByEntityEvent`, `EntityDeathEvent`, `EntityRegainHealthEvent`, `FoodLevelChangeEvent`, `PlayerRespawnEvent` |
| Economy & progression | `ITEM`, `PROGRESSION` | `LoggableItemEvent`, `LoggableProgressionEvent` | `EntityPickupItemEvent`, `PlayerDropItemEvent`, `PlayerExpChangeEvent`, `PlayerLevelChangeEvent`, `EnchantItemEvent`, `CraftItemEvent`, `PlayerFishEvent`, `PlayerCommandPreprocessEvent` |
| Session detail | extend `LoggablePlayerEvent` + new `PlayerEventAction` values | (extend existing) | `PlayerTeleportEvent`, `PlayerGameModeChangeEvent`, `PlayerBedEnterEvent`, `PlayerChangedWorldEvent`; enrich login with IP / UUID / protocol version |

### Cross-cutting concerns

- **Per-category enable toggles** in `splunk.properties`, read in `AbstractEventLogger` /
  plugin `onEnable`. Keys follow the existing `splunk.craft.enable.*` convention. New
  high-volume categories (combat, item, performance sampling) default to **off** so an
  upgrade does not silently flood a server's HEC.
- **Throttling** for high-frequency events (damage, food, item pickup) reusing the
  guava-cache + granularity pattern already in `PlayerEventLogger` (per-player, time- or
  distance-bounded). Performance sampler runs on a fixed tick interval (configurable,
  default e.g. every 600 ticks / 30 s).
- **Splunk CIM mapping** preserved via `SplunkCimLogEvent`; new fields map to CIM where a
  standard field exists.
- **PII note:** session detail logs player IP. Document this in the plugin config and gate
  it behind its own toggle (default off) so operators opt in deliberately.

## Components & isolation

- `shared-mc/loggable_events/*` — pure data carriers, no Bukkit dependency, unit-testable
  in isolation (construct, set fields, assert `toJson()` / CIM output).
- `spigot/eventloggers/*` — Bukkit `Listener`s, one per category, each translating Bukkit
  events into shared-mc loggables. Depend on shared-mc + Bukkit API only.
- `ScheduledMetricLogger` — owns the scheduler task; depends on the Splunk connection and a
  metric source. Testable by invoking its sample method directly.
- `LogToSplunkPlugin.onEnable` — wiring only: read toggles, register enabled listeners,
  start the sampler if enabled.

## Error handling

- Listeners must never throw into the Bukkit event bus. Wrap loggable construction +
  send in try/catch; on failure log a warning and drop the event (current behavior of
  `AbstractEventLogger.logAndSend` via the connection's async send is preserved).
- Missing/invalid config falls back to safe defaults (categories off, sampler off) and
  logs a warning, matching the existing properties-load fallback.

## Testing

- Unit tests per new `Loggable*Event`: field population and `toJson()` / CIM output.
- Throttle logic test (guava-cache eviction / granularity) for the high-volume path.
- `ScheduledMetricLogger` sample method test with a stubbed metric source.
- Build-level: `mvn clean package` green; dependency-check gate; shaded jar smoke-loads.

## Sequencing

1. **Workstream 1 first to green.** The `json-simple → gson` swap in `shared-mc` `toJson()`
   is the foundation Workstream 2's new classes serialize through; the Java 21 bump affects
   all modules.
2. **Workstream 2 in parallel after the shared-mc serialization change merges.** New event
   classes and loggers have near-zero file overlap with the remaining Workstream-1 pom
   edits, so a second agent can scaffold them concurrently.

## Open risks

- Paper API method availability: a few Bukkit hooks (e.g. accurate TPS) may require
  Paper-specific APIs vs vanilla Spigot. The implementation agent verifies against
  `spigot-api 1.21.10` and falls back to a manual tick-time sampler if needed.
- `EnchantItemEvent` / `CraftItemEvent` field richness varies; map only stable fields.

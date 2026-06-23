# Logging Extension & Dependency Modernization — Enhancements Writeup

**Date:** 2026-06-22
**Branch:** `feature/extend-logging-modernize-deps`
**Related:** [PR description](superpowers/PR-extend-logging-modernize-deps.md), [design spec](superpowers/specs/2026-06-22-extend-logging-and-modernize-deps-design.md), [logging plan](superpowers/plans/2026-06-22-logging-extension.md), [dependency plan](superpowers/plans/2026-06-22-dependency-modernization.md)

## 1. Motivation

The Splunk-for-Minecraft plugin (`logtosplunk-plugin` + `shared-mc` + `spigot`) shipped only seven event types: player connect/disconnect/chat/move/death and block break/place. Two problems blocked wider deployment:

1. **Insufficient telemetry.** Operators wanted dashboards covering combat outcomes, item/economy flow, player progression, and server health — none of which existed.
2. **Stale, vulnerable dependencies.** The build targeted Java 8, depended on an unmaintained JSON library (`json-simple`), and used a shade plugin version with known issues. Before deploying to other/newer Minecraft servers, both needed addressing.

This work tackles both in one branch since they touch the same modules and the dependency bump (Java 21, gson) is a prerequisite for the new logging code's cleanest implementation (gson for the new HEC envelope).

## 2. New logging categories

Each category is **independently toggled** via a `splunk.craft.enable.*` property, defaulting to `false`. An operator turns on exactly the telemetry they want; existing deployments see no behavior change until they edit `splunk.properties` and restart.

### Combat & survival (`enable.combat`)
Damage taken, kills, healing, hunger changes, respawns. Damage/heal/hunger events are *per-player throttled* (`EventThrottle`) because they fire on nearly every tick during a fight — without throttling, a single PvP encounter could generate thousands of Splunk events per minute. Kills deliberately do **not** duplicate the pre-existing `DeathEventLogger`'s `DeathEvent` — `CombatEventLogger` has no `EntityDeathEvent` handler, by design (see §4).

### Item economy (`enable.item`)
Pickup and drop. Pickup is throttled — picking up a stack of arrows or XP orbs fires one event per item. Drop is not throttled — it's a deliberate, low-frequency player action.

### Progression (`enable.progression`)
XP/level changes, enchanting, crafting, fishing, and command execution. Not throttled — naturally low-frequency.

### Server lifecycle & performance (`enable.server`, `enable.performance`)
Server start and weather change events fire from a `Listener`. Performance is different: there's no Bukkit event for "sample current load," so `PerformanceSampler` extends a new `ScheduledMetricLogger` base class that polls on a timer (`BukkitRunnable.runTaskTimer`, interval configurable via `splunk.craft.performance.interval_ticks`, default 600 ticks = 30s). It currently reports online player count and loaded chunk count. TPS and average tick time (`Bukkit.getTPS()`, `Bukkit.getAverageTickTime()`) are **Paper-only APIs absent from vanilla `spigot-api`** — confirmed by decompiling the cached `spigot-api-1.21.10-R0.1-SNAPSHOT.jar` with `javap`. The carrier class (`LoggablePerformanceEvent`) already has `setTps`/`setMspt` ready to use; wiring them up just requires switching the spigot module's dependency to `paper-api`. That swap was intentionally left out of this branch to avoid changing the project's API surface as a side effect of a logging feature — it's a separate decision for whoever owns the deployment target.

### Session detail (`enable.session_detail`, `enable.session_ip`)
Teleports, gamemode changes, bed entry, world changes — gated together behind `session_detail` since they're all "what is this player doing in this session" signals. Player UUID is now always captured on connect (cheap, stable identifier, useful for joining events across sessions). Client IP is a separate, narrower toggle (`session_ip`) — **this project explicitly does not treat session IP as PII** (confirmed by stakeholder), so it's just another opt-in data field, not a privacy-gated one. Protocol version was attempted but dropped for the same Paper-API reason as TPS/MSPT (`Player.getProtocolVersion()` doesn't exist on vanilla spigot-api).

## 3. Flood control: `EventThrottle`

A small, Bukkit-free utility (`shared-mc/.../util/EventThrottle.java`) backed by a Guava `Cache<String, Long>`. `allow(String key)` returns `true` (and records the timestamp) only if `key` hasn't been seen within the configured window; otherwise `false`. The constructor accepts an injectable `LongSupplier` clock specifically so unit tests can fast-forward time without `Thread.sleep`. This is the only new mechanism standing between high-frequency Bukkit events and an HEC token getting rate-limited or a Splunk index getting flooded.

## 4. Two bugs caught by final review (not per-task review)

The subagent-driven workflow used for this branch runs spec-compliance and code-quality review after *each* task, but cross-cutting issues only surface once the whole branch is reviewed together:

1. **Duplicate kill logging.** `CombatEventLogger.onDeath` and the pre-existing `DeathEventLogger.captureDeathEvent` both handled `EntityDeathEvent` for player kills — with `enable.combat=true`, every kill produced two Splunk events (`CombatEvent` + `DeathEvent`) describing the same fact. Fixed by removing `CombatEventLogger.onDeath` entirely; `DeathEventLogger` remains the single source of truth for deaths.
2. **Dead toggle.** `ENABLE_SESSION_DETAIL` was defined in `AbstractEventLogger` but nothing read it — the four session-detail handlers (`onTeleport`, `onGameModeChange`, `onBedEnter`, `onWorldChange`) ran unconditionally regardless of the config flag. Fixed by adding an `isEnabled(ENABLE_SESSION_DETAIL)` early-return guard to each.

Neither bug would fail a build or a unit test; both were "logically wrong but compiles fine" — exactly the class of bug a whole-branch read catches and per-file review misses.

## 5. Dependency modernization

| Area | Before | After |
|---|---|---|
| Java target | 8 | 21 |
| JSON library | `json-simple` | `gson` (HEC envelope rewritten in `SingleSplunkConnection.buildHecEnvelope`) |
| junit | 4.8.2 | 4.13.2 |
| maven-shade-plugin | 2.4.1 | 3.6.0 |
| maven-compiler-plugin | (implicit) | 3.14.0, `<release>21</release>` |
| Dependency versions | scattered per-module | centralized in parent `dependencyManagement` |
| `forge` module | in reactor, unmaintained 11+ yrs | removed from `<modules>` (dir kept, not deleted) |
| CVE scanning | none | OWASP `dependency-check-maven`, CVSS 7.0 fail threshold, `owasp-suppressions.xml` template |

The OWASP gate is **configured but not yet executed** in this environment — the anonymous NVD API enforces aggressive rate limiting (~5 req/30s against a 360k-record CVE database), so a full sync never completed locally. It needs to run once in a networked CI job with `NVD_API_KEY` set before it's a meaningful gate rather than a no-op.

## 6. What's verified vs. what's deferred

**Verified:**
- 15 shared-mc unit tests, TDD'd alongside every new data-carrier class and `EventThrottle`.
- Full reactor build (`mvn -o clean package`) green on Java 21.
- Live manual smoke test against a running Paper 26.1.2 server: new jar deployed, all toggles enabled, server restarted, confirmed each new event type (`CombatEvent`, `ItemEvent`, `ProgressionEvent`, `ServerEvent`, `PerformanceEvent`, session-detail `PlayerEvent` fields) reaches Splunk via HEC.

**Deferred (tracked, not silently dropped):**
- OWASP CVE scan execution (needs `NVD_API_KEY` + network access).
- `spigot-api` → `paper-api` swap to restore TPS/MSPT/protocol-version (pending a decision on whether the plugin should commit to Paper-only, vs. staying spigot-API-compatible).
- Branch is intentionally **not merged or pushed** — kept as-is pending review.

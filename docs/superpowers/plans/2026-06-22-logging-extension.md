# Logging Extension Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add server- and player-level telemetry (combat, survival, economy, progression, session detail, server lifecycle/performance) to the Splunk-for-Minecraft spigot plugin, every new category opt-in and throttled.

**Architecture:** Preserve the existing pattern — platform-agnostic `Loggable*Event` data carriers in `shared-mc` (extending `AbstractLoggableEvent`, serialized via gson `toJson()`), paired with Bukkit `Listener` loggers in `spigot` (extending `AbstractEventLogger`) registered in `LogToSplunkPlugin.onEnable`. One new abstraction, `ScheduledMetricLogger`, drives non-event metrics (TPS/MSPT/player count) off the Bukkit scheduler. Each category is gated by a `splunk.properties` toggle and high-frequency events are throttled.

**Tech Stack:** Java 21, Maven (`mvn-bin/`), `spigot-api 1.21.10`, gson 2.13.2, guava 33.5.0 cache, junit 4.13.2.

**Depends on:** `2026-06-22-dependency-modernization.md` — that plan's Java 21 target and gson HEC swap must land first. New event classes serialize through the gson `toJson()` path.

---

## Build & Test Commands (use throughout)

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export MVN="./mvn-bin/bin/mvn -f /home/splunk/appDev/minecraft-app/pom.xml"
```

- Test shared-mc POJOs: `$MVN -pl shared-mc -am test`
- Compile spigot (listener wiring gate): `$MVN -pl spigot -am clean compile`
- Full build incl. shaded plugin: `$MVN clean package`

Branch: `feature/extend-logging-modernize-deps` (continue on the same branch as the dependency plan).

---

## Testing strategy

- **shared-mc `Loggable*Event` classes** are pure POJOs (no Bukkit). Full TDD: construct, set fields, assert `toJson()` contains the expected keys/values.
- **Throttle logic** is extracted into a Bukkit-free `EventThrottle` helper in shared-mc so it is unit-testable without a server.
- **spigot `Listener` classes** require a running Bukkit server to exercise, which is out of scope for unit tests here. They are verified by (a) a clean `compile` gate proving they bind to the real `spigot-api 1.21.10` types, and (b) a manual smoke test against a local Paper server in the final task. Keep listeners thin: translate the Bukkit event to a `Loggable*Event` and call `logAndSend` — all assertable logic lives in the POJOs and `EventThrottle`.

---

## File Structure

**shared-mc (data carriers — unit tested):**

| File | Responsibility |
|---|---|
| `loggable_events/LoggableEventType.java` (modify) | Add `SERVER, PERFORMANCE, COMBAT, ITEM, PROGRESSION` |
| `loggable_events/AbstractLoggableEvent.java` (modify) | Add a null-coordinate-safe constructor for location-less events |
| `loggable_events/LoggableServerEvent.java` (create) | Server lifecycle (start/stop/weather) |
| `loggable_events/LoggablePerformanceEvent.java` (create) | TPS / MSPT / online count sample |
| `loggable_events/LoggableCombatEvent.java` (create) | Damage / kill / heal / hunger / respawn |
| `loggable_events/LoggableItemEvent.java` (create) | Item pickup / drop |
| `loggable_events/LoggableProgressionEvent.java` (create) | XP / level / enchant / craft / fish / command |
| `loggable_events/LoggablePlayerEvent.java` (modify) | New `PlayerEventAction`s + IP/UUID/protocol/gamemode setters |
| `util/EventThrottle.java` (create) | Bukkit-free per-key time/granularity throttle |

**spigot (Bukkit listeners — compile-gated + smoke):**

| File | Responsibility |
|---|---|
| `eventloggers/CombatEventLogger.java` (create) | Combat & survival hooks |
| `eventloggers/ItemEventLogger.java` (create) | Item pickup/drop hooks |
| `eventloggers/ProgressionEventLogger.java` (create) | XP/level/enchant/craft/fish/command hooks |
| `eventloggers/ServerEventLogger.java` (create) | ServerLoad + weather hooks |
| `eventloggers/PerformanceSampler.java` (create) | Scheduled TPS/MSPT/count sampler |
| `scheduling/ScheduledMetricLogger.java` (create) | Base for scheduler-driven loggers |
| `eventloggers/PlayerEventLogger.java` (modify) | Add session-detail hooks |
| `LogToSplunkPlugin.java` (modify) | Read toggles, register enabled loggers, start sampler |

**Config:**

| File | Responsibility |
|---|---|
| `sampleModConfig/splunk.properties` (modify, if present) | Document new toggle keys |

---

## Task 1: Extend LoggableEventType and add a location-less constructor (TDD)

The current `AbstractLoggableEvent` constructor dereferences `coordinates.xCoord` and NPEs if coordinates are null. Server and performance events have no location, so they need a safe constructor.

**Files:**
- Modify: `shared-mc/src/main/java/com/splunk/sharedmc/loggable_events/LoggableEventType.java`
- Modify: `shared-mc/src/main/java/com/splunk/sharedmc/loggable_events/AbstractLoggableEvent.java`
- Test: `shared-mc/src/test/java/com/splunk/sharedmc/loggable_events/AbstractLoggableEventTest.java` (create)

- [ ] **Step 1: Write the failing test**

Create `shared-mc/src/test/java/com/splunk/sharedmc/loggable_events/AbstractLoggableEventTest.java`:

```java
package com.splunk.sharedmc.loggable_events;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class AbstractLoggableEventTest {

    @Test
    public void locationLessConstructor_doesNotNpe_andOmitsCoords() {
        AbstractLoggableEvent e =
                new AbstractLoggableEvent(LoggableEventType.SERVER, 0L, "world");
        String json = e.toJson();
        assertTrue(json.contains("SERVER".toLowerCase()) || json.contains("ServerEvent"));
        assertFalse("location-less event must not emit xCoord", json.contains("xCoord"));
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `$MVN -pl shared-mc -am test -Dtest=AbstractLoggableEventTest`
Expected: FAIL — `SERVER` enum constant and the 3-arg constructor do not exist (compile error).

- [ ] **Step 3: Add the new enum values**

In `LoggableEventType.java`, extend the enum:

```java
public enum LoggableEventType {
    PLAYER("PlayerEvent"),
    BLOCK("BlockEvent"),
    DEATH("DeathEvent"),
    SERVER("ServerEvent"),
    PERFORMANCE("PerformanceEvent"),
    COMBAT("CombatEvent"),
    ITEM("ItemEvent"),
    PROGRESSION("ProgressionEvent");

    private final String eventName;

    LoggableEventType(String eventName) {
        this.eventName = eventName;
    }

    public String getEventName() {
        return eventName;
    }
}
```

- [ ] **Step 4: Add the location-less constructor**

In `AbstractLoggableEvent.java`, add a constructor that skips coordinate fields, and have the existing constructor reject null coordinates explicitly. Replace the constructor (lines 23-36) with:

```java
public AbstractLoggableEvent(LoggableEventType type, long worldTime, String worldName, Point3dLong coordinates) {
    this(type, worldTime, worldName);
    if (coordinates != null) {
        this.addField("xCoord", coordinates.xCoord);
        this.addField("yCoord", coordinates.yCoord);
        this.addField("zCoord", coordinates.zCoord);
    }
}

/**
 * Constructor for events with no world location (e.g. server lifecycle, performance).
 */
public AbstractLoggableEvent(LoggableEventType type, long worldTime, String worldName) {
    super(type.getEventName(), "");
    this.addField("time", System.currentTimeMillis());
    this.addField("game_time", worldTime);
    if (worldName != null) {
        this.addField("world", worldName);
    }
}
```

- [ ] **Step 5: Run the test to verify it passes**

Run: `$MVN -pl shared-mc -am test -Dtest=AbstractLoggableEventTest`
Expected: PASS.

- [ ] **Step 6: Run the full shared-mc test suite to confirm no regression**

Run: `$MVN -pl shared-mc -am test`
Expected: PASS (existing Player/Block/Death events still serialize with coordinates).

- [ ] **Step 7: Commit**

```bash
git add shared-mc/src/main/java/com/splunk/sharedmc/loggable_events/LoggableEventType.java \
        shared-mc/src/main/java/com/splunk/sharedmc/loggable_events/AbstractLoggableEvent.java \
        shared-mc/src/test/java/com/splunk/sharedmc/loggable_events/AbstractLoggableEventTest.java
git commit -m "feat(shared-mc): add SERVER/PERFORMANCE/COMBAT/ITEM/PROGRESSION types + location-less event ctor"
```

---

## Task 2: LoggableServerEvent + LoggablePerformanceEvent (TDD)

**Files:**
- Create: `shared-mc/src/main/java/com/splunk/sharedmc/loggable_events/LoggableServerEvent.java`
- Create: `shared-mc/src/main/java/com/splunk/sharedmc/loggable_events/LoggablePerformanceEvent.java`
- Test: `shared-mc/src/test/java/com/splunk/sharedmc/loggable_events/LoggableServerEventTest.java`
- Test: `shared-mc/src/test/java/com/splunk/sharedmc/loggable_events/LoggablePerformanceEventTest.java`

- [ ] **Step 1: Write the failing tests**

Create `LoggableServerEventTest.java`:

```java
package com.splunk.sharedmc.loggable_events;

import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class LoggableServerEventTest {
    @Test
    public void serverStart_serializesAction() {
        LoggableServerEvent e = new LoggableServerEvent(
                LoggableServerEvent.ServerAction.SERVER_START, 0L, null);
        String json = e.toJson();
        assertTrue(json.contains("server_start"));
    }

    @Test
    public void weatherChange_carriesState() {
        LoggableServerEvent e = new LoggableServerEvent(
                LoggableServerEvent.ServerAction.WEATHER_CHANGE, 1000L, "world");
        e.setWeather("storm");
        String json = e.toJson();
        assertTrue(json.contains("weather"));
        assertTrue(json.contains("storm"));
    }
}
```

Create `LoggablePerformanceEventTest.java`:

```java
package com.splunk.sharedmc.loggable_events;

import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class LoggablePerformanceEventTest {
    @Test
    public void sample_serializesMetrics() {
        LoggablePerformanceEvent e = new LoggablePerformanceEvent(0L);
        e.setTps(19.8).setMspt(8.4).setOnlinePlayers(12).setLoadedChunks(1500);
        String json = e.toJson();
        assertTrue(json.contains("tps"));
        assertTrue(json.contains("19.8"));
        assertTrue(json.contains("mspt"));
        assertTrue(json.contains("online_players"));
    }
}
```

- [ ] **Step 2: Run to verify failure**

Run: `$MVN -pl shared-mc -am test -Dtest=LoggableServerEventTest,LoggablePerformanceEventTest`
Expected: FAIL — classes do not exist.

- [ ] **Step 3: Implement LoggableServerEvent**

Create `LoggableServerEvent.java`:

```java
package com.splunk.sharedmc.loggable_events;

/**
 * Server lifecycle and world-state events (start, stop, weather).
 */
public class LoggableServerEvent extends AbstractLoggableEvent {

    public LoggableServerEvent(ServerAction action, long gameTime, String worldName) {
        super(LoggableEventType.SERVER, gameTime, worldName);
        this.addField(ACTION, action.asString());
    }

    public LoggableServerEvent setWeather(String weather) {
        this.addField("weather", weather);
        return this;
    }

    public LoggableServerEvent setMotd(String motd) {
        this.addField("motd", motd);
        return this;
    }

    public enum ServerAction {
        SERVER_START("server_start"),
        SERVER_STOP("server_stop"),
        WEATHER_CHANGE("weather_change");

        private final String action;
        ServerAction(String action) { this.action = action; }
        public String asString() { return action; }
    }
}
```

- [ ] **Step 4: Implement LoggablePerformanceEvent**

Create `LoggablePerformanceEvent.java`:

```java
package com.splunk.sharedmc.loggable_events;

/**
 * A sampled snapshot of server performance metrics.
 */
public class LoggablePerformanceEvent extends AbstractLoggableEvent {

    public LoggablePerformanceEvent(long gameTime) {
        super(LoggableEventType.PERFORMANCE, gameTime, null);
        this.addField(ACTION, "performance_sample");
    }

    public LoggablePerformanceEvent setTps(double tps) {
        this.addField("tps", tps);
        return this;
    }

    public LoggablePerformanceEvent setMspt(double mspt) {
        this.addField("mspt", mspt);
        return this;
    }

    public LoggablePerformanceEvent setOnlinePlayers(int count) {
        this.addField("online_players", count);
        return this;
    }

    public LoggablePerformanceEvent setLoadedChunks(int chunks) {
        this.addField("loaded_chunks", chunks);
        return this;
    }
}
```

- [ ] **Step 5: Run to verify pass**

Run: `$MVN -pl shared-mc -am test -Dtest=LoggableServerEventTest,LoggablePerformanceEventTest`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add shared-mc/src/main/java/com/splunk/sharedmc/loggable_events/LoggableServerEvent.java \
        shared-mc/src/main/java/com/splunk/sharedmc/loggable_events/LoggablePerformanceEvent.java \
        shared-mc/src/test/java/com/splunk/sharedmc/loggable_events/LoggableServerEventTest.java \
        shared-mc/src/test/java/com/splunk/sharedmc/loggable_events/LoggablePerformanceEventTest.java
git commit -m "feat(shared-mc): add LoggableServerEvent and LoggablePerformanceEvent"
```

---

## Task 3: LoggableCombatEvent (TDD)

Covers damage, kills, healing, hunger, respawn — survival + combat in one carrier with an action enum.

**Files:**
- Create: `shared-mc/src/main/java/com/splunk/sharedmc/loggable_events/LoggableCombatEvent.java`
- Test: `shared-mc/src/test/java/com/splunk/sharedmc/loggable_events/LoggableCombatEventTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.splunk.sharedmc.loggable_events;

import static org.junit.Assert.assertTrue;
import com.splunk.sharedmc.Point3dLong;
import org.junit.Test;

public class LoggableCombatEventTest {
    @Test
    public void damage_carriesAttackerVictimAndAmount() {
        LoggableCombatEvent e = new LoggableCombatEvent(
                LoggableCombatEvent.CombatAction.DAMAGE, 0L, "world", new Point3dLong(1, 2, 3));
        e.setVictim("Steve").setSource("Zombie").setAmount(4.5).setCause("ENTITY_ATTACK");
        String json = e.toJson();
        assertTrue(json.contains("victim"));
        assertTrue(json.contains("Steve"));
        assertTrue(json.contains("source"));
        assertTrue(json.contains("4.5"));
        assertTrue(json.contains("damage"));
    }

    @Test
    public void hunger_carriesFoodLevel() {
        LoggableCombatEvent e = new LoggableCombatEvent(
                LoggableCombatEvent.CombatAction.HUNGER, 0L, "world", null);
        e.setVictim("Alex").setFoodLevel(7);
        String json = e.toJson();
        assertTrue(json.contains("food_level"));
        assertTrue(json.contains("hunger"));
    }
}
```

- [ ] **Step 2: Run to verify failure**

Run: `$MVN -pl shared-mc -am test -Dtest=LoggableCombatEventTest`
Expected: FAIL — class missing.

- [ ] **Step 3: Implement LoggableCombatEvent**

```java
package com.splunk.sharedmc.loggable_events;

import com.splunk.sharedmc.Point3dLong;

/**
 * Combat and survival events: damage dealt/taken, kills, healing, hunger, respawn.
 */
public class LoggableCombatEvent extends AbstractLoggableEvent {

    public LoggableCombatEvent(CombatAction action, long gameTime, String worldName, Point3dLong location) {
        super(LoggableEventType.COMBAT, gameTime, worldName, location);
        this.addField(ACTION, action.asString());
    }

    public LoggableCombatEvent setVictim(String victim) {
        this.addField("victim", victim);
        return this;
    }

    public LoggableCombatEvent setSource(String source) {
        this.addField("source", source);
        return this;
    }

    public LoggableCombatEvent setAmount(double amount) {
        this.addField("amount", amount);
        return this;
    }

    public LoggableCombatEvent setCause(String cause) {
        this.addField(CAUSE, cause);
        return this;
    }

    public LoggableCombatEvent setFoodLevel(int foodLevel) {
        this.addField("food_level", foodLevel);
        return this;
    }

    public LoggableCombatEvent setHealthRemaining(double health) {
        this.addField("health_remaining", health);
        return this;
    }

    public enum CombatAction {
        DAMAGE("damage"),
        KILL("kill"),
        HEAL("heal"),
        HUNGER("hunger"),
        RESPAWN("respawn");

        private final String action;
        CombatAction(String action) { this.action = action; }
        public String asString() { return action; }
    }
}
```

- [ ] **Step 4: Run to verify pass**

Run: `$MVN -pl shared-mc -am test -Dtest=LoggableCombatEventTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add shared-mc/src/main/java/com/splunk/sharedmc/loggable_events/LoggableCombatEvent.java \
        shared-mc/src/test/java/com/splunk/sharedmc/loggable_events/LoggableCombatEventTest.java
git commit -m "feat(shared-mc): add LoggableCombatEvent"
```

---

## Task 4: LoggableItemEvent + LoggableProgressionEvent (TDD)

**Files:**
- Create: `shared-mc/src/main/java/com/splunk/sharedmc/loggable_events/LoggableItemEvent.java`
- Create: `shared-mc/src/main/java/com/splunk/sharedmc/loggable_events/LoggableProgressionEvent.java`
- Test: `shared-mc/src/test/java/com/splunk/sharedmc/loggable_events/LoggableItemEventTest.java`
- Test: `shared-mc/src/test/java/com/splunk/sharedmc/loggable_events/LoggableProgressionEventTest.java`

- [ ] **Step 1: Write the failing tests**

`LoggableItemEventTest.java`:

```java
package com.splunk.sharedmc.loggable_events;

import static org.junit.Assert.assertTrue;
import com.splunk.sharedmc.Point3dLong;
import org.junit.Test;

public class LoggableItemEventTest {
    @Test
    public void pickup_carriesItemAndQuantity() {
        LoggableItemEvent e = new LoggableItemEvent(
                LoggableItemEvent.ItemAction.PICKUP, 0L, "world", new Point3dLong(0, 64, 0));
        e.setPlayerName("Steve").setItem("DIAMOND").setQuantity(3);
        String json = e.toJson();
        assertTrue(json.contains("pickup"));
        assertTrue(json.contains("DIAMOND"));
        assertTrue(json.contains("quantity"));
    }
}
```

`LoggableProgressionEventTest.java`:

```java
package com.splunk.sharedmc.loggable_events;

import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class LoggableProgressionEventTest {
    @Test
    public void levelUp_carriesNewLevel() {
        LoggableProgressionEvent e = new LoggableProgressionEvent(
                LoggableProgressionEvent.ProgressionAction.LEVEL_CHANGE, 0L, "world");
        e.setPlayerName("Alex").setNewLevel(30);
        String json = e.toJson();
        assertTrue(json.contains("level_change"));
        assertTrue(json.contains("new_level"));
        assertTrue(json.contains("30"));
    }

    @Test
    public void command_carriesCommandText() {
        LoggableProgressionEvent e = new LoggableProgressionEvent(
                LoggableProgressionEvent.ProgressionAction.COMMAND, 0L, "world");
        e.setPlayerName("Alex").setDetail("/gamemode creative");
        String json = e.toJson();
        assertTrue(json.contains("command"));
        assertTrue(json.contains("gamemode creative"));
    }
}
```

- [ ] **Step 2: Run to verify failure**

Run: `$MVN -pl shared-mc -am test -Dtest=LoggableItemEventTest,LoggableProgressionEventTest`
Expected: FAIL — classes missing.

- [ ] **Step 3: Implement LoggableItemEvent**

```java
package com.splunk.sharedmc.loggable_events;

import com.splunk.sharedmc.Point3dLong;

/**
 * Item economy events: pickup and drop.
 */
public class LoggableItemEvent extends AbstractLoggableEvent {

    public LoggableItemEvent(ItemAction action, long gameTime, String worldName, Point3dLong location) {
        super(LoggableEventType.ITEM, gameTime, worldName, location);
        this.addField(ACTION, action.asString());
    }

    public LoggableItemEvent setPlayerName(String playerName) {
        this.addField(PLAYER_NAME, playerName);
        return this;
    }

    public LoggableItemEvent setItem(String item) {
        this.addField("item", item);
        return this;
    }

    public LoggableItemEvent setQuantity(int quantity) {
        this.addField("quantity", quantity);
        return this;
    }

    public enum ItemAction {
        PICKUP("pickup"),
        DROP("drop");

        private final String action;
        ItemAction(String action) { this.action = action; }
        public String asString() { return action; }
    }
}
```

- [ ] **Step 4: Implement LoggableProgressionEvent**

```java
package com.splunk.sharedmc.loggable_events;

/**
 * Player progression and activity: XP, level, enchant, craft, fish, command.
 */
public class LoggableProgressionEvent extends AbstractLoggableEvent {

    public LoggableProgressionEvent(ProgressionAction action, long gameTime, String worldName) {
        super(LoggableEventType.PROGRESSION, gameTime, worldName);
        this.addField(ACTION, action.asString());
    }

    public LoggableProgressionEvent setPlayerName(String playerName) {
        this.addField(PLAYER_NAME, playerName);
        return this;
    }

    public LoggableProgressionEvent setNewLevel(int level) {
        this.addField("new_level", level);
        return this;
    }

    public LoggableProgressionEvent setExpAmount(int exp) {
        this.addField("exp_amount", exp);
        return this;
    }

    /** Free-form detail: command text, enchant name, crafted item, fish caught. */
    public LoggableProgressionEvent setDetail(String detail) {
        this.addField("detail", detail);
        return this;
    }

    public enum ProgressionAction {
        EXP_CHANGE("exp_change"),
        LEVEL_CHANGE("level_change"),
        ENCHANT("enchant"),
        CRAFT("craft"),
        FISH("fish"),
        COMMAND("command");

        private final String action;
        ProgressionAction(String action) { this.action = action; }
        public String asString() { return action; }
    }
}
```

- [ ] **Step 5: Run to verify pass**

Run: `$MVN -pl shared-mc -am test -Dtest=LoggableItemEventTest,LoggableProgressionEventTest`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add shared-mc/src/main/java/com/splunk/sharedmc/loggable_events/LoggableItemEvent.java \
        shared-mc/src/main/java/com/splunk/sharedmc/loggable_events/LoggableProgressionEvent.java \
        shared-mc/src/test/java/com/splunk/sharedmc/loggable_events/LoggableItemEventTest.java \
        shared-mc/src/test/java/com/splunk/sharedmc/loggable_events/LoggableProgressionEventTest.java
git commit -m "feat(shared-mc): add LoggableItemEvent and LoggableProgressionEvent"
```

---

## Task 5: Extend LoggablePlayerEvent with session detail (TDD)

Add session-detail actions and fields (IP, UUID, protocol version, gamemode) to the existing player event.

**Files:**
- Modify: `shared-mc/src/main/java/com/splunk/sharedmc/loggable_events/LoggablePlayerEvent.java`
- Test: `shared-mc/src/test/java/com/splunk/sharedmc/loggable_events/LoggablePlayerEventTest.java` (create)

- [ ] **Step 1: Write the failing test**

```java
package com.splunk.sharedmc.loggable_events;

import static org.junit.Assert.assertTrue;
import com.splunk.sharedmc.Point3dLong;
import org.junit.Test;

public class LoggablePlayerEventTest {
    @Test
    public void connect_carriesSessionDetail() {
        LoggablePlayerEvent e = new LoggablePlayerEvent(
                LoggablePlayerEvent.PlayerEventAction.PLAYER_CONNECT, 0L, "world", new Point3dLong(0, 64, 0));
        e.setPlayerName("Steve")
         .setPlayerUuid("11111111-2222-3333-4444-555555555555")
         .setPlayerIp("203.0.113.7")
         .setProtocolVersion(767);
        String json = e.toJson();
        assertTrue(json.contains("uuid"));
        assertTrue(json.contains("203.0.113.7"));
        assertTrue(json.contains("protocol_version"));
    }

    @Test
    public void gamemodeChange_serializesAction() {
        LoggablePlayerEvent e = new LoggablePlayerEvent(
                LoggablePlayerEvent.PlayerEventAction.GAMEMODE_CHANGE, 0L, "world", new Point3dLong(0, 64, 0));
        e.setGamemode("CREATIVE");
        String json = e.toJson();
        assertTrue(json.contains("gamemode_change"));
        assertTrue(json.contains("CREATIVE"));
    }
}
```

- [ ] **Step 2: Run to verify failure**

Run: `$MVN -pl shared-mc -am test -Dtest=LoggablePlayerEventTest`
Expected: FAIL — new actions and setters missing.

- [ ] **Step 3: Add new actions and setters**

In `LoggablePlayerEvent.java`, extend the `PlayerEventAction` enum with:

```java
TELEPORT("teleport"),
GAMEMODE_CHANGE("gamemode_change"),
BED_ENTER("bed_enter"),
WORLD_CHANGE("world_change"),
```

(insert before `ADVANCEMENT("advancement");`, keeping the existing constants).

Add these setter methods to the class body (alongside the existing setters):

```java
public LoggablePlayerEvent setPlayerUuid(String uuid) {
    this.addField("uuid", uuid);
    return this;
}

public LoggablePlayerEvent setPlayerIp(String ip) {
    this.addField("client_ip", ip);
    return this;
}

public LoggablePlayerEvent setProtocolVersion(int protocol) {
    this.addField("protocol_version", protocol);
    return this;
}

public LoggablePlayerEvent setGamemode(String gamemode) {
    this.addField("gamemode", gamemode);
    return this;
}
```

Note: the test asserts the substring `protocol_version` and the field key for IP is `client_ip` — but the connect test only checks `203.0.113.7` substring, so the IP key name is free. Keep `client_ip` for Splunk CIM `src_ip`-style clarity.

- [ ] **Step 4: Run to verify pass**

Run: `$MVN -pl shared-mc -am test -Dtest=LoggablePlayerEventTest`
Expected: PASS.

- [ ] **Step 5: Full shared-mc suite**

Run: `$MVN -pl shared-mc -am test`
Expected: PASS (all Loggable* tests green).

- [ ] **Step 6: Commit**

```bash
git add shared-mc/src/main/java/com/splunk/sharedmc/loggable_events/LoggablePlayerEvent.java \
        shared-mc/src/test/java/com/splunk/sharedmc/loggable_events/LoggablePlayerEventTest.java
git commit -m "feat(shared-mc): add session-detail actions and fields to LoggablePlayerEvent"
```

---

## Task 6: EventThrottle helper (TDD)

A Bukkit-free per-key throttle so high-frequency listeners (damage, food, item pickup) don't flood Splunk. Reuses the guava cache idea from `PlayerEventLogger` but extracted and testable.

**Files:**
- Create: `shared-mc/src/main/java/com/splunk/sharedmc/util/EventThrottle.java`
- Test: `shared-mc/src/test/java/com/splunk/sharedmc/util/EventThrottleTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.splunk.sharedmc.util;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import org.junit.Test;

public class EventThrottleTest {
    @Test
    public void firstEventForKeyPasses_secondWithinWindowBlocked() {
        // window of 1000ms; supply our own clock for determinism
        long[] now = {1_000L};
        EventThrottle throttle = new EventThrottle(1000L, () -> now[0]);

        assertTrue("first event passes", throttle.allow("Steve"));
        now[0] = 1_500L; // 500ms later, inside window
        assertFalse("second event inside window blocked", throttle.allow("Steve"));
        now[0] = 2_100L; // 1100ms after first, outside window
        assertTrue("event after window passes", throttle.allow("Steve"));
    }

    @Test
    public void differentKeysAreIndependent() {
        long[] now = {0L};
        EventThrottle throttle = new EventThrottle(1000L, () -> now[0]);
        assertTrue(throttle.allow("Steve"));
        assertTrue(throttle.allow("Alex"));
    }
}
```

- [ ] **Step 2: Run to verify failure**

Run: `$MVN -pl shared-mc -am test -Dtest=EventThrottleTest`
Expected: FAIL — class missing.

- [ ] **Step 3: Implement EventThrottle**

```java
package com.splunk.sharedmc.util;

import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * Per-key time-window throttle. {@link #allow(String)} returns true at most once per
 * window per key. Backed by a size-bounded guava cache so it is safe for many players.
 */
public class EventThrottle {

    private static final int MAX_KEYS = 1024;

    private final long windowMillis;
    private final LongSupplier clock;
    private final Cache<String, Long> lastAllowed;

    public EventThrottle(long windowMillis) {
        this(windowMillis, System::currentTimeMillis);
    }

    /** Testable constructor with an injectable clock. */
    public EventThrottle(long windowMillis, LongSupplier clock) {
        this.windowMillis = windowMillis;
        this.clock = clock;
        this.lastAllowed = CacheBuilder.newBuilder()
                .maximumSize(MAX_KEYS)
                .expireAfterAccess(windowMillis * 4, TimeUnit.MILLISECONDS)
                .build();
    }

    /**
     * @return true if an event for {@code key} should be sent now (first call, or the
     *         window since the last allowed call has elapsed); false to drop it.
     */
    public synchronized boolean allow(String key) {
        long now = clock.getAsLong();
        Long last = lastAllowed.getIfPresent(key);
        if (last == null || (now - last) >= windowMillis) {
            lastAllowed.put(key, now);
            return true;
        }
        return false;
    }
}
```

- [ ] **Step 4: Run to verify pass**

Run: `$MVN -pl shared-mc -am test -Dtest=EventThrottleTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add shared-mc/src/main/java/com/splunk/sharedmc/util/EventThrottle.java \
        shared-mc/src/test/java/com/splunk/sharedmc/util/EventThrottleTest.java
git commit -m "feat(shared-mc): add Bukkit-free EventThrottle helper"
```

---

## Task 7: Config toggles in AbstractEventLogger

Expose per-category enable flags read from the same `Properties` already passed to loggers.

**Files:**
- Modify: `shared-mc/src/main/java/com/splunk/sharedmc/event_loggers/AbstractEventLogger.java`

- [ ] **Step 1: Add toggle keys and a protected accessor**

In `AbstractEventLogger.java`, add these constants near the existing `*_PROP_KEY` constants:

```java
public static final String ENABLE_COMBAT = "splunk.craft.enable.combat";
public static final String ENABLE_ITEM = "splunk.craft.enable.item";
public static final String ENABLE_PROGRESSION = "splunk.craft.enable.progression";
public static final String ENABLE_SESSION_DETAIL = "splunk.craft.enable.session_detail";
public static final String ENABLE_SERVER = "splunk.craft.enable.server";
public static final String ENABLE_PERFORMANCE = "splunk.craft.enable.performance";
public static final String ENABLE_SESSION_IP = "splunk.craft.enable.session_ip";
public static final String PERFORMANCE_INTERVAL_TICKS = "splunk.craft.performance.interval_ticks";
```

Store the `Properties` on the instance so subclasses can read toggles. Change the field section and constructor: add an instance field `protected final Properties props;` and assign `this.props = properties;` at the top of the constructor. Then add:

```java
/** Reads a boolean toggle; default false keeps high-volume categories opt-in. */
protected boolean isEnabled(String key) {
    return Boolean.parseBoolean(props.getProperty(key, "false"));
}

protected int intProp(String key, int defaultValue) {
    try {
        return Integer.parseInt(props.getProperty(key, Integer.toString(defaultValue)));
    } catch (NumberFormatException e) {
        return defaultValue;
    }
}
```

- [ ] **Step 2: Compile shared-mc**

Run: `$MVN -pl shared-mc -am clean compile`
Expected: BUILD SUCCESS.

- [ ] **Step 3: Commit**

```bash
git add shared-mc/src/main/java/com/splunk/sharedmc/event_loggers/AbstractEventLogger.java
git commit -m "feat(shared-mc): add per-category enable toggles to AbstractEventLogger"
```

---

## Task 8: CombatEventLogger (spigot)

**Files:**
- Create: `spigot/src/main/java/com/splunk/spigot/eventloggers/CombatEventLogger.java`

- [ ] **Step 1: Implement the listener**

Create `CombatEventLogger.java`:

```java
package com.splunk.spigot.eventloggers;

import static com.splunk.spigot.LogToSplunkPlugin.locationAsPoint;

import java.util.Properties;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import com.splunk.sharedmc.event_loggers.AbstractEventLogger;
import com.splunk.sharedmc.loggable_events.LoggableCombatEvent;
import com.splunk.sharedmc.loggable_events.LoggableCombatEvent.CombatAction;
import com.splunk.sharedmc.util.EventThrottle;

/**
 * Logs combat and survival events. High-frequency events (damage, hunger) are throttled
 * per-player.
 */
public class CombatEventLogger extends AbstractEventLogger implements Listener {

    private static final long THROTTLE_MS = 1000L;
    private final EventThrottle throttle = new EventThrottle(THROTTLE_MS);

    public CombatEventLogger(Properties props) {
        super(props);
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        Player victim = (Player) event.getEntity();
        if (!throttle.allow("dmg:" + victim.getName())) {
            return;
        }
        LoggableCombatEvent loggable = new LoggableCombatEvent(
                CombatAction.DAMAGE, victim.getWorld().getTime(), victim.getWorld().getName(),
                locationAsPoint(victim.getLocation()));
        loggable.setVictim(victim.getName())
                .setSource(event.getDamager().getType().toString())
                .setAmount(event.getFinalDamage())
                .setCause(event.getCause().toString())
                .setHealthRemaining(victim.getHealth());
        logAndSend(loggable);
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        // Player deaths are already handled by DeathEventLogger; log mob kills with a killer.
        if (event.getEntity() instanceof Player) {
            return;
        }
        Player killer = event.getEntity().getKiller();
        if (killer == null) {
            return;
        }
        LoggableCombatEvent loggable = new LoggableCombatEvent(
                CombatAction.KILL, event.getEntity().getWorld().getTime(),
                event.getEntity().getWorld().getName(), locationAsPoint(event.getEntity().getLocation()));
        loggable.setSource(killer.getName())
                .setVictim(event.getEntity().getType().toString());
        logAndSend(loggable);
    }

    @EventHandler
    public void onRegainHealth(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getEntity();
        if (!throttle.allow("heal:" + player.getName())) {
            return;
        }
        LoggableCombatEvent loggable = new LoggableCombatEvent(
                CombatAction.HEAL, player.getWorld().getTime(), player.getWorld().getName(),
                locationAsPoint(player.getLocation()));
        loggable.setVictim(player.getName())
                .setAmount(event.getAmount())
                .setCause(event.getRegainReason().toString())
                .setHealthRemaining(player.getHealth());
        logAndSend(loggable);
    }

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getEntity();
        if (!throttle.allow("food:" + player.getName())) {
            return;
        }
        LoggableCombatEvent loggable = new LoggableCombatEvent(
                CombatAction.HUNGER, player.getWorld().getTime(), player.getWorld().getName(),
                locationAsPoint(player.getLocation()));
        loggable.setVictim(player.getName()).setFoodLevel(event.getFoodLevel());
        logAndSend(loggable);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        LoggableCombatEvent loggable = new LoggableCombatEvent(
                CombatAction.RESPAWN, event.getPlayer().getWorld().getTime(),
                event.getPlayer().getWorld().getName(), locationAsPoint(event.getRespawnLocation()));
        loggable.setVictim(event.getPlayer().getName());
        logAndSend(loggable);
    }
}
```

- [ ] **Step 2: Compile spigot (binds to real spigot-api types)**

Run: `$MVN -pl spigot -am clean compile`
Expected: BUILD SUCCESS. If a method (e.g. `getFinalDamage`) is unavailable on the pinned API, the compile fails here — adjust to the available accessor.

- [ ] **Step 3: Commit**

```bash
git add spigot/src/main/java/com/splunk/spigot/eventloggers/CombatEventLogger.java
git commit -m "feat(spigot): add CombatEventLogger with per-player throttling"
```

---

## Task 9: ItemEventLogger + ProgressionEventLogger (spigot)

**Files:**
- Create: `spigot/src/main/java/com/splunk/spigot/eventloggers/ItemEventLogger.java`
- Create: `spigot/src/main/java/com/splunk/spigot/eventloggers/ProgressionEventLogger.java`

- [ ] **Step 1: Implement ItemEventLogger**

```java
package com.splunk.spigot.eventloggers;

import static com.splunk.spigot.LogToSplunkPlugin.locationAsPoint;

import java.util.Properties;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;

import com.splunk.sharedmc.event_loggers.AbstractEventLogger;
import com.splunk.sharedmc.loggable_events.LoggableItemEvent;
import com.splunk.sharedmc.loggable_events.LoggableItemEvent.ItemAction;
import com.splunk.sharedmc.util.EventThrottle;

/**
 * Logs item pickup and drop. Pickups are throttled per-player to avoid floods.
 */
public class ItemEventLogger extends AbstractEventLogger implements Listener {

    private final EventThrottle throttle = new EventThrottle(1000L);

    public ItemEventLogger(Properties props) {
        super(props);
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getEntity();
        if (!throttle.allow("pickup:" + player.getName())) {
            return;
        }
        LoggableItemEvent loggable = new LoggableItemEvent(
                ItemAction.PICKUP, player.getWorld().getTime(), player.getWorld().getName(),
                locationAsPoint(player.getLocation()));
        loggable.setPlayerName(player.getName())
                .setItem(event.getItem().getItemStack().getType().toString())
                .setQuantity(event.getItem().getItemStack().getAmount());
        logAndSend(loggable);
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        LoggableItemEvent loggable = new LoggableItemEvent(
                ItemAction.DROP, player.getWorld().getTime(), player.getWorld().getName(),
                locationAsPoint(player.getLocation()));
        loggable.setPlayerName(player.getName())
                .setItem(event.getItemDrop().getItemStack().getType().toString())
                .setQuantity(event.getItemDrop().getItemStack().getAmount());
        logAndSend(loggable);
    }
}
```

- [ ] **Step 2: Implement ProgressionEventLogger**

```java
package com.splunk.spigot.eventloggers;

import java.util.Properties;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerLevelChangeEvent;
import org.bukkit.entity.Player;

import com.splunk.sharedmc.event_loggers.AbstractEventLogger;
import com.splunk.sharedmc.loggable_events.LoggableProgressionEvent;
import com.splunk.sharedmc.loggable_events.LoggableProgressionEvent.ProgressionAction;

/**
 * Logs progression and activity: XP, level, enchant, craft, fish, command.
 */
public class ProgressionEventLogger extends AbstractEventLogger implements Listener {

    public ProgressionEventLogger(Properties props) {
        super(props);
    }

    private LoggableProgressionEvent base(ProgressionAction action, Player player) {
        LoggableProgressionEvent e = new LoggableProgressionEvent(
                action, player.getWorld().getTime(), player.getWorld().getName());
        e.setPlayerName(player.getName());
        return e;
    }

    @EventHandler
    public void onExpChange(PlayerExpChangeEvent event) {
        LoggableProgressionEvent e = base(ProgressionAction.EXP_CHANGE, event.getPlayer());
        e.setExpAmount(event.getAmount());
        logAndSend(e);
    }

    @EventHandler
    public void onLevelChange(PlayerLevelChangeEvent event) {
        LoggableProgressionEvent e = base(ProgressionAction.LEVEL_CHANGE, event.getPlayer());
        e.setNewLevel(event.getNewLevel());
        logAndSend(e);
    }

    @EventHandler
    public void onEnchant(EnchantItemEvent event) {
        LoggableProgressionEvent e = base(ProgressionAction.ENCHANT, event.getEnchanter());
        e.setDetail(event.getItem().getType().toString());
        logAndSend(e);
    }

    @EventHandler
    public void onCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        LoggableProgressionEvent e = base(ProgressionAction.CRAFT, player);
        e.setDetail(event.getRecipe().getResult().getType().toString());
        logAndSend(e);
    }

    @EventHandler
    public void onFish(PlayerFishEvent event) {
        LoggableProgressionEvent e = base(ProgressionAction.FISH, event.getPlayer());
        e.setDetail(event.getState().toString());
        logAndSend(e);
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        LoggableProgressionEvent e = base(ProgressionAction.COMMAND, event.getPlayer());
        e.setDetail(event.getMessage());
        logAndSend(e);
    }
}
```

- [ ] **Step 3: Compile spigot**

Run: `$MVN -pl spigot -am clean compile`
Expected: BUILD SUCCESS.

- [ ] **Step 4: Commit**

```bash
git add spigot/src/main/java/com/splunk/spigot/eventloggers/ItemEventLogger.java \
        spigot/src/main/java/com/splunk/spigot/eventloggers/ProgressionEventLogger.java
git commit -m "feat(spigot): add ItemEventLogger and ProgressionEventLogger"
```

---

## Task 10: Session-detail hooks in PlayerEventLogger (spigot)

Enrich the existing player logger with teleport, gamemode change, bed enter, world change, and login session detail (UUID, IP, protocol). IP logging is gated by its own toggle.

**Files:**
- Modify: `spigot/src/main/java/com/splunk/spigot/eventloggers/PlayerEventLogger.java`

- [ ] **Step 1: Add the new imports**

At the top of `PlayerEventLogger.java`, add:

```java
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import com.splunk.sharedmc.loggable_events.LoggablePlayerEvent.PlayerEventAction;
```

- [ ] **Step 2: Enrich the login handler with session detail**

In `onPlayerConnect` (handling `PlayerLoginEvent`), after building the loggable, add UUID/protocol always and IP only when enabled. Replace the body of `onPlayerConnect`:

```java
@EventHandler
public void onPlayerConnect(PlayerLoginEvent event) {
    LoggablePlayerEvent loggable =
            generateLoggablePlayerEvent(event, PlayerEventAction.PLAYER_CONNECT, null, event.getKickMessage());
    loggable.setPlayerUuid(event.getPlayer().getUniqueId().toString());
    loggable.setProtocolVersion(event.getPlayer().getProtocolVersion());
    if (isEnabled(ENABLE_SESSION_IP) && event.getAddress() != null) {
        loggable.setPlayerIp(event.getAddress().getHostAddress());
    }
    logAndSend(loggable);
}
```

Note: `Player.getProtocolVersion()` is a Paper API. If the pinned `spigot-api` does not expose it, the compile in Step 4 fails; fall back to dropping the protocol line (it is optional enrichment).

- [ ] **Step 3: Add the new session-detail handlers**

Add these methods to `PlayerEventLogger`:

```java
@EventHandler
public void onTeleport(PlayerTeleportEvent event) {
    LoggablePlayerEvent loggable = generateLoggablePlayerEvent(
            event, PlayerEventAction.TELEPORT, event.getCause().toString(), null);
    loggable.setFrom(locationAsPoint(event.getFrom()));
    loggable.setTo(locationAsPoint(event.getTo()));
    logAndSend(loggable);
}

@EventHandler
public void onGameModeChange(PlayerGameModeChangeEvent event) {
    LoggablePlayerEvent loggable = generateLoggablePlayerEvent(
            event, PlayerEventAction.GAMEMODE_CHANGE, null, null);
    loggable.setGamemode(event.getNewGameMode().toString());
    logAndSend(loggable);
}

@EventHandler
public void onBedEnter(PlayerBedEnterEvent event) {
    LoggablePlayerEvent loggable = generateLoggablePlayerEvent(
            event, PlayerEventAction.BED_ENTER, null, null);
    logAndSend(loggable);
}

@EventHandler
public void onWorldChange(PlayerChangedWorldEvent event) {
    LoggablePlayerEvent loggable = generateLoggablePlayerEvent(
            event, PlayerEventAction.WORLD_CHANGE, null, event.getFrom().getName());
    logAndSend(loggable);
}
```

Note: `generateLoggablePlayerEvent` takes a `PlayerEvent`. `PlayerGameModeChangeEvent`, `PlayerBedEnterEvent`, `PlayerChangedWorldEvent`, and `PlayerTeleportEvent` all extend `PlayerEvent`, so they pass directly.

- [ ] **Step 4: Compile spigot**

Run: `$MVN -pl spigot -am clean compile`
Expected: BUILD SUCCESS. Resolve any API-availability failures per the notes above.

- [ ] **Step 5: Commit**

```bash
git add spigot/src/main/java/com/splunk/spigot/eventloggers/PlayerEventLogger.java
git commit -m "feat(spigot): add teleport/gamemode/bed/world-change and login session detail"
```

---

## Task 11: ScheduledMetricLogger base + ServerEventLogger + PerformanceSampler (spigot)

Non-event metrics need a scheduler. Add a thin base and two concrete users.

**Files:**
- Create: `spigot/src/main/java/com/splunk/spigot/scheduling/ScheduledMetricLogger.java`
- Create: `spigot/src/main/java/com/splunk/spigot/eventloggers/PerformanceSampler.java`
- Create: `spigot/src/main/java/com/splunk/spigot/eventloggers/ServerEventLogger.java`

- [ ] **Step 1: Implement the scheduler base**

```java
package com.splunk.spigot.scheduling;

import java.util.Properties;

import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import com.splunk.sharedmc.event_loggers.AbstractEventLogger;

/**
 * Base for loggers driven by the Bukkit scheduler rather than the event bus.
 * Subclasses implement {@link #sample()}; {@link #start(Plugin, long)} schedules it.
 */
public abstract class ScheduledMetricLogger extends AbstractEventLogger {

    public ScheduledMetricLogger(Properties props) {
        super(props);
    }

    /** Called on each scheduled tick. Build and send the metric event(s) here. */
    protected abstract void sample();

    /** Schedules {@link #sample()} every {@code intervalTicks} ticks. */
    public void start(Plugin plugin, long intervalTicks) {
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    sample();
                } catch (Exception e) {
                    logger.warn("Scheduled metric sample failed", e);
                }
            }
        }.runTaskTimer(plugin, intervalTicks, intervalTicks);
    }
}
```

- [ ] **Step 2: Implement PerformanceSampler**

```java
package com.splunk.spigot.eventloggers;

import java.util.Properties;

import org.bukkit.Bukkit;

import com.splunk.sharedmc.loggable_events.LoggablePerformanceEvent;
import com.splunk.spigot.scheduling.ScheduledMetricLogger;

/**
 * Periodically samples server performance (TPS, MSPT, online players, loaded chunks).
 */
public class PerformanceSampler extends ScheduledMetricLogger {

    public PerformanceSampler(Properties props) {
        super(props);
    }

    @Override
    protected void sample() {
        LoggablePerformanceEvent e = new LoggablePerformanceEvent(0L);
        // Bukkit.getTPS() returns [1m, 5m, 15m] averages (Paper/Spigot).
        double[] tps = Bukkit.getTPS();
        if (tps.length > 0) {
            e.setTps(tps[0]);
        }
        e.setMspt(Bukkit.getAverageTickTime());
        e.setOnlinePlayers(Bukkit.getOnlinePlayers().size());
        int loaded = 0;
        for (org.bukkit.World w : Bukkit.getWorlds()) {
            loaded += w.getLoadedChunks().length;
        }
        e.setLoadedChunks(loaded);
        logAndSend(e);
    }
}
```

Note: `Bukkit.getTPS()` and `Bukkit.getAverageTickTime()` are Paper APIs (also present on modern Spigot). If unavailable on the pinned API, the compile in Step 4 fails — fall back to omitting TPS/MSPT and sampling only player/chunk counts.

- [ ] **Step 3: Implement ServerEventLogger**

```java
package com.splunk.spigot.eventloggers;

import java.util.Properties;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.event.weather.WeatherChangeEvent;

import com.splunk.sharedmc.event_loggers.AbstractEventLogger;
import com.splunk.sharedmc.loggable_events.LoggableServerEvent;
import com.splunk.sharedmc.loggable_events.LoggableServerEvent.ServerAction;

/**
 * Logs server lifecycle and world-state events.
 */
public class ServerEventLogger extends AbstractEventLogger implements Listener {

    public ServerEventLogger(Properties props) {
        super(props);
    }

    @EventHandler
    public void onServerLoad(ServerLoadEvent event) {
        LoggableServerEvent e = new LoggableServerEvent(ServerAction.SERVER_START, 0L, null);
        e.setMotd(event.getType().toString());
        logAndSend(e);
    }

    @EventHandler
    public void onWeatherChange(WeatherChangeEvent event) {
        LoggableServerEvent e = new LoggableServerEvent(
                ServerAction.WEATHER_CHANGE, event.getWorld().getTime(), event.getWorld().getName());
        e.setWeather(event.toWeatherState() ? "storm" : "clear");
        logAndSend(e);
    }
}
```

- [ ] **Step 4: Compile spigot**

Run: `$MVN -pl spigot -am clean compile`
Expected: BUILD SUCCESS. Resolve API-availability failures per the notes.

- [ ] **Step 5: Commit**

```bash
git add spigot/src/main/java/com/splunk/spigot/scheduling/ScheduledMetricLogger.java \
        spigot/src/main/java/com/splunk/spigot/eventloggers/PerformanceSampler.java \
        spigot/src/main/java/com/splunk/spigot/eventloggers/ServerEventLogger.java
git commit -m "feat(spigot): add ScheduledMetricLogger base, PerformanceSampler, ServerEventLogger"
```

---

## Task 12: Wire everything into LogToSplunkPlugin with toggles

**Files:**
- Modify: `spigot/src/main/java/com/splunk/spigot/LogToSplunkPlugin.java`

- [ ] **Step 1: Register the new loggers gated by toggles**

In `LogToSplunkPlugin.onEnable`, after the existing three `registerEvents` calls (line 48), add:

```java
final org.bukkit.plugin.PluginManager pm = getServer().getPluginManager();

// Existing always-on registrations remain above. New categories are opt-in.
final java.util.Properties p = properties;

if (Boolean.parseBoolean(p.getProperty("splunk.craft.enable.combat", "false"))) {
    pm.registerEvents(new com.splunk.spigot.eventloggers.CombatEventLogger(p), this);
}
if (Boolean.parseBoolean(p.getProperty("splunk.craft.enable.item", "false"))) {
    pm.registerEvents(new com.splunk.spigot.eventloggers.ItemEventLogger(p), this);
}
if (Boolean.parseBoolean(p.getProperty("splunk.craft.enable.progression", "false"))) {
    pm.registerEvents(new com.splunk.spigot.eventloggers.ProgressionEventLogger(p), this);
}
if (Boolean.parseBoolean(p.getProperty("splunk.craft.enable.server", "false"))) {
    pm.registerEvents(new com.splunk.spigot.eventloggers.ServerEventLogger(p), this);
}
if (Boolean.parseBoolean(p.getProperty("splunk.craft.enable.performance", "false"))) {
    int interval = 600;
    try {
        interval = Integer.parseInt(p.getProperty("splunk.craft.performance.interval_ticks", "600"));
    } catch (NumberFormatException ignored) { }
    new com.splunk.spigot.eventloggers.PerformanceSampler(p).start(this, interval);
}
```

(The existing `PlayerEventLogger` registration stays; its new session-detail handlers come along automatically. Session-detail and the IP sub-toggle are read inside `PlayerEventLogger` via `isEnabled`.)

- [ ] **Step 2: Full reactor build incl. shaded plugin**

Run: `$MVN clean package`
Expected: BUILD SUCCESS; shaded plugin jar under `logtosplunk-plugin/target/`.

- [ ] **Step 3: Commit**

```bash
git add spigot/src/main/java/com/splunk/spigot/LogToSplunkPlugin.java
git commit -m "feat(spigot): register new event loggers gated by per-category toggles"
```

---

## Task 13: Document the new config toggles

**Files:**
- Modify: `sampleModConfig/splunk.properties` (if it exists; otherwise create a documented sample there)

- [ ] **Step 1: Locate the sample config**

Run: `find /home/splunk/appDev/minecraft-app/sampleModConfig -name 'splunk.properties'`
If none exists, create `sampleModConfig/splunk.properties`.

- [ ] **Step 2: Append the documented toggles**

Add (keeping any existing keys):

```properties
# --- Extended logging categories (all opt-in; default off) ---
# Combat & survival: damage, kills, healing, hunger, respawn (throttled per player)
splunk.craft.enable.combat=false
# Item economy: pickup, drop
splunk.craft.enable.item=false
# Progression: xp, level, enchant, craft, fish, command
splunk.craft.enable.progression=false
# Server lifecycle: start, weather change
splunk.craft.enable.server=false
# Performance sampler: TPS, MSPT, online players, loaded chunks
splunk.craft.enable.performance=false
# How often (in server ticks, 20 ticks = 1s) to sample performance
splunk.craft.performance.interval_ticks=600
# Session detail: log client IP on connect (PII — opt in deliberately)
splunk.craft.enable.session_ip=false
```

- [ ] **Step 3: Commit**

```bash
git add sampleModConfig/splunk.properties
git commit -m "docs: document extended-logging config toggles in sample splunk.properties"
```

---

## Task 14: Manual smoke test against a local Paper server

**Files:** none (verification)

- [ ] **Step 1: Build the plugin jar**

Run: `$MVN clean package` → confirm `logtosplunk-plugin/target/*.jar`.

- [ ] **Step 2: Deploy to a local Paper 1.21.10 server**

Copy the shaded jar into the test server's `plugins/`, set `config/splunk.properties` with a valid HEC token and **all new toggles enabled**, start the server on Java 21.

- [ ] **Step 3: Exercise each category and confirm Splunk receives events**

In-game / console: take damage, eat, pick up and drop an item, gain XP / level up, run a command, change gamemode, sleep in a bed, change worlds, trigger weather. Wait for a performance sample interval. In Splunk, confirm events arrive for `CombatEvent`, `ItemEvent`, `ProgressionEvent`, `PlayerEvent` (teleport/gamemode/bed/world), `ServerEvent`, `PerformanceEvent`.

- [ ] **Step 4: Confirm throttling and opt-out**

Verify rapid damage does not produce one event per tick (throttled to ~1/s), and that with a category toggle set to `false` no events of that type are emitted.

- [ ] **Step 5: Record the smoke result in the PR description.**

---

## Done criteria

- All `shared-mc` unit tests green (`$MVN -pl shared-mc -am test`).
- `$MVN clean package` green; shaded plugin jar built.
- Each new category logs to Splunk when enabled, is silent when disabled, and high-volume events are throttled.
- IP logging only occurs with `splunk.craft.enable.session_ip=true`.

## Self-review notes (addressed)

- **Spec coverage:** all four requested categories (combat/survival, economy/progression, session detail, server lifecycle/performance) map to Tasks 2–5 (data) and 8–12 (wiring). Toggles (Task 7/12/13), throttling (Task 6 + listener use), PII/IP gate (Task 10/13) covered.
- **NPE gotcha:** location-less constructor added in Task 1 before any location-less event (server/performance) is built.
- **Type consistency:** `EventThrottle.allow(String)`, `LoggableEventType` constants, action enums, and setter names are referenced identically across data tasks and listener tasks.
- **API-availability risk:** Paper-specific calls (`getTPS`, `getAverageTickTime`, `getProtocolVersion`) are flagged at each compile gate with a documented fallback, matching the spec's "open risks".

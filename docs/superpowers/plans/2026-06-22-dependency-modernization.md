# Dependency Modernization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Land a modern, CVE-scanned, reproducible dependency baseline targeting Java 21, with the `forge` module excluded from the reactor.

**Architecture:** A Maven multi-module reactor (`splunk.minecraft.app` parent → `shared-mc`, `spigot`, `logtosplunk-plugin`; `forge` removed from reactor). Changes are confined to pom files plus one Java source swap (`json-simple` → `gson`) in `shared-mc`. This plan is the foundation for the logging-extension plan, which builds on the gson change and Java 21 target.

**Tech Stack:** Maven (bundled at `mvn-bin/`), Java 21 (`/usr/lib/jvm/java-21-openjdk-amd64`), log4j 2.25.1, gson 2.13.2, httpclient5, OWASP dependency-check-maven.

---

## Build & Test Commands (use throughout)

Set once per shell:

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export MVN="./mvn-bin/bin/mvn -f /home/splunk/appDev/minecraft-app/pom.xml"
```

- Full build: `$MVN clean package`
- Single module test: `$MVN -pl shared-mc -am test`
- Dependency scan: `$MVN -pl shared-mc,spigot,logtosplunk-plugin org.owasp:dependency-check-maven:check`

All work happens on branch `feature/extend-logging-modernize-deps` (already created).

---

## File Structure

| File | Responsibility | Change |
|---|---|---|
| `pom.xml` (parent) | Reactor modules, dependencyManagement, compiler config | Modify: drop forge module, Java 21, bump compiler plugin, add dependency-check, manage gson |
| `shared-mc/pom.xml` | shared lib deps | Modify: remove json-simple, drop redundant explicit log4j versions |
| `shared-mc/src/main/java/com/splunk/sharedmc/SingleSplunkConnection.java` | HEC envelope build | Modify: replace `org.json.simple.JSONObject` with gson `JsonObject` |
| `logtosplunk-plugin/pom.xml` | shaded plugin packaging | Modify: bump shade plugin, drop stale `1.0.1` from forge profile |
| `spigot/pom.xml` | spigot adapter deps | No dependency change; inherits Java 21 from parent |

---

## Task 1: Capture a CVE baseline before changing anything

**Files:**
- Modify: `pom.xml` (add OWASP plugin to `<build><plugins>`)

- [ ] **Step 1: Add the OWASP dependency-check plugin to the parent pom**

In `pom.xml`, inside the existing `<build><plugins>` block (after the `maven-compiler-plugin`), add:

```xml
<plugin>
    <groupId>org.owasp</groupId>
    <artifactId>dependency-check-maven</artifactId>
    <version>12.1.0</version>
    <configuration>
        <!-- Fail the build on any High/Critical finding (CVSS >= 7.0) -->
        <failBuildOnCVSS>7.0</failBuildOnCVSS>
        <!-- forge is out of reactor; skip test-scope noise -->
        <skipProvidedScope>false</skipProvidedScope>
        <suppressionFiles>
            <suppressionFile>${maven.multiModuleProjectDirectory}/owasp-suppressions.xml</suppressionFile>
        </suppressionFiles>
    </configuration>
</plugin>
```

- [ ] **Step 2: Create an (initially empty) suppression file**

Create `owasp-suppressions.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<suppressions xmlns="https://jeremylong.github.io/DependencyCheck/dependency-suppression.1.3.xsd">
    <!-- Add <suppress> entries here only for verified false positives, with a justifying comment. -->
</suppressions>
```

- [ ] **Step 3: Run the scan to capture the baseline (do not fail the build yet)**

Run:
```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
./mvn-bin/bin/mvn -f pom.xml -pl shared-mc,spigot,logtosplunk-plugin -am \
  org.owasp:dependency-check-maven:12.1.0:aggregate -DfailBuildOnCVSS=11 \
  -Dformats=HTML,JSON
```
Expected: completes; writes `target/dependency-check-report.html` + `.json`. First run downloads the NVD database and may take several minutes. If NVD rate-limits, obtain a free API key from https://nvd.nist.gov/developers/request-an-api-key and pass `-DnvdApiKey=<key>`.

- [ ] **Step 4: Record findings**

Read `target/dependency-check-report.json`. List every dependency with a CVSS >= 7.0 finding in a scratch note (you will verify each is resolved by Task 6's final scan). Do not fix individual CVEs by hand — the version bumps in Tasks 2–4 address them; the final scan confirms.

- [ ] **Step 5: Commit**

```bash
git add pom.xml owasp-suppressions.xml
git commit -m "build: add OWASP dependency-check with CVSS 7.0 gate"
```

---

## Task 2: Target Java 21 and bump the compiler plugin

**Files:**
- Modify: `pom.xml:54-66` (the `<build><plugins>` compiler entry)

- [ ] **Step 1: Replace the compiler plugin block**

In `pom.xml`, replace:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <version>2.3.2</version>
    <configuration>
        <source>1.8</source>
        <target>1.8</target>
    </configuration>
</plugin>
```

with:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <version>3.14.0</version>
    <configuration>
        <release>21</release>
    </configuration>
</plugin>
```

- [ ] **Step 2: Add explicit properties so encoding + Java version are unambiguous**

In `pom.xml`, add a `<properties>` block immediately after `<packaging>pom</packaging>` (line 14):

```xml
<properties>
    <maven.compiler.release>21</maven.compiler.release>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
</properties>
```

- [ ] **Step 3: Build to verify Java 21 compiles the existing code**

Run:
```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
./mvn-bin/bin/mvn -f pom.xml -pl shared-mc,spigot,logtosplunk-plugin -am clean compile
```
Expected: BUILD SUCCESS. (forge is still in the reactor at this point; if forge fails to compile under Java 21, that is expected — it is removed in Task 5. To isolate, the `-pl` list above excludes forge.)

- [ ] **Step 4: Commit**

```bash
git add pom.xml
git commit -m "build: target Java 21 and bump maven-compiler-plugin to 3.14.0"
```

---

## Task 3: Replace json-simple with gson in SingleSplunkConnection (TDD)

`json-simple 1.1` (2009) is used in exactly one place: building the `{"event": <message>}` HEC envelope in `SingleSplunkConnection.sendToSplunk`. gson 2.13.2 is already a `shared-mc` dependency. This task swaps it under test.

**Files:**
- Test: `shared-mc/src/test/java/com/splunk/sharedmc/SingleSplunkConnectionTest.java` (create)
- Modify: `shared-mc/src/main/java/com/splunk/sharedmc/SingleSplunkConnection.java:27,94-98`
- Modify: `shared-mc/pom.xml` (remove json-simple dependency)
- Modify: `pom.xml` (remove json-simple from dependencyManagement)

- [ ] **Step 1: Extract the envelope build into a testable method**

The current `sendToSplunk` both builds the envelope and mutates `messagesToSend`. To test the JSON shape without side effects, add a package-private static helper. In `SingleSplunkConnection.java`, add this method (near `sendToSplunk`):

```java
/**
 * Wraps a raw event message in the Splunk HEC envelope: {"event": <message>}.
 * Package-private for testing.
 */
static String buildHecEnvelope(String message) {
    com.google.gson.JsonObject event = new com.google.gson.JsonObject();
    event.addProperty("event", message);
    return event.toString();
}
```

- [ ] **Step 2: Write the failing test**

Create `shared-mc/src/test/java/com/splunk/sharedmc/SingleSplunkConnectionTest.java`:

```java
package com.splunk.sharedmc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Test;

public class SingleSplunkConnectionTest {

    @Test
    public void buildHecEnvelope_wrapsMessageInEventField() {
        String out = SingleSplunkConnection.buildHecEnvelope("hello world");
        JsonObject parsed = JsonParser.parseString(out).getAsJsonObject();
        assertEquals("hello world", parsed.get("event").getAsString());
    }

    @Test
    public void buildHecEnvelope_escapesQuotes() {
        String out = SingleSplunkConnection.buildHecEnvelope("a \"quoted\" value");
        // Must be valid JSON and round-trip the exact string.
        JsonObject parsed = JsonParser.parseString(out).getAsJsonObject();
        assertEquals("a \"quoted\" value", parsed.get("event").getAsString());
        assertTrue(out.contains("\\\""));
    }
}
```

- [ ] **Step 3: Run the test to verify it fails**

Run:
```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
./mvn-bin/bin/mvn -f pom.xml -pl shared-mc -am test -Dtest=SingleSplunkConnectionTest
```
Expected: FAIL — `buildHecEnvelope` does not yet exist OR (after Step 1) PASS for the helper but the production `sendToSplunk` still imports json-simple. If Step 1 already added the helper, this step confirms the helper passes; proceed to wire it into `sendToSplunk`.

- [ ] **Step 4: Rewrite `sendToSplunk` to use the helper and remove the json-simple import**

In `SingleSplunkConnection.java`, delete line 27 `import org.json.simple.JSONObject;`. Replace the body of `sendToSplunk` (lines 93-99):

```java
@Override
public void sendToSplunk(String message) {
    messagesToSend.append(buildHecEnvelope(message));
}
```

- [ ] **Step 5: Remove json-simple from shared-mc/pom.xml**

Delete the dependency block (`shared-mc/pom.xml:60-64`):

```xml
<dependency>
    <groupId>com.googlecode.json-simple</groupId>
    <artifactId>json-simple</artifactId>
    <version>1.1</version>
</dependency>
```

- [ ] **Step 6: Remove json-simple from the parent dependencyManagement**

Delete the block in `pom.xml:47-51`:

```xml
<dependency>
    <groupId>com.googlecode.json-simple</groupId>
    <artifactId>json-simple</artifactId>
    <version>1.1</version>
</dependency>
```

- [ ] **Step 7: Run tests and a full shared-mc build to verify**

Run:
```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
./mvn-bin/bin/mvn -f pom.xml -pl shared-mc -am clean test
```
Expected: BUILD SUCCESS, `SingleSplunkConnectionTest` PASS, no `org.json.simple` on the classpath.

- [ ] **Step 8: Verify json-simple is fully gone**

Run:
```bash
grep -rn "json.simple\|JSONObject\|JSONValue" /home/splunk/appDev/minecraft-app/shared-mc/src
```
Expected: no matches.

- [ ] **Step 9: Commit**

```bash
git add shared-mc/src pom.xml shared-mc/pom.xml
git commit -m "refactor: replace json-simple with gson for HEC envelope"
```

---

## Task 4: Centralize versions and remove redundant explicit versions

shared-mc redeclares `log4j-api`/`log4j-core` versions already managed by the parent. Centralize so a single bump propagates.

**Files:**
- Modify: `pom.xml` (add gson + httpclient5/httpcore5 + guava to dependencyManagement)
- Modify: `shared-mc/pom.xml` (drop explicit `<version>` on log4j entries)

- [ ] **Step 1: Add managed versions to the parent dependencyManagement**

In `pom.xml`, inside `<dependencyManagement><dependencies>`, add (alongside the existing log4j/splunk entries):

```xml
<dependency>
    <groupId>com.google.code.gson</groupId>
    <artifactId>gson</artifactId>
    <version>2.13.2</version>
</dependency>
<dependency>
    <groupId>com.google.guava</groupId>
    <artifactId>guava</artifactId>
    <version>33.5.0-jre</version>
</dependency>
<dependency>
    <groupId>org.apache.httpcomponents.core5</groupId>
    <artifactId>httpcore5</artifactId>
    <version>5.3.6</version>
</dependency>
<dependency>
    <groupId>org.apache.httpcomponents.client5</groupId>
    <artifactId>httpclient5</artifactId>
    <version>5.5.1</version>
</dependency>
```

- [ ] **Step 2: Bump junit to 4.13.2 in the parent dependencyManagement**

In `pom.xml`, change the junit version from `4.8.2` to `4.13.2`:

```xml
<dependency>
    <groupId>junit</groupId>
    <artifactId>junit</artifactId>
    <version>4.13.2</version>
    <scope>test</scope>
</dependency>
```

- [ ] **Step 3: Remove explicit `<version>` from shared-mc dependencies now managed by parent**

In `shared-mc/pom.xml`, delete the `<version>` lines from `log4j-api`, `log4j-core`, `httpcore5`, `httpclient5`, `guava`, `gson`, and `splunk-library-javalogging` so they inherit from the parent. Example for log4j-api:

```xml
<dependency>
    <groupId>org.apache.logging.log4j</groupId>
    <artifactId>log4j-api</artifactId>
</dependency>
```

(For httpcore5/httpclient5/guava/gson, the parent now manages them via Step 1; leave their `<dependency>` entries but drop the `<version>` child element.)

- [ ] **Step 4: Build to confirm versions still resolve**

Run:
```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
./mvn-bin/bin/mvn -f pom.xml -pl shared-mc -am clean test
```
Expected: BUILD SUCCESS, tests still PASS.

- [ ] **Step 5: Verify resolved versions are unchanged**

Run:
```bash
./mvn-bin/bin/mvn -f pom.xml -pl shared-mc dependency:tree | grep -E "log4j|guava|gson|httpc|junit"
```
Expected: log4j 2.25.1, guava 33.5.0-jre, gson 2.13.2, httpcore5 5.3.6, httpclient5 5.5.1, junit 4.13.2.

- [ ] **Step 6: Commit**

```bash
git add pom.xml shared-mc/pom.xml
git commit -m "build: centralize dependency versions in parent, bump junit to 4.13.2"
```

---

## Task 5: Exclude forge from the reactor and clean the plugin shade config

**Files:**
- Modify: `pom.xml:8-13` (modules)
- Modify: `logtosplunk-plugin/pom.xml:65` (shade version) and `:96-100` (stale 1.0.1 in forge profile)

- [ ] **Step 1: Remove the forge module from the reactor**

In `pom.xml`, change the `<modules>` block to:

```xml
<modules>
    <module>spigot</module>
    <module>shared-mc</module>
    <module>logtosplunk-plugin</module>
</modules>
```

(Leave the `forge/` directory and its pom on disk untouched.)

- [ ] **Step 2: Bump the shade plugin in the plugin pom**

In `logtosplunk-plugin/pom.xml`, change the `maven-shade-plugin` version from `2.4.1` to `3.6.0`.

- [ ] **Step 3: Remove the stale 1.0.1 splunk lib from the include-forge profile**

In `logtosplunk-plugin/pom.xml`, delete the dependency block at lines 96-100 (the `splunk-library-javalogging` `1.0.1` entry inside the `include-forge` profile). The `include-forge` profile is now dead (forge is out of the reactor); leaving the stale version invites confusion.

- [ ] **Step 4: Full reactor build**

Run:
```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
./mvn-bin/bin/mvn -f pom.xml clean package
```
Expected: BUILD SUCCESS across `shared-mc`, `spigot`, `logtosplunk-plugin`; the shaded plugin jar is produced under `logtosplunk-plugin/target/`.

- [ ] **Step 5: Commit**

```bash
git add pom.xml logtosplunk-plugin/pom.xml
git commit -m "build: drop forge from reactor, bump shade plugin to 3.6.0, remove stale splunk 1.0.1"
```

---

## Task 6: Enforce the CVE gate

**Files:** none (verification + config gate only)

- [ ] **Step 1: Run the dependency-check with the failing gate enabled**

Run:
```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
./mvn-bin/bin/mvn -f pom.xml -pl shared-mc,spigot,logtosplunk-plugin -am \
  org.owasp:dependency-check-maven:12.1.0:aggregate -Dformats=HTML,JSON
```
Expected: BUILD SUCCESS with `failBuildOnCVSS=7.0` (from Task 1 config). If it FAILS, read `target/dependency-check-report.json`, identify the offending dependency, and either bump it (preferred) or, only for a verified false positive, add a documented `<suppress>` entry to `owasp-suppressions.xml` with a justification comment.

- [ ] **Step 2: Confirm every Task-1 baseline finding is resolved**

Compare against the scratch note from Task 1 Step 4. Every High/Critical must be either gone or explicitly suppressed-with-justification.

- [ ] **Step 3: Commit any suppressions or final bumps**

```bash
git add owasp-suppressions.xml pom.xml shared-mc/pom.xml logtosplunk-plugin/pom.xml
git commit -m "build: pass OWASP CVSS 7.0 gate with no unsuppressed High/Critical CVEs"
```

---

## Done criteria

- `$MVN clean package` is green with `forge` excluded.
- No `org.json.simple` anywhere in source or on the classpath.
- Java 21 release target; compiler/shade plugins modern.
- OWASP scan passes the CVSS 7.0 gate; report archived under `target/`.
- All versions for shared libs centralized in the parent pom.

## Self-review notes (addressed)

- Spec WS1 item 4 said the gson swap touches `toJson()`; verification showed `toJson()` already uses gson and json-simple lives only in `SingleSplunkConnection` — Task 3 targets the correct file.
- Spec WS1 item 3 (forge splunk lib) is intentionally NOT changed in forge's own pom (out of reactor); Task 5 instead removes the stale `1.0.1` from the plugin's dead `include-forge` profile.

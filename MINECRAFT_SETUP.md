# Splunk Minecraft Server & LogToSplunk Plugin Setup Guide

This guide describes how to install, configure, run, and verify a local Minecraft server
integrated with the `LogToSplunk` plugin/mod. See the [Architecture section of
README.md](README.md#architecture) for how the pieces fit together.

---

## 📋 Requirements

### Splunk side

1. **Splunk Enterprise**, installed and running on the host machine (or reachable over the
   network).
2. **HTTP Event Collector (HEC)** enabled and a token created: **Settings → Data Inputs → HTTP
   Event Collector**. Note the token's **index** and **port** (default `8088`) — the plugin
   sends event JSON there, sourcetype `minecraft:json`. There is no `index=` key in
   `splunk.properties`; the index is whatever the HEC token itself is bound to.
3. **(Optional) KV-store bearer token**, only if you want the player-stats feature (see below):
   **Settings → Tokens**, a token whose user has write access to the `SplunkCraft` app's
   collections. This uses the splunkd **management** port (default `8089`, HTTPS) — a different
   port and credential than HEC.
4. **The `minecraft-app` Splunk app** installed (this repo, copied into
   `$SPLUNK_HOME/etc/apps/`) and, if using player stats, the **`SplunkCraft`** app too
   (`SplunkCraft/` in this repo — ships the `minecraft_player_stats` KV collection + lookup).

### Minecraft side

1. **JDK 21+** for building (`java -version`).
2. **Maven**, for platforms built with Maven (Spigot, Paper, and the dist aggregator). `mvn-bin/`
   is expected by `install_minecraft.sh` but isn't tracked in this repo — unpack your own Maven
   distribution there, or build with a system-installed `mvn` directly.
3. **A Minecraft server matching one of the supported platform/version combinations:**

   | Platform  | MC version(s)                            | Build tool |
   |-----------|-------------------------------------------|------------|
   | Spigot    | 1.21.1 (default), 1.20.1, 1.20.4, 1.20.6   | Maven (`mvn package` / `-P mc-1201` etc.) |
   | Paper     | 1.21.1                                     | Maven |
   | Forge     | 1.20.1                                     | Gradle (`forge-1.20.1-47.4.20/gradlew`) |
   | NeoForge  | 1.20.4                                     | Gradle |
   | Fabric    | 1.20.6                                     | Gradle |

   Only Spigot, Paper, and Forge currently have the full extended-logging + KV-store feature set
   (see the platform support matrix in README.md); NeoForge/Fabric only have block/death/player
   logging.
4. **tmux** (or an equivalent terminal multiplexer), if you want the server to keep running after
   you disconnect — see "Running the server" below.

---

## ⚡ Building

`install_minecraft.sh` automates a from-scratch **Spigot/Paper** setup: builds the plugin with
Maven, downloads a PaperMC server jar, accepts the EULA, writes a generated HEC token into both
`server-config/splunk.properties` and Splunk's `inputs.conf`, and deploys the built jar into
`plugins/`.

```bash
./install_minecraft.sh
```

By default it installs to `/home/splunk/minecraft-server`; override with `MC_SERVER_DIR`:

```bash
export MC_SERVER_DIR="/path/to/custom/minecraft-server"
./install_minecraft.sh
```

For **Forge/NeoForge/Fabric**, build the platform module directly with its own Gradle wrapper
(e.g. `cd forge-1.20.1-47.4.20 && ./gradlew build`) and copy the shaded jar from `build/libs/`
into the server's `mods/` directory yourself — these platforms aren't wired into
`install_minecraft.sh` yet.

Either way, once the jar is in place, copy `logtosplunk-plugin/src/main/config/splunk.properties`
(fully commented, safe to use as a template) to `<server-dir>/config/splunk.properties` and
replace every `CHANGEME` value.

---

## 🚀 Running the server

Use `minecraft-server-ctl.sh` to start/stop/restart the server inside a detached `tmux` session,
so it keeps running after you log out:

```bash
./minecraft-server-ctl.sh start      # launch in a new tmux session
./minecraft-server-ctl.sh status     # check tmux session / process / port
./minecraft-server-ctl.sh console    # attach to watch the live console (Ctrl-b d to detach)
./minecraft-server-ctl.sh stop       # graceful stop, waits for the process to exit
./minecraft-server-ctl.sh restart    # stop, then start
```

Configure it via environment variables if your server isn't the default ATLauncher path:

```bash
export MC_SERVER_DIR="/path/to/your/server"
export MC_LAUNCH_CMD="./LaunchServer.sh"   # or ./run.sh, whatever your server uses
export MC_TMUX_SESSION="my-mc-server"
export MC_SERVER_PORT="25565"
./minecraft-server-ctl.sh start
```

> **Why not just `tmux send-keys stop` by hand?** Once the server process exits, the pane sits at
> a shell prompt ("Press any key to close..."). Sending one more keystroke into that pane to
> "finish" the stop can consume that prompt and close the pane — and if it's the tmux session's
> only window, that kills the whole session, not just the Minecraft process. `minecraft-server-ctl.sh`
> polls for the java process to actually exit and then kills the tmux session directly, without
> ever sending a second keystroke into the pane. If you're scripting your own start/stop instead
> of using this script, keep that in mind.

---

## 🔍 Verification & Manual Steps

### 1. Connecting a Minecraft Client
1. Open the Minecraft Launcher and launch the matching version for your server (see the platform
   table above).
2. Select **Multiplayer** → **Add Server**, address `localhost` (or `127.0.0.1:<port>`).
3. Connect and play — block break/place, death, and player-movement events (plus the extended
   categories, if enabled and supported on your platform) forward to Splunk in real time.

### 2. Verifying HEC Token in Splunk
```bash
cat /opt/splunk/etc/apps/splunk_httpinput/local/inputs.conf
```
You should see a stanza like:
```ini
[http://minecraft]
disabled = 0
token = <your-generated-token>
index = <your-index>
sourcetype = minecraft:json
```

### 3. Verifying Minecraft Logs
```bash
grep -i "splunk" <server-dir>/logs/latest.log
```
You should see `Splunk for Minecraft initialized.` (spigot/paper) or the equivalent Forge/mod
startup line, plus `Sending data to splunk...` as events flow.

### 4. Verifying the KV store (if enabled)
```bash
curl -sk -H "Authorization: Bearer <your-kvstore-token>" \
  "https://127.0.0.1:8089/servicesNS/nobody/SplunkCraft/storage/collections/data/minecraft_player_stats"
```
or in SPL: `| inputlookup minecraft_player_stats`

---

## 🛠️ Troubleshooting

- **Port already in use**: `./minecraft-server-ctl.sh status` shows whether something's already
  listening; find the process with `lsof -i :<port>` if it's not this server, or change
  `server-port` in `server.properties`.
- **HEC connection issues**: verify Splunk is running (`/opt/splunk/bin/splunk status`), check the
  HEC listener (`curl -i http://localhost:8088/services/collector/health`), and confirm the token
  in `<server-dir>/config/splunk.properties` matches Splunk's `inputs.conf`.
- **KV-store upsert failures**: check the server log for `KV-store batch_save failed` — usually a
  wrong bearer token, wrong management port (`8089`, not `8088`), or the `SplunkCraft` app not
  installed/reloaded (`POST /services/apps/local/_reload`).
- **Events flowing to HEC but a whole category (e.g. combat) never appears**: check the platform
  support matrix in README.md — NeoForge and Fabric don't implement the extended logging
  categories yet, regardless of config toggles.

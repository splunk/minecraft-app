package com.splunk.forge.eventloggers;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.splunk.forge.scheduling.ScheduledMetricLogger;
import com.splunk.sharedmc.KvStoreConnection;
import com.splunk.sharedmc.event_loggers.AbstractEventLogger;

/**
 * Periodically scrapes the per-player snapshot files Minecraft writes to disk
 * ({@code <world>/stats/<uuid>.json} and {@code <world>/advancements/<uuid>.json}), flattens
 * them into one document per player, and upserts those documents into a Splunk KV-store
 * collection via {@link KvStoreConnection}.
 *
 * <p>Identical to the spigot module's {@code PlayerStatsScraper} (this class never touched the
 * Bukkit API to begin with — only file I/O, gson, and the shared KV-store client), copied here
 * because Forge cannot depend on the spigot module's Bukkit-based scheduler.
 *
 * <p>Runs off the main server thread (see {@link ScheduledMetricLogger#startAsync}); it only
 * reads files and does HTTP, never touches live world state. To avoid re-uploading unchanged
 * players every cycle, the last-seen file modification time per UUID is cached and only changed
 * files are pushed.
 */
public class PlayerStatsScraper extends ScheduledMetricLogger {

    /** Default world directory name; overridable via {@link AbstractEventLogger#WORLD_PATH}. */
    private static final String DEFAULT_WORLD = "world";

    /** Custom-stat keys we surface as first-class columns (the rest are kept as category totals). */
    private static final String[] CUSTOM_STAT_COLUMNS = {
            "minecraft:deaths",
            "minecraft:mob_kills",
            "minecraft:player_kills",
            "minecraft:play_time",
            "minecraft:total_world_time",
            "minecraft:walk_one_cm",
            "minecraft:sprint_one_cm",
            "minecraft:jump",
            "minecraft:damage_dealt",
            "minecraft:damage_taken",
            "minecraft:time_since_rest"
    };

    private final KvStoreConnection kv;
    private final File statsDir;
    private final File advancementsDir;
    private final File usercacheFile;

    /** UUID -> last stats-file lastModified() we successfully uploaded, to push only deltas. */
    private final Map<String, Long> uploadedMtimes = new HashMap<>();

    public PlayerStatsScraper(Properties props) {
        super(props);

        String host = props.getProperty(KVSTORE_HOST, "127.0.0.1");
        int port = intProp(KVSTORE_PORT, 8089);
        String app = props.getProperty(KVSTORE_APP, "minecraft-app");
        String collection = props.getProperty(KVSTORE_COLLECTION, "minecraft_player_stats");
        String token = props.getProperty(KVSTORE_TOKEN);
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("Property `" + KVSTORE_TOKEN + "` must be set (a splunkd "
                    + "bearer token) to write player stats to the KV store.");
        }
        this.kv = new KvStoreConnection(host, port, app, collection, token);

        String serverDir = System.getProperty("user.dir");
        String world = props.getProperty(WORLD_PATH, DEFAULT_WORLD);
        File worldDir = new File(world);
        if (!worldDir.isAbsolute()) {
            worldDir = new File(serverDir, world);
        }
        this.statsDir = new File(worldDir, "stats");
        this.advancementsDir = new File(worldDir, "advancements");
        this.usercacheFile = new File(serverDir, "usercache.json");
    }

    @Override
    protected void sample() {
        File[] statFiles = statsDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (statFiles == null || statFiles.length == 0) {
            logger.debug("No player stats files found at {}", statsDir.getAbsolutePath());
            return;
        }

        Map<String, String> uuidToName = loadUsercache();

        JsonArray batch = new JsonArray();
        for (File statFile : statFiles) {
            String uuid = stripExtension(statFile.getName());
            long mtime = statFile.lastModified();

            File advFile = new File(advancementsDir, uuid + ".json");
            long advMtime = advFile.exists() ? advFile.lastModified() : 0L;
            long combinedMtime = Math.max(mtime, advMtime);

            Long lastSeen = uploadedMtimes.get(uuid);
            if (lastSeen != null && lastSeen == combinedMtime) {
                continue; // unchanged since last successful upload
            }

            JsonObject doc = buildDocument(uuid, uuidToName.get(uuid), statFile, advFile, combinedMtime);
            if (doc != null) {
                batch.add(doc);
            }
        }

        if (batch.size() == 0) {
            return;
        }

        if (kv.batchSave(batch.toString())) {
            // Only mark as uploaded once splunkd accepted the batch.
            for (JsonElement el : batch) {
                JsonObject doc = el.getAsJsonObject();
                uploadedMtimes.put(doc.get("uuid").getAsString(), doc.get("last_modified").getAsLong());
            }
            logger.info("Upserted {} player-stats document(s) to KV store.", batch.size());
        } else {
            logger.warn("KV-store upsert failed; will retry {} player(s) next cycle.", batch.size());
        }
    }

    private JsonObject buildDocument(String uuid, String name, File statFile, File advFile, long combinedMtime) {
        try (Reader r = new FileReader(statFile)) {
            JsonObject root = JsonParser.parseReader(r).getAsJsonObject();

            JsonObject doc = new JsonObject();
            doc.addProperty("_key", uuid);      // makes batch_save an idempotent upsert
            doc.addProperty("uuid", uuid);
            if (name != null) {
                doc.addProperty("name", name);
            }
            doc.addProperty("last_modified", combinedMtime);
            if (root.has("DataVersion")) {
                doc.addProperty("data_version", root.get("DataVersion").getAsInt());
            }

            JsonObject stats = root.has("stats") ? root.getAsJsonObject("stats") : new JsonObject();

            // Per-category totals (sum of all entries within minecraft:mined, :killed, etc.).
            for (Map.Entry<String, JsonElement> cat : stats.entrySet()) {
                String column = "stat_" + simpleKey(cat.getKey()) + "_total";
                doc.addProperty(column, sumValues(cat.getValue().getAsJsonObject()));
            }

            // Selected custom stats surfaced as their own columns.
            if (stats.has("minecraft:custom")) {
                JsonObject custom = stats.getAsJsonObject("minecraft:custom");
                for (String key : CUSTOM_STAT_COLUMNS) {
                    if (custom.has(key)) {
                        doc.addProperty(simpleKey(key), custom.get(key).getAsLong());
                    }
                }
            }

            addAdvancementCounts(doc, advFile);
            return doc;
        } catch (IOException | RuntimeException e) {
            logger.warn("Failed to parse stats for player {} ({})", uuid, statFile.getName(), e);
            return null;
        }
    }

    /**
     * Counts completed advancements, excluding recipe unlocks ({@code minecraft:recipes/...})
     * which are noise. Adds {@code advancements_completed} and {@code advancements_total}.
     */
    private void addAdvancementCounts(JsonObject doc, File advFile) {
        if (!advFile.exists()) {
            return;
        }
        try (Reader r = new FileReader(advFile)) {
            JsonObject root = JsonParser.parseReader(r).getAsJsonObject();
            int completed = 0;
            int total = 0;
            for (Map.Entry<String, JsonElement> e : root.entrySet()) {
                String id = e.getKey();
                if ("DataVersion".equals(id) || id.startsWith("minecraft:recipes/")) {
                    continue;
                }
                if (!e.getValue().isJsonObject()) {
                    continue;
                }
                total++;
                JsonObject adv = e.getValue().getAsJsonObject();
                if (adv.has("done") && adv.get("done").getAsBoolean()) {
                    completed++;
                }
            }
            doc.addProperty("advancements_completed", completed);
            doc.addProperty("advancements_total", total);
        } catch (IOException | RuntimeException e) {
            logger.debug("Failed to parse advancements file {}", advFile.getName(), e);
        }
    }

    /** Sums every numeric value in a stat category object (e.g. all blocks under minecraft:mined). */
    private static long sumValues(JsonObject obj) {
        long sum = 0L;
        for (Map.Entry<String, JsonElement> e : obj.entrySet()) {
            sum += e.getValue().getAsLong();
        }
        return sum;
    }

    /** Builds a flat UUID -> name map from the server's usercache.json (best-effort). */
    private Map<String, String> loadUsercache() {
        Map<String, String> map = new HashMap<>();
        if (!usercacheFile.exists()) {
            return map;
        }
        try (Reader r = new FileReader(usercacheFile)) {
            JsonArray arr = JsonParser.parseReader(r).getAsJsonArray();
            for (JsonElement el : arr) {
                JsonObject o = el.getAsJsonObject();
                if (o.has("uuid") && o.has("name")) {
                    map.put(o.get("uuid").getAsString(), o.get("name").getAsString());
                }
            }
        } catch (IOException | RuntimeException e) {
            logger.debug("Could not read usercache.json for player names", e);
        }
        return map;
    }

    /** "minecraft:mined" -> "mined"; "minecraft:play_time" -> "play_time". */
    private static String simpleKey(String namespacedKey) {
        int idx = namespacedKey.indexOf(':');
        return idx >= 0 ? namespacedKey.substring(idx + 1) : namespacedKey;
    }

    private static String stripExtension(String fileName) {
        int idx = fileName.lastIndexOf('.');
        return idx >= 0 ? fileName.substring(0, idx) : fileName;
    }
}

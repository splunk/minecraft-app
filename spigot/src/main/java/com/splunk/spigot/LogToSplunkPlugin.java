package com.splunk.spigot;

import java.io.File;
import java.io.FileReader;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.Location;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import com.splunk.sharedmc.Point3dLong;
import com.splunk.spigot.eventloggers.BlockEventLogger;
import com.splunk.spigot.eventloggers.DeathEventLogger;
import com.splunk.spigot.eventloggers.PlayerEventLogger;

public class LogToSplunkPlugin extends JavaPlugin implements Listener {
    public static final String MODID = "logtosplunk";
    public static final String VERSION = "1.0-SNAPSHOT";
    public static final String NAME = "Splunk for Minecraft";
    public static final String SPLUNK_PROPERTIES = "/config/splunk.properties";

    protected Properties properties;

    private static final Logger logger = LogManager.getLogger(LogToSplunkPlugin.class.getName());

    /**
     * Called when the mod is initialized.
     */
    @Override
    public void onEnable() {
        // Could probably move this to the AbstractEventLogger in shared
        properties = new Properties();
        final String path = System.getProperty("user.dir") + SPLUNK_PROPERTIES;
        try (final FileReader reader = new FileReader(new File(path))) {

            properties.load(reader);
        } catch (final Exception e) {
            logger.warn(
                    String.format(
                            "Unable to load properties for LogToSplunkMod at %s! Default values will be used.", path),
                    e);
        }

        getServer().getPluginManager().registerEvents(new BlockEventLogger(properties), this);
        getServer().getPluginManager().registerEvents(new DeathEventLogger(properties), this);
        getServer().getPluginManager().registerEvents(new PlayerEventLogger(properties), this);

        final org.bukkit.plugin.PluginManager pm = getServer().getPluginManager();
        final java.util.Properties p = properties;

        // Each extended logging category is opt-in: only register its listener/sampler if
        // the corresponding splunk.craft.enable.* toggle is set to true in config.
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
            createPerformanceSampler(p).start(this, interval);
        }
        // Player-stats scraper: reads on-disk stats/advancements JSON and upserts to the KV
        // store. Runs async (file I/O + HTTP) so it never stalls a tick.
        if (Boolean.parseBoolean(p.getProperty("splunk.craft.enable.playerstats", "false"))) {
            int interval = 6000;
            try {
                interval = Integer.parseInt(p.getProperty("splunk.craft.playerstats.interval_ticks", "6000"));
            } catch (NumberFormatException ignored) { }
            new com.splunk.spigot.eventloggers.PlayerStatsScraper(p).startAsync(this, interval);
        }

        logAndSend("Splunk for Minecraft initialized.");
    }

    /**
     * Factory for the performance sampler. Subclasses (e.g. the Paper module) override this
     * to return a platform-specific sampler that fills in TPS/MSPT from Paper-only APIs.
     */
    protected com.splunk.spigot.eventloggers.PerformanceSampler createPerformanceSampler(Properties props) {
        return new com.splunk.spigot.eventloggers.PerformanceSampler(props);
    }

    private void logAndSend(String message) {
        logger.info(message);
    }

    // nullable...
    public static Point3dLong locationAsPoint(Location location) {
        if (location == null) {
            return null;
        }
        return new Point3dLong(location.getX(), location.getY(), location.getZ());
    }
}
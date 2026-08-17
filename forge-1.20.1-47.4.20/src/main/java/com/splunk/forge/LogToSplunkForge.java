package com.splunk.forge;

import java.io.File;
import java.io.FileReader;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.splunk.forge.eventloggers.BlockEventLogger;
import com.splunk.forge.eventloggers.CombatEventLogger;
import com.splunk.forge.eventloggers.DeathEventLogger;
import com.splunk.forge.eventloggers.ItemEventLogger;
import com.splunk.forge.eventloggers.PerformanceSampler;
import com.splunk.forge.eventloggers.PlayerEventLogger;
import com.splunk.forge.eventloggers.PlayerStatsScraper;
import com.splunk.forge.eventloggers.ProgressionEventLogger;
import com.splunk.forge.eventloggers.ServerEventLogger;
import com.splunk.sharedmc.event_loggers.AbstractEventLogger;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;

/**
 * Forge entrypoint for Minecraft 1.20.1. Mirrors the spigot/fabric core: loads
 * {@code <cwd>/config/splunk.properties}, wires up the shared event loggers, and registers
 * them on the Forge event bus. Unlike Fabric, Forge exposes native block-place and chat
 * events, so no mixin is required.
 *
 * <p>The extended logging categories (combat/item/progression/server/performance) and the
 * KV-store player-stats scraper are opt-in via the same {@code splunk.craft.enable.*} toggles
 * used by the spigot module, gated the same way.
 */
@Mod(LogToSplunkForge.MODID)
public class LogToSplunkForge {
    public static final String MODID = "logtosplunk";
    public static final String SPLUNK_PROPERTIES = "/config/splunk.properties";

    private static final Logger LOGGER = LogManager.getLogger("LogToSplunk");

    public LogToSplunkForge() {
        final Properties properties = loadProperties();

        MinecraftForge.EVENT_BUS.register(new PlayerEventLogger(properties));
        MinecraftForge.EVENT_BUS.register(new BlockEventLogger(properties));
        MinecraftForge.EVENT_BUS.register(new DeathEventLogger(properties));

        if (Boolean.parseBoolean(properties.getProperty(AbstractEventLogger.ENABLE_COMBAT, "false"))) {
            MinecraftForge.EVENT_BUS.register(new CombatEventLogger(properties));
        }
        if (Boolean.parseBoolean(properties.getProperty(AbstractEventLogger.ENABLE_ITEM, "false"))) {
            MinecraftForge.EVENT_BUS.register(new ItemEventLogger(properties));
        }
        if (Boolean.parseBoolean(properties.getProperty(AbstractEventLogger.ENABLE_PROGRESSION, "false"))) {
            MinecraftForge.EVENT_BUS.register(new ProgressionEventLogger(properties));
        }
        if (Boolean.parseBoolean(properties.getProperty(AbstractEventLogger.ENABLE_SERVER, "false"))) {
            MinecraftForge.EVENT_BUS.register(new ServerEventLogger(properties));
        }
        if (Boolean.parseBoolean(properties.getProperty(AbstractEventLogger.ENABLE_PERFORMANCE, "false"))) {
            int interval = parseIntProperty(
                    properties, AbstractEventLogger.PERFORMANCE_INTERVAL_TICKS, 600);
            PerformanceSampler sampler = new PerformanceSampler(properties);
            sampler.start(interval);
            MinecraftForge.EVENT_BUS.register(sampler);
        }
        if (Boolean.parseBoolean(properties.getProperty(AbstractEventLogger.ENABLE_PLAYERSTATS, "false"))) {
            int interval = parseIntProperty(
                    properties, AbstractEventLogger.PLAYERSTATS_INTERVAL_TICKS, 6000);
            new PlayerStatsScraper(properties).startAsync(interval);
        }

        LOGGER.info("Splunk for Minecraft (Forge) initialized.");
    }

    private static int parseIntProperty(Properties properties, String key, int defaultValue) {
        try {
            return Integer.parseInt(properties.getProperty(key, Integer.toString(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private Properties loadProperties() {
        final Properties properties = new Properties();
        final String path = System.getProperty("user.dir") + SPLUNK_PROPERTIES;
        try (final FileReader reader = new FileReader(new File(path))) {
            properties.load(reader);
        } catch (final Exception e) {
            LOGGER.warn("Unable to load properties for LogToSplunk at {}! Default values will be used.", path, e);
        }
        return properties;
    }
}

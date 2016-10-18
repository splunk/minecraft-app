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
import com.splunk.spigot.eventloggers.EntityEventLogger;
import com.splunk.spigot.eventloggers.PlayerEventLogger;

public class LogToSplunkPlugin extends JavaPlugin implements Listener {
    public static final String MODID = "logtosplunk";
    public static final String VERSION = "1.1-SNAPSHOT";
    public static final String NAME = "Splunk for Minecraft";
    public static final String SPLUNK_PROPERTIES = "/config/splunk.properties";

    private Properties properties;

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
        getServer().getPluginManager().registerEvents(new EntityEventLogger(properties),this);

        logAndSend("Splunk for Minecraft initialized.");
    }

    /**
     * Logs and sends messages to be prepared for Splunk.
     *
     * @param message The message to log.
     */
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
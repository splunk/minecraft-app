package com.splunk.spigot;

import java.io.File;
import java.io.FileReader;
import java.util.Properties;

import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.splunk.sharedmc.BasicSplunkMessagePreparer;
import com.splunk.sharedmc.SplunkMessagePreparer;
import com.splunk.spigot.eventloggers.BlockEventLogger;
import com.splunk.spigot.eventloggers.DeathEventLogger;

public class LogToSplunkPlugin extends JavaPlugin implements Listener {
    public static final String MODID = "logtosplunk";
    public static final String VERSION = "0.9.0 Beta";
    public static final String NAME = "Splunk for Minecraft";
    public static final String SPLUNK_MOD_PROPERTIES = "/config/splunk_mod.properties";

    private Properties properties;
    private SplunkMessagePreparer messagePreparer;

    private static final Logger logger = LoggerFactory.getLogger(LogToSplunkPlugin.class.getName());

    /**
     * Constructor that is called by Forge. Uses the default SplunkMessagePreparer.
     */
    public LogToSplunkPlugin() {
        this(new BasicSplunkMessagePreparer());
    }

    /**
     * Constructor.
     *
     * @param splunkMessagePreparer The message preparer to use.
     */
    @VisibleForTesting
    public LogToSplunkPlugin(SplunkMessagePreparer splunkMessagePreparer) {
        this.messagePreparer = splunkMessagePreparer;
    }

    /**
     * Called when the mod is initialized.
     */
    @Override
    public void onEnable() {
        properties = new Properties();
        final String path = System.getProperty("user.dir") + SPLUNK_MOD_PROPERTIES;
        try (final FileReader reader = new FileReader(new File(path))) {

            properties.load(reader);
        } catch (final Exception e) {
            logger.warn(
                    String.format(
                            "Unable to load properties for LogToSplunkMod at %s! Default values will be used.", path),
                    e);
        }

        messagePreparer.init(properties);

        //final PlayerEventLogger playerEventLogger = new PlayerEventLogger(properties, messagePreparer);
        //final BlockEventLogger blockLogger = new BlockEventLogger(properties, messagePreparer);
        //final DeathEventLogger deathLogger = new DeathEventLogger(properties, messagePreparer);

        getServer().getPluginManager().registerEvents(new BlockEventLogger(properties, messagePreparer), this);
        getServer().getPluginManager().registerEvents(new DeathEventLogger(properties, messagePreparer), this);

        logAndSend("Splunk for Minecraft initialized.");
    }

    /**
     * Logs and sends messages to be prepared for Splunk.
     *
     * @param message The message to log.
     */
    private void logAndSend(String message) {
        logger.info(message);
        messagePreparer.writeMessage(message);
    }
}
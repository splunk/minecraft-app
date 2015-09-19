package com.splunk.forge;

import java.io.File;
import java.io.FileReader;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.annotations.VisibleForTesting;
import com.splunk.forge.event_loggers.BlockEventLogger;
import com.splunk.forge.event_loggers.DeathEventLogger;
import com.splunk.forge.event_loggers.PlayerEventLogger;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.EventBus;

@Mod(modid = LogToSplunkMod.MODID, version = LogToSplunkMod.VERSION, name = LogToSplunkMod.NAME,
        acceptableRemoteVersions = "*")
public class LogToSplunkMod {
    public static final String MODID = "logtosplunk";
    public static final String VERSION = "1.0-SNAPSHOT";
    public static final String NAME = "Splunk for Minecraft";
    public static final String LOGGER_NAME = "LogToSplunk";
    public static final String SPLUNK_MOD_PROPERTIES = "/config/splunk.properties";

    private static final Logger logger = LogManager.getLogger(LogToSplunkMod.class);
    /**
     * Used for registering listeners to FML events.
     */
    private final EventBus fmlBus;

    /**
     * Used for registering listeners to ForgeMinecraft events.
     */
    private final EventBus mcBus;

    public LogToSplunkMod() {
        this(FMLCommonHandler.instance().bus(), MinecraftForge.EVENT_BUS);
    }

    /**
     * Constructor.
     *
     * @param fmlBus EventBus to register FML event listeners on. For when you're having a bad day.
     * @param mcBus Used to register MinecraftForge event listeners. Not actually Irish.
     */
    @VisibleForTesting
    public LogToSplunkMod(EventBus fmlBus, EventBus mcBus) {
        this.fmlBus = fmlBus;
        this.mcBus = mcBus;
    }

    /**
     * Called when the mod is initialized.
     */
    @EventHandler
    @SuppressWarnings("unused")
    public void init(FMLInitializationEvent event) {
        //duplicated in the spigot module, this code could be moved to the AbstractEventLogger in shared
        final Properties properties = new Properties();
        final String path = System.getProperty("user.dir") + SPLUNK_MOD_PROPERTIES;
        try {
            final FileReader reader = new FileReader(new File(path));
            properties.load(reader);
        } catch (final Exception e) {
            logger.warn(
                    String.format(
                            "Unable to load properties for LogToSplunkMod at %s! Default values will be used.", path),
                    e);
        }

        final PlayerEventLogger playerEventLogger = new PlayerEventLogger(properties);
        fmlBus.register(playerEventLogger);
        mcBus.register(playerEventLogger);

        final BlockEventLogger blockLogger = new BlockEventLogger(properties);
        mcBus.register(blockLogger);

        final DeathEventLogger deathLogger = new DeathEventLogger(properties);
        mcBus.register(deathLogger);

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
}
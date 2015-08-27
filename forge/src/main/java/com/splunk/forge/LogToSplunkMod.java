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
import com.splunk.sharedmc.SplunkMessagePreparer;
import com.splunk.sharedmc.loggable_events.LoggableEvent;

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
    public static final String VERSION = "0.9.0 Beta";
    public static final String NAME = "Splunk for Minecraft";
    public static final String LOGGER_NAME = "LogToSplunk";
    public static final String LOG_EVENTS_TO_CONSOLE_PROP_KEY = "mod.splunk.enable.consolelog";
    public static final String SPLUNK_HOST_PROP_KEY = "mod.splunk.connection.host";
    public static final String SPLUNK_PORT_PROP_KEY = "mod.splunk.connection.port";
    public static final String DEFAULT_HOST = "localhost";
    public static final String DEFAULT_PORT = "8888";
    public static final String SPLUNK_MOD_PROPERTIES = "/config/splunk_mod.properties";

    // TODO: Add splunk appender.
    //    private static final Logger logger = LoggerFactory.getLogger(LOGGER_NAME);
    private static final Logger logger = LogManager.getLogger(LogToSplunkMod.class);
    /**
     * Used for registering listeners to FML events.
     */
    private final EventBus fmlBus;

    /**
     * Used for registering listeners to ForgeMinecraft events.
     */
    private final EventBus mcBus;

    /**
     * Used for processing messages from the various Minecraft event handlers.
     */
    private final SplunkMessagePreparer messagePreparer;

    /**
     * Constructor that is called by Forge. Uses the default SplunkMessagePreparer.
     */
    public LogToSplunkMod() {
        //TODO: This is a 'dummy' splunk message preparer that should be om
        this(
                new SplunkMessagePreparer() {
                    @Override
                    public void writeMessage(LoggableEvent loggableEvent) {

                    }

                    @Override
                    public void writeMessage(String s) {

                    }

                    @Override
                    public void init(Properties properties) {

                    }
                }, FMLCommonHandler.instance().bus(), MinecraftForge.EVENT_BUS);
    }

    /**
     * Constructor.
     *
     * @param splunkMessagePreparer The message preparer to use.
     * @param fmlBus EventBus to register FML event listeners on. For when you're having a bad day.
     * @param mcBus Used to register MinecraftForge event listeners. Not actually Irish.
     */
    @VisibleForTesting
    public LogToSplunkMod(SplunkMessagePreparer splunkMessagePreparer, EventBus fmlBus, EventBus mcBus) {
        this.messagePreparer = splunkMessagePreparer;
        this.fmlBus = fmlBus;
        this.mcBus = mcBus;
    }

    /**
     * Called when the mod is initialized.
     */
    @EventHandler
    @SuppressWarnings("unused")
    public void init(FMLInitializationEvent event) {
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

        messagePreparer.init(properties);

                final PlayerEventLogger playerEventLogger = new PlayerEventLogger(properties, messagePreparer);
                fmlBus.register(playerEventLogger);
                mcBus.register(playerEventLogger);

                final BlockEventLogger blockLogger = new BlockEventLogger(properties, messagePreparer);
                mcBus.register(blockLogger);

                final DeathEventLogger deathLogger = new DeathEventLogger(properties, messagePreparer);
                mcBus.register(deathLogger);

        logAndSend("Splunk for Minecraft initialized.");
    }

    /**
     * Logs and sends messages to be prepared for Splunk.
     *
     * @param message The message to log.
     */
    private void logAndSend(String message) {
        System.out.println(message);
        logger.info(message);
        messagePreparer.writeMessage(message);
    }
}
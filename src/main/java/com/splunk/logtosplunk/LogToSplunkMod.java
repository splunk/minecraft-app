package com.splunk.logtosplunk;

import static org.apache.logging.log4j.LogManager.getLogger;

import org.apache.logging.log4j.Logger;

import com.google.common.annotations.VisibleForTesting;
import com.splunk.logtosplunk.event_loggers.PlayerEventLogger;

import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.EventBus;

@Mod(modid = LogToSplunkMod.MODID, version = LogToSplunkMod.VERSION, name = LogToSplunkMod.NAME)
public class LogToSplunkMod {
    public static final String MODID = "logtosplunk";
    public static final String VERSION = "0.1 Alpha";
    public static final String NAME = "Splunk for Minecraft";
    public static final String LOGGER_NAME = "LogToSplunk";

    private static final Logger logger = getLogger(LOGGER_NAME);

    /**
     * Used for registering listeners to events.
     */
    private final EventBus bus;

    /**
     * Used for processing messages from the various Minecraft event handlers.
     */
    private final SplunkMessagePreparer messagePreparer;

    /**
     * Constructor that is called by Forge. Uses the default SplunkMessagePreparer.
     */
    public LogToSplunkMod() {
        this(new OriginalSplunkMessagePreparer(), new FMLCommonHandler().instance().bus());
    }

    /**
     * Constructor.
     *
     * @param splunkMessagePreparer The message preparer to use.
     * @param bus Passed in to allow testing via JUnit (problems with classloader)
     */
    @VisibleForTesting
    public LogToSplunkMod(SplunkMessagePreparer splunkMessagePreparer, EventBus bus) {
        this.messagePreparer = splunkMessagePreparer;
        this.bus = bus;
    }

    /**
     * Called when the mod is initialized.
     */
    @Mod.EventHandler
    @SuppressWarnings("unused")
    public void init(FMLInitializationEvent event) {
        bus.register(new PlayerEventLogger(messagePreparer));
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
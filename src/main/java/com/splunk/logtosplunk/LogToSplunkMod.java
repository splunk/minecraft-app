package com.splunk.logtosplunk;

import static org.apache.logging.log4j.LogManager.getLogger;

import org.apache.logging.log4j.Logger;

import com.google.common.annotations.VisibleForTesting;
import com.splunk.logtosplunk.event_loggers.BlockEventLogger;
import com.splunk.logtosplunk.event_loggers.DeathEventLogger;
import com.splunk.logtosplunk.event_loggers.PlayerEventLogger;
import com.splunk.logtosplunk.event_loggers.PlayerMovementEventLogger;

import net.minecraftforge.common.MinecraftForge;
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
        this(new OriginalSplunkMessagePreparer(), FMLCommonHandler.instance().bus(), MinecraftForge.EVENT_BUS);
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
    @Mod.EventHandler
    @SuppressWarnings("unused")
    public void init(FMLInitializationEvent event) {
        PlayerEventLogger playerEventLogger = new PlayerEventLogger(messagePreparer);
        fmlBus.register(playerEventLogger);
        mcBus.register(playerEventLogger);

        PlayerMovementEventLogger playerMovementEventLogger = new PlayerMovementEventLogger(messagePreparer);
        mcBus.register(playerMovementEventLogger);

        BlockEventLogger blockLogger = new BlockEventLogger(messagePreparer);
        mcBus.register(blockLogger);

        DeathEventLogger deathLogger = new DeathEventLogger(messagePreparer);
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
        messagePreparer.writeMessage(message);
    }
}
package com.splunk.logtosplunk;

import static org.apache.logging.log4j.LogManager.getLogger;

import org.apache.logging.log4j.Logger;

import com.google.common.annotations.VisibleForTesting;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

@Mod(modid = LogToSplunkMod.MODID, version = LogToSplunkMod.VERSION, name = LogToSplunkMod.NAME)
public class LogToSplunkMod {
    public static final String MODID = "logtosplunk";
    public static final String VERSION = "0.1 Alpha";
    public static final String NAME = "Splunk for Minecraft";
    public static final String LOGGER_NAME = "LogToSplunk";

    private static final Logger logger = getLogger(LOGGER_NAME);

    /**
     * Used for processing messages from the various Minecraft event handlers.
     */
    private final SplunkMessagePreparer messagePreparer;

    /**
     * Constructor that is called by Forge. Uses the default SplunkMessagePreparer.
     */
    public LogToSplunkMod(){
        this(new OriginalSplunkMessagePreparer());
    }

    /**
     * Constructor.
     * @param splunkMessagePreparer The message preparer to use.
     */
    @VisibleForTesting
    public LogToSplunkMod(SplunkMessagePreparer splunkMessagePreparer){
        this.messagePreparer = splunkMessagePreparer;
    }
    /**
     * Called when the mod is initialized.
     */
    @Mod.EventHandler
    @SuppressWarnings("unused")
    public void init(FMLInitializationEvent event) {
        logAndSend("Splunk for Minecraft initialized.");
    }

    private void logAndSend(String message){
        logger.info(message);
        messagePreparer.writeMessage(message);
    }
}
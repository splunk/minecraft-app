package com.splunk.logtosplunk;

import static org.apache.logging.log4j.LogManager.getLogger;

import org.apache.logging.log4j.Logger;

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
     * Called when the mod is initialized.
     */
    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
       logger.info("I wanted to be... a lumberjack!");
    }
}
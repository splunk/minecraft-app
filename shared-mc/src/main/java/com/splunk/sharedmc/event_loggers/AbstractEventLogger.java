package com.splunk.sharedmc.event_loggers;

import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.splunk.sharedmc.loggable_events.LoggableEvent;

/**
 * EventLoggers log to the Minecraft server console and send data to Splunk.
 */
public class AbstractEventLogger {
    public static final String LOGGER_NAME = "LogToSplunk";
    public static final String LOG_EVENTS_TO_CONSOLE_PROP_KEY = "mod.splunk.enable.consolelog";

    protected static final Logger logger = LogManager.getLogger(LOGGER_NAME);

    /**
     * If true, events will be logged to the server console.
     */
    private final boolean logEventsToConsole;

    public AbstractEventLogger(Properties properties) {
        logEventsToConsole = Boolean.valueOf(properties.getProperty(LOG_EVENTS_TO_CONSOLE_PROP_KEY, "false"));
    }

    /**
     * Logs via slf4j-simple and forwards the message to the message preparer.
     *
     * @param loggable The message to log.
     */
    protected void logAndSend(LoggableEvent loggable) {
        logger.info(loggable.toString());
    }
}

package com.splunk.forge.event_loggers;

import java.util.Properties;

import com.splunk.forge.LogToSplunkMod;
import com.splunk.forge.SplunkMessagePreparer;
import com.splunk.forge.loggable_events.LoggableEvent;

import sun.util.logging.resources.logging.log4j.LogManager;
import sun.util.logging.resources.logging.log4j.Logger;

/**
 * EventLoggers log to the Minecraft server console and send data to Splunk.
 */
public class AbstractEventLogger {
    private static final Logger logger = LogManager.getLogger(LogToSplunkMod.LOGGER_NAME);

    /**
     * If true, events will be logged to the server console.
     */
    private final boolean logEventsToConsole;

    /**
     * Processes and sends messages to Splunk.
     */
    private final SplunkMessagePreparer messagePreparer;

    public AbstractEventLogger(Properties properties, SplunkMessagePreparer splunkMessagePreparer) {
        this.messagePreparer = splunkMessagePreparer;
        logEventsToConsole =
                Boolean.valueOf(properties.getProperty(LogToSplunkMod.LOG_EVENTS_TO_CONSOLE_PROP_KEY, "false"));
    }

    /**
     * Logs via Log4j if enabled and forwards the message to the message preparer.
     *
     * @param loggable The message to log.
     */
    protected void logAndSend(LoggableEvent loggable) {
        if (logEventsToConsole) {
            logger.info(loggable);
        }
        messagePreparer.writeMessage(loggable);
    }
}

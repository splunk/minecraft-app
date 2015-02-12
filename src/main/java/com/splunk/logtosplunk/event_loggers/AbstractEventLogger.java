package com.splunk.logtosplunk.event_loggers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.splunk.logtosplunk.LogToSplunkMod;
import com.splunk.logtosplunk.SplunkMessagePreparer;
import com.splunk.logtosplunk.loggable_events.LoggableEvent;

/**
 * EventLoggers log to the minecraft server console and send data to Splunk.
 */
public class AbstractEventLogger {
    private static final Logger logger = LogManager.getLogger(LogToSplunkMod.LOGGER_NAME);

    /**
     * Processes and sends messages to Splunk.
     */
    protected final SplunkMessagePreparer messagePreparer;

    public AbstractEventLogger(
            SplunkMessagePreparer splunkMessagePreparer) {
        this.messagePreparer = splunkMessagePreparer;
    }

    /**
     * Logs via Log4j and forwards the messgage to the message preparer.
     *
     * @param loggable The message to log.
     */
    protected void logAndSend(LoggableEvent loggable) {
        logger.info(loggable);
        messagePreparer.writeMessage(loggable);
    }
}

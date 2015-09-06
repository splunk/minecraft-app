package com.splunk.sharedmc.event_loggers;

import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.splunk.sharedmc.SingleSplunkConnection;
import com.splunk.sharedmc.loggable_events.LoggableEvent;

/**
 * EventLoggers log to the Minecraft server console and send data to Splunk.
 */
public class AbstractEventLogger {
    public static final String LOGGER_NAME = "LogToSplunk";

    //TODO: rename these so neither mod nor plugin are in the key? (mod=forge, plugin=spigot)...
    public static final String LOG_EVENTS_TO_CONSOLE_PROP_KEY = "mod.splunk.enable.consolelog";
    public static final String SPLUNK_HOST = "mod.splunk.connection.host";
    public static final String SPLUNK_PORT = "mod.splunk.connection.port";

    protected static final Logger logger = LogManager.getLogger(LOGGER_NAME);

    private static SingleSplunkConnection connection;

    /**
     * If true, events will be logged to the server console.
     */
    private static boolean logEventsToConsole;
    private static String host;
    private static int port;

    public AbstractEventLogger(Properties properties) {
        if (connection == null) {
            logEventsToConsole = Boolean.valueOf(properties.getProperty(LOG_EVENTS_TO_CONSOLE_PROP_KEY, "true"));
            host = properties.getProperty(SPLUNK_HOST, "127.0.0.1");
            port = Integer.valueOf(properties.getProperty(SPLUNK_PORT, "8888"));
            connection = new SingleSplunkConnection(host, port, true);
        }
    }

    /**
     * Logs via slf4j-simple and forwards the message to the message preparer.
     *
     * @param loggable The message to log.
     */
    protected void logAndSend(LoggableEvent loggable) {
        String message = loggable.toString().replace("\"", "").replaceAll("\\r\\n", "");
        // TODO: reimplement...?
        // if(logEventsToConsole) {
        logger.info(message);
        //}
        connection.sendToSplunk(message);
    }
}

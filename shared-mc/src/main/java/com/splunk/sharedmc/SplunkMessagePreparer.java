package com.splunk.sharedmc;

import java.util.Properties;

import com.splunk.sharedmc.loggable_events.LoggableEvent;

/**
 * Processes messages before sending them to a Splunk instance.
 */
public interface SplunkMessagePreparer {

    /**
     * Takes a loggable event and sends it to Splunk.
     *
     * @param loggable The loggable event to send to Splunk.
     */
    void writeMessage(LoggableEvent loggable);

    /**
     * Takes a string, format/stamps it and sends it to Splunk.
     *
     * @param message The unformatted message to send to Splunk.
     */
    void writeMessage(String message);

    /**
     * Initializes this SplunkMessagePreparer with the given properties.
     *
     * @param props Properties to initialize this with.
     */
    void init(Properties props);
}

package com.splunk.logtosplunk;

import com.splunk.logtosplunk.loggable_events.LoggableEvent;

/**
 * Processes messages before sending them to a Splunk instance.
 */
public interface SplunkMessagePreparer {
    void writeMessage(LoggableEvent loggable);

    /**
     * Takes a string, format/stamps it and sends it to Splunk.
     * @param message The unformatted message to send to Splunk.
     */
    public void writeMessage(String message);
}

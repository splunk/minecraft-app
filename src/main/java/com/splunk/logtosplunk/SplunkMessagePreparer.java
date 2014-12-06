package com.splunk.logtosplunk;

/**
 * Processes messages before sending them to a Splunk instance.
 */
public interface SplunkMessagePreparer {
    /**
     * Takes a string, format/stamps it and sends it to Splunk.
     * @param message The unformatted message to send to Splunk.
     */
    public void writeMessage(String message);
}

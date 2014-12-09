package com.splunk.logtosplunk;

/**
 * Has a way of connecting to Splunk and sends information to it.
 */
public interface SplunkConnector {

    /**
     * Sends a message to Splunk(s).
     * @param message The message to send.
     */
    public void sendToSplunk(String message);
}

package com.splunk.sharedmc;

/**
 * Has a way of connecting to Splunk and sends information to it.
 */
public interface SplunkConnection {

    /**
     * Sends a message to Splunk(s).
     *
     * @param message The message to send.
     */
    public void sendToSplunk(String message);
}

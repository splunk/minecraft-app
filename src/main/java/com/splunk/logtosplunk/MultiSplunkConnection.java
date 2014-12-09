package com.splunk.logtosplunk;

/**
 * Manages multiple connections to Splunk, if desired, and forwards messages to each.
 */
public class MultiSplunkConnection implements SplunkConnector {

    @Override
    public void sendToSplunk(String message) {
    //NYI
    }
}

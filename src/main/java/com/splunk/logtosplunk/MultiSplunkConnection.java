package com.splunk.logtosplunk;

/**
 * Manages multiple connections to Splunk, if desired, and forwards messages to each.
 */
public class MultiSplunkConnection implements SplunkConnector {

    public MultiSplunkConnection(){

    }
    @Override
    public void sendToSplunk(String message) {
    //NYI
    }
}

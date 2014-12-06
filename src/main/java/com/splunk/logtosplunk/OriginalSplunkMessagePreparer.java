package com.splunk.logtosplunk;

import java.util.Calendar;

public class OriginalSplunkMessagePreparer implements SplunkMessagePreparer {

    /**
     * Connection(s) to Splunk.
     */
    private final SplunkConnector connector;

    /**
     * Constructor, uses default splunk connection.
     */
    public OriginalSplunkMessagePreparer(){
        this(new MultiSplunkConnection());
    }

    /**
     * Constructor.
     * @param splunkConnector Connection to splunk to use.
     */
    public OriginalSplunkMessagePreparer(SplunkConnector splunkConnector){
        this.connector = splunkConnector;
    }

    @Override
    public void writeMessage(String message) {
        String stampedMsg = Calendar.getInstance().getTime().toString() + " " + message + "\r\n\r\n";
        connector.sendToSplunk(stampedMsg);
    }
}

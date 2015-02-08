package com.splunk.logtosplunk;

/**
 * Standard spy.  Returns input values from getters.
 */
public class SplunkConnectionSpy implements SplunkConnection {

    private String message;

    @Override
    public void sendToSplunk(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}

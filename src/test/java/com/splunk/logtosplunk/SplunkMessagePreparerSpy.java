package com.splunk.logtosplunk;

/**
 * Standard testing spy, makes available input sent to it and does nothing else.
 */
public class SplunkMessagePreparerSpy implements SplunkMessagePreparer{
    String message;

    @Override
    public void writeMessage(String message) {
        this.message=message;
    }

    public String getMessage() {
        return message;
    }
}

package com.splunk.logtosplunk;

import com.splunk.logtosplunk.loggable_events.LoggableEvent;

/**
 * Standard testing spy, makes available input sent to it and does nothing else.
 */
public class SplunkMessagePreparerSpy implements SplunkMessagePreparer {
    private String message;

    private LoggableEvent loggable;

    @Override
    public void writeMessage(String message) {
        this.message = message;
    }

    @Override
    public void writeMessage(LoggableEvent loggable) {
        this.loggable = loggable;
    }

    public String getMessage() {
        return message;
    }

    public LoggableEvent getLoggable() {
        return loggable;
    }
}

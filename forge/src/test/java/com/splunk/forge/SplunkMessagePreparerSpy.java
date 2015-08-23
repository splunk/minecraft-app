package com.splunk.forge;

import java.util.Properties;

import com.splunk.sharedmc.SplunkMessagePreparer;
import com.splunk.sharedmc.loggable_events.LoggableEvent;

/**
 * Standard testing spy, makes available input sent to it and does nothing else.
 */
public class SplunkMessagePreparerSpy implements SplunkMessagePreparer {
    private String message;
    private boolean initCalled;
    private LoggableEvent loggable;

    @Override
    public void writeMessage(String message) {
        this.message = message;
    }

    @Override
    public void init(Properties props) {
        initCalled = true;
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

    public boolean isInitCalled() {
        return initCalled;
    }
}

package com.splunk.logtosplunk;

import com.splunk.logtosplunk.loggable_events.LoggableEvent;
import com.splunk.logtosplunk.loggable_events.LoggablePlayerEvent;

public class OriginalSplunkMessagePreparer implements SplunkMessagePreparer {
    static final String BASE_PLAYER_STRING = "action=%s player=%s";
    static final String REASON = " reason=%s";
    static final String MESSAGE = " message=\"%s\"";
    /**
     * Connection(s) to Splunk.
     */
    private final SplunkConnector connector;

    /**
     * Constructor, uses default Splunk connection.
     */
    public OriginalSplunkMessagePreparer() {
        this(new MultiSplunkConnection());
    }

    /**
     * Constructor.
     *
     * @param splunkConnector Connection to splunk to use.
     */
    public OriginalSplunkMessagePreparer(SplunkConnector splunkConnector) {
        this.connector = splunkConnector;
    }

    @Override
    public void writeMessage(LoggableEvent loggable) {
        if (loggable instanceof LoggablePlayerEvent) {
            writePlayerMessage((LoggablePlayerEvent) loggable);
        }
    }

    @Override
    public void writeMessage(String message) {
        connector.sendToSplunk(message);
    }

    private void writePlayerMessage(LoggablePlayerEvent event) {
        StringBuilder b = new StringBuilder(
                String.format(BASE_PLAYER_STRING, event.getAction().asString(), event.getPlayerName()));
        if (event.getReason() != null) {
            b.append(String.format(REASON, event.getReason()));
        }
        b.append(" " + extractLocation(event));
        if (event.getMessage() != null) {
            b.append(String.format(MESSAGE, event.getMessage()));
        }
        writeMessage(b.toString());
    }

    private String extractLocation(LoggableEvent event) {
        StringBuilder b = new StringBuilder();
        b.append("world=" + event.getWorldName());
        if (event.getCoordinates() != null) {
            b.append(
                    " " + "x=" + event.getCoordinates().xCoord +
                            " " + "y=" + event.getCoordinates().yCoord +
                            " " + "z=" + event.getCoordinates().zCoord);
        }
        b.append(" " + "game_time=" + event.getWorldTime());
        return b.toString();
    }
}

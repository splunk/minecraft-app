package com.splunk.sharedmc;

import java.util.Properties;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.splunk.sharedmc.loggable_events.LoggableBlockEvent;
import com.splunk.sharedmc.loggable_events.LoggableDeathEvent;
import com.splunk.sharedmc.loggable_events.LoggableEvent;
import com.splunk.sharedmc.loggable_events.LoggablePlayerEvent;
import com.splunk.sharedmc.loggable_events.LoggablePlayerEvent.PlayerEventAction;

/**
 * Based off of the original Splunk Minecraft app, this message preparer takes data from Minecraft events and prepares
 * them for Splunk.
 */
public class BasicSplunkMessagePreparer implements SplunkMessagePreparer {
    static final String BASE_PLAYER_STRING = "action=%s player=%s";
    static final String ACTION = "action=%s";
    static final String REASON = " reason=%s";
    static final String MESSAGE = " message=\"%s\"";
    static final String BLOCK = " block_type=%s";
    static final String BASE_BLOCK_TYPE = " base_type=%s";
    static final String KILLER = " killer=%s";
    static final String VICTIM = " victim=%s";
    static final String DAMAGE_SOURCE = " damage_source=%s";
    public static final String SPLUNK_HOST_PROP_KEY = "mod.splunk.connection.host";
    public static final String SPLUNK_PORT_PROP_KEY = "mod.splunk.connection.port";
    public static final String DEFAULT_HOST = "localhost";
    public static final String DEFAULT_PORT = "8888";

    /**
     * Connection(s) to Splunk.
     */
    private SplunkConnection connector;

    /**
     * Keeps track players last positions, in a guava cache for it's eviction policy.
     */
    private final Cache<String, Point3dLong> lastKnownCoordinates = CacheBuilder.newBuilder().maximumSize(
            Constants.MAX_PLAYERS).build(
            new CacheLoader<String, Point3dLong>() {
                @Override
                public Point3dLong load(String key) throws Exception {
                    return lastKnownCoordinates.getIfPresent(key);
                }
            });

    /**
     * Constructor, uses default Splunk connection which is initialized in the {@code init()} method.
     */
    public BasicSplunkMessagePreparer() {

    }

    /**
     * Constructs a new BasicSplunkMessagePreparer with the given connection.
     *
     * @param splunkConnection Connection to Splunk to use.
     */
    public BasicSplunkMessagePreparer(SplunkConnection splunkConnection) {
        this.connector = splunkConnection;
    }

    @Override
    public void writeMessage(LoggableEvent loggable) {
        if (loggable instanceof LoggablePlayerEvent) {
            writePlayerMessage((LoggablePlayerEvent) loggable);
        } else if (loggable instanceof LoggableBlockEvent) {
            writeBlockMessage((LoggableBlockEvent) loggable);
        } else if (loggable instanceof LoggableDeathEvent) {
            writeDeathMessage((LoggableDeathEvent) loggable);
        }
    }

    @Override
    public void writeMessage(String message) {
        connector.sendToSplunk(message);
    }

    @Override
    public void init(Properties props) {
        final String host = props.getProperty(SPLUNK_HOST_PROP_KEY, DEFAULT_HOST);
        final int port = Integer.valueOf(props.getProperty(SPLUNK_PORT_PROP_KEY, DEFAULT_PORT));
        connector = new SingleSplunkConnection(host, port, true);
    }

    /**
     * Processes a LoggableBlockEvent to send to Splunk.
     *
     * @param event The event to process.
     */
    private void writeBlockMessage(LoggableBlockEvent event) {
        final StringBuilder b = new StringBuilder(
                String.format(BASE_PLAYER_STRING, event.getAction().asString(), event.getPlayerName()));
        b.append(' ').append(extractLocation(event));
        b.append(String.format(BLOCK, event.getBlockName()));
        b.append(String.format(BASE_BLOCK_TYPE, event.getBaseType()));
        writeMessage(b.toString());
    }

    /**
     * Processes a LoggablePlayerEvent to send to Splunk.
     *
     * @param event The event to process.
     */
    private void writePlayerMessage(LoggablePlayerEvent event) {
        if (event.getAction() == PlayerEventAction.LOCATION) {
            logLegacyMoveEvent(event);
            return;
        }

        final StringBuilder b = new StringBuilder(
                String.format(BASE_PLAYER_STRING, event.getAction().asString(), event.getPlayerName()));
        if (event.getReason() != null) {
            b.append(String.format(REASON, event.getReason()));
        }
        b.append(' ').append(extractLocation(event));
        if (event.getMessage() != null) {
            b.append(String.format(MESSAGE, event.getMessage()));
        }
        writeMessage(b.toString());
    }

    /**
     * Logs move data in the way of the craftbukkit splunk minecraft app, which was based off of craftbukkit's
     * MoveEvent.
     *
     * @param event Movement event to be logged.
     */
    private void logLegacyMoveEvent(LoggablePlayerEvent event) {
        final String playerName = event.getPlayerName();
        final Point3dLong lastCoords = lastKnownCoordinates.getIfPresent(playerName);
        lastKnownCoordinates.put(playerName, event.getCoordinates());

        if (lastCoords == null) {
            return;
        }

        // message in this format to support old craftbukkit style move events (and how they were logged in splunk).
        writeMessage(
                "action=" + event.getAction().asString() +
                        " player=" + playerName +
                        " world=" + event.getWorldName() +
                        " from_x=" + lastCoords.xCoord +
                        " from_y=" + lastCoords.yCoord +
                        " from_z=" + lastCoords.zCoord +
                        " to_x=" + event.getCoordinates().xCoord +
                        " to_y=" + event.getCoordinates().yCoord +
                        " to_z=" + event.getCoordinates().zCoord +
                        " game_time=" + event.getWorldTime());
    }

    /**
     * Processes a {@link LoggableDeathEvent} to send to Splunk.
     *
     * @param event
     */
    private void writeDeathMessage(LoggableDeathEvent event) {
        final StringBuilder b = new StringBuilder();
        b.append(String.format(ACTION, event.getAction().asString()));
        b.append(String.format(VICTIM, event.getVictim()));
        if (event.getKiller() != null) {
            b.append(String.format(KILLER, event.getKiller()));
        }
        b.append(String.format(DAMAGE_SOURCE, event.getDamageSource()));
        b.append(' ').append(extractLocation(event));

        writeMessage(b.toString());
    }

    /**
     * Produces a String representing location in the original Splunk Minecraft App format.
     *
     * @param event The event to extract the location String from.
     * @return A String representing the location in the event.
     */
    private static String extractLocation(LoggableEvent event) {
        final StringBuilder b = new StringBuilder();
        b.append("world=").append(event.getWorldName());
        if (event.getCoordinates() != null) {
            b.append(' ' + "x=").append(event.getCoordinates().xCoord).append(' ').append("y=")
                    .append(event.getCoordinates().yCoord).append(' ').append("z=")
                    .append(event.getCoordinates().zCoord);
        }
        b.append(' ' + "game_time=").append(event.getWorldTime());
        return b.toString();
    }
}

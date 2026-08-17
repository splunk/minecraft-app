package com.splunk.forge.eventloggers;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import com.splunk.sharedmc.Point3dLong;
import com.splunk.sharedmc.event_loggers.AbstractEventLogger;
import com.splunk.sharedmc.loggable_events.LoggablePlayerEvent;
import com.splunk.sharedmc.loggable_events.LoggablePlayerEvent.PlayerEventAction;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

/**
 * Logs player connect/disconnect/chat/move events. Forge has no per-move event, so movement
 * is derived by polling player positions every {@link #MOVE_SAMPLE_INTERVAL_TICKS} ticks and
 * emitting only when a player has moved more than {@link #GRANULARITY} blocks.
 */
public class PlayerEventLogger extends AbstractEventLogger {
    public static final double GRANULARITY = 1.5;
    private static final int MOVE_SAMPLE_INTERVAL_TICKS = 10;

    private int tickCounter = 0;
    private final Map<UUID, Point3dLong> lastKnownCoordinates = new HashMap<>();

    public PlayerEventLogger(Properties props) {
        super(props);
    }

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        final LoggablePlayerEvent loggable = base(player, PlayerEventAction.PLAYER_CONNECT);
        loggable.setPlayerUuid(player.getStringUUID());
        logAndSend(loggable);
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        lastKnownCoordinates.remove(player.getUUID());
        logAndSend(base(player, PlayerEventAction.PLAYER_DISCONNECT));
    }

    @SubscribeEvent
    public void onChat(ServerChatEvent event) {
        final ServerPlayer player = event.getPlayer();
        final LoggablePlayerEvent loggable = base(player, PlayerEventAction.CHAT);
        loggable.setMessage(event.getMessage().getString());
        logAndSend(loggable);
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (++tickCounter % MOVE_SAMPLE_INTERVAL_TICKS != 0) {
            return;
        }
        final MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        for (final ServerPlayer player : server.getPlayerList().getPlayers()) {
            final Point3dLong current = new Point3dLong(player.getX(), player.getY(), player.getZ());
            final Point3dLong previous = lastKnownCoordinates.get(player.getUUID());
            if (previous != null && distance(previous, current) < GRANULARITY) {
                continue;
            }
            final LoggablePlayerEvent loggable = base(player, PlayerEventAction.LOCATION);
            loggable.setFrom(previous);
            loggable.setTo(current);
            lastKnownCoordinates.put(player.getUUID(), current);
            logAndSend(loggable);
        }
    }

    private LoggablePlayerEvent base(ServerPlayer player, PlayerEventAction action) {
        final Level level = player.level();
        final long worldTime = level.getGameTime();
        final String worldName = level.dimension().location().toString();
        final Point3dLong loc = new Point3dLong(player.getX(), player.getY(), player.getZ());
        final LoggablePlayerEvent loggable = new LoggablePlayerEvent(action, worldTime, worldName, loc);
        loggable.setPlayerName(player.getName().getString());
        return loggable;
    }

    private static double distance(Point3dLong a, Point3dLong b) {
        final double dx = a.xCoord - b.xCoord;
        final double dy = a.yCoord - b.yCoord;
        final double dz = a.zCoord - b.zCoord;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}

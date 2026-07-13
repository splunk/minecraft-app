package com.splunk.forge.eventloggers;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.splunk.sharedmc.event_loggers.AbstractEventLogger;
import com.splunk.sharedmc.loggable_events.LoggableServerEvent;
import com.splunk.sharedmc.loggable_events.LoggableServerEvent.ServerAction;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Logs server lifecycle and world-state events. Mirrors the spigot module's
 * {@code ServerEventLogger}.
 *
 * <p>Forge has no dedicated weather-change event (unlike Bukkit's {@code WeatherChangeEvent}),
 * so each loaded level's raining state is polled every {@link #WEATHER_SAMPLE_INTERVAL_TICKS}
 * ticks and reported only when it actually changed since the last sample.
 */
public class ServerEventLogger extends AbstractEventLogger {

    private static final int WEATHER_SAMPLE_INTERVAL_TICKS = 200;

    private final Map<ResourceLocation, Boolean> lastKnownWeather = new HashMap<>();
    private int tickCounter = 0;

    public ServerEventLogger(Properties props) {
        super(props);
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        LoggableServerEvent e = new LoggableServerEvent(ServerAction.SERVER_START, 0L, null);
        e.setMotd(event.getServer().getMotd());
        logAndSend(e);
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (++tickCounter % WEATHER_SAMPLE_INTERVAL_TICKS != 0) {
            return;
        }
        for (ServerLevel level : event.getServer().getAllLevels()) {
            ResourceLocation dimension = level.dimension().location();
            boolean raining = level.isRaining();
            Boolean previous = lastKnownWeather.put(dimension, raining);
            if (previous != null && previous == raining) {
                continue;
            }
            LoggableServerEvent e = new LoggableServerEvent(
                    ServerAction.WEATHER_CHANGE, level.getGameTime(), dimension.toString());
            e.setWeather(raining ? "storm" : "clear");
            logAndSend(e);
        }
    }
}

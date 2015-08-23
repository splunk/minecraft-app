//package com.splunk.logtosplunk.eventloggers;
//
//import java.util.Properties;
//
//import com.google.common.cache.Cache;
//import com.google.common.cache.CacheBuilder;
//import com.google.common.cache.CacheLoader;
//import com.splunk.spigot.Constants;
//import com.splunk.spigot.Point3dLong;
//import com.splunk.spigot.SplunkMessagePreparer;
//import com.splunk.spigot.event_loggers.AbstractEventLogger;
//import com.splunk.spigot.loggable_events.LoggablePlayerEvent;
//import com.splunk.spigot.loggable_events.LoggablePlayerEvent.PlayerEventAction;
//
//import net.minecraft.entity.player.EntityPlayer;
//import net.minecraft.util.Vec3;
//import net.minecraft.world.World;
//import net.minecraftforge.event.ServerChatEvent;
//import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
//import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
//import net.minecraftforge.fml.common.gameevent.PlayerEvent;
//import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
//import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent;
//import net.minecraftforge.fml.relauncher.Side;
//import net.minecraftforge.fml.relauncher.SideOnly;
//
///**
// * Handles the logging of player events.
// */
//public class PlayerEventLogger extends AbstractEventLogger {
//    public static final double GRANULARITY = 1.5;
//
//    /**
//     * Constructs a new PlayerEventLogger with the given SplunkMessagePreparer.
//     *
//     * @param splunkMessagePreparer Message preparer to send packaged raw messages to.
//     * @param props Properties to configure this EventLogger with.
//     */
//    public PlayerEventLogger(Properties props, SplunkMessagePreparer splunkMessagePreparer) {
//        super(props, splunkMessagePreparer);
//    }
//
//    /**
//     * Logs to Splunk when a player logs in.
//     *
//     * @param event The captured event.
//     */
//
//    public void onPlayerConnect(PlayerLoggedInEvent event) {
//        logAndSend(
//                generateLoggablePlayerEvent(event, PlayerEventAction.PLAYER_CONNECT, null, null));
//    }
//
//    /**
//     * Logs to Splunk when a player logs out.
//     *
//     * @param event The captured event.
//     */
//
//    public void onPlayerDisconnect(PlayerLoggedOutEvent event) {
//        logAndSend(
//                generateLoggablePlayerEvent(
//                        event, PlayerEventAction.PLAYER_DISCONNECT, null, null));
//    }
//
//    /**
//     * Logs to Splunk when a player chats and what they chat.
//     *
//     * @param chatEvent The captured chat event.
//     */
//
//    public void onPlayerChat(ServerChatEvent chatEvent) {
//        logAndSend(
//                generateLoggablePlayerEvent(chatEvent, PlayerEventAction.CHAT, chatEvent.message));
//    }
//
//    private static LoggablePlayerEvent generateLoggablePlayerEvent(
//            PlayerEvent event, PlayerEventAction actionType, String reason, String message) {
//        final World world = event.player.getEntityWorld();
//        final long worldTime = world.getWorldTime();
//        final String worldName = world.getWorldInfo().getWorldName();
//        final Vec3 playerPos = event.player.getPositionVector();
//        final Point3dLong coordinates = new Point3dLong(playerPos.xCoord, playerPos.yCoord, playerPos.zCoord);
//        final LoggablePlayerEvent loggable = new LoggablePlayerEvent(actionType, worldTime, worldName, coordinates);
//        loggable.setPlayerName(event.player.getDisplayNameString());
//        loggable.setReason(reason);
//        loggable.setMessage(message);
//
//        return loggable;
//    }
//
//    private static LoggablePlayerEvent generateLoggablePlayerEvent(
//            ServerChatEvent event, PlayerEventAction actionType, String message) {
//        final World world = event.player.getEntityWorld();
//        final long worldTime = world.getWorldTime();
//        final String worldName = world.getWorldInfo().getWorldName();
//        final Vec3 playerPos = event.player.getPositionVector();
//        final Point3dLong coordinates = new Point3dLong(playerPos.xCoord, playerPos.yCoord, playerPos.zCoord);
//        final LoggablePlayerEvent loggable = new LoggablePlayerEvent(actionType, worldTime, worldName, coordinates);
//        loggable.setPlayerName(event.player.getDisplayNameString());
//        loggable.setMessage(message);
//
//        return loggable;
//    }
//
//    /**
//     * Living update seems to get called about 10x/sec. We check if the update belongs to a player and if so we check if
//     * the players position has changed significantly based on {@code GRANULARITY}.
//     *
//     * @param playerMove The captured event.
//     */
//
//    public void onPlayerStatusReported(LivingUpdateEvent playerMove) {
//;
//            logAndSend(
//                    new LoggablePlayerEvent(
//                            PlayerEventAction.LOCATION, worldTime, worldName, coordinates).setPlayerName(playerName));
//        }
//    }
//
//    /**
//     * Keeps track players last positions, in a guava cache for it's eviction policy.
//     */
//    private final Cache<String, Vec3> lastKnownCoordinates = CacheBuilder.newBuilder().maximumSize(
//            Constants.MAX_PLAYERS).build(
//            new CacheLoader<String, Vec3>() {
//                @Override
//                public Vec3 load(String key) throws Exception {
//                    return lastKnownCoordinates.getIfPresent(key);
//                }
//            });
//}

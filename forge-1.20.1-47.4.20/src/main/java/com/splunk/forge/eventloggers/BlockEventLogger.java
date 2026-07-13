package com.splunk.forge.eventloggers;

import java.util.Properties;

import com.splunk.sharedmc.Point3dLong;
import com.splunk.sharedmc.event_loggers.AbstractEventLogger;
import com.splunk.sharedmc.loggable_events.LoggableBlockEvent;
import com.splunk.sharedmc.loggable_events.LoggableBlockEvent.BlockEventAction;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Logs block break/place events via Forge's native {@code BlockEvent.BreakEvent} and
 * {@code BlockEvent.EntityPlaceEvent}.
 */
public class BlockEventLogger extends AbstractEventLogger {

    public BlockEventLogger(Properties props) {
        super(props);
    }

    @SubscribeEvent
    public void onBreak(BlockEvent.BreakEvent event) {
        final Player player = event.getPlayer();
        log(levelOf(event.getLevel(), player), player, event.getPos(), event.getState(), BlockEventAction.BREAK);
    }

    @SubscribeEvent
    public void onPlace(BlockEvent.EntityPlaceEvent event) {
        final Player player = event.getEntity() instanceof Player p ? p : null;
        log(levelOf(event.getLevel(), player), player, event.getPos(), event.getPlacedBlock(), BlockEventAction.PLACE);
    }

    private static Level levelOf(LevelAccessor accessor, Player player) {
        if (accessor instanceof Level level) {
            return level;
        }
        return player != null ? player.level() : null;
    }

    private void log(Level level, Player player, BlockPos pos, BlockState state, BlockEventAction action) {
        if (level == null || level.isClientSide()) {
            return;
        }
        final long worldTime = level.getGameTime();
        final String worldName = level.dimension().location().toString();
        final Point3dLong loc = new Point3dLong(pos.getX(), pos.getY(), pos.getZ());
        final ResourceLocation id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        final String blockName = id != null ? id.toString() : "unknown";
        final String playerName = player != null ? player.getName().getString() : null;
        logAndSend(new LoggableBlockEvent(action, worldTime, worldName, loc, blockName, playerName));
    }
}

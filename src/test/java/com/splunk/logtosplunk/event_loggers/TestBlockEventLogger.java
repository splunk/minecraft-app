package com.splunk.logtosplunk.event_loggers;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import static junit.framework.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.splunk.logtosplunk.SplunkMessagePreparerSpy;
import com.splunk.logtosplunk.actions.BlockEventAction;
import com.splunk.logtosplunk.loggable_events.LoggableBlockEvent;

import mockit.MockUp;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraft.world.storage.WorldInfo;
import net.minecraftforge.event.world.BlockEvent;

public class TestBlockEventLogger {

    private BlockEventLogger logger;

    private SplunkMessagePreparerSpy spy;

    /**
     * For login logout event.
     */
    @Mock
    EntityPlayer player = mock(EntityPlayer.class);

    @Mock
    IBlockState state = mock(IBlockState.class);

    @Mock
    BlockPos blockPos = mock(BlockPos.class);

    @Mock
    World world = mock(World.class);

    @InjectMocks
    private BlockEvent.BreakEvent breakEvent = mock(BlockEvent.BreakEvent.class);

    @InjectMocks
    private BlockEvent.PlaceEvent placeEvent = mock(BlockEvent.PlaceEvent.class);

    /**
     * Milk was a bad choice.
     */
    @Before
    public void setUp() {
        spy = new SplunkMessagePreparerSpy();
        logger = new BlockEventLogger(spy);

        MockitoAnnotations.initMocks(this);
        when(player.getName()).thenReturn("Bro!");
        when(player.getPositionVector()).thenReturn(new Vec3(10, 10, 10));

        //for some reason only break event has a getPlayer, placeevent you just
        // access it directly...
        when(breakEvent.getPlayer()).thenReturn(player);
        when(player.getEntityWorld()).thenReturn(world);
        when(world.getWorldTime()).thenReturn(1000L);

        when(blockPos.getX()).thenReturn(10);
        when(blockPos.getY()).thenReturn(10);
        when(blockPos.getZ()).thenReturn(10);

        WorldInfo info = mock(WorldInfo.class);
        when(info.getWorldName()).thenReturn("WoName");
        when(world.getWorldInfo()).thenReturn(info);
        final Block block = mock(Block.class);

        when(block.getUnlocalizedName()).thenReturn("chocolate");
        final Item item = mock(Item.class);
        when(item.getItemStackDisplayName((net.minecraft.item.ItemStack) anyObject()))
                .thenReturn("I hope it was worth it block");

        //Need Item's static method 'getItemFromBlock' because blocks'.getItem() fails in non-test situation. >:(
        new MockUp<Item>() {
            @mockit.Mock
            public Item getItemFromBlock(Block block) {
                return item;
            }
        };

        when(state.getBlock()).thenReturn(block);
    }

    @Test
    public void testCaptureBreakEvent() {
        LoggableBlockEvent expected = getExpectedLoggableBlockEvent(BlockEventAction.BREAK);

        logger.captureBreakEvent(breakEvent);
        assertEquals(expected, spy.getLoggable());
    }

    @Test
    public void testCapturePlaceEvent() {
        LoggableBlockEvent expected = getExpectedLoggableBlockEvent(BlockEventAction.PLACE);

        logger.capturePlaceEvent(placeEvent);
        assertEquals(expected, spy.getLoggable());
    }

    private LoggableBlockEvent getExpectedLoggableBlockEvent(BlockEventAction action) {
        return new LoggableBlockEvent(action, 1000, "WoName", new Vec3(10, 10, 10)).setPlayerName("Bro!")
                .setBlockName("I_hope_it_was_worth_it_block").setBaseType("chocolate");
    }
}

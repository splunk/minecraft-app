package com.splunk.spigot.eventloggers;

import java.util.Properties;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import com.splunk.sharedmc.Point3dLong;
import com.splunk.sharedmc.event_loggers.AbstractEventLogger;
import com.splunk.sharedmc.loggable_events.LoggableBlockEvent;
import com.splunk.sharedmc.loggable_events.LoggableBlockEvent.BlockEventAction;

import org.bukkit.inventory.ItemStack;

/**
 * Handles the logging of block events.
 */
public class BlockEventLogger extends AbstractEventLogger implements Listener {

    public BlockEventLogger(Properties properties) {
        super(properties);
    }

    /**
     * Captures Block BreakEvents.
     *
     * @param event The captured BreakEvent.
     */
    @EventHandler
    public void captureBreakEvent(BlockBreakEvent event) {

        // only log successful log breaks
        if (!event.isCancelled()) {

            logAndSend(getLoggableBlockBreakPlaceEvent(BlockEventAction.BREAK, event, event.getPlayer().getInventory().getItemInMainHand().getType().toString()));
        }
    }

    /**
     * Captures Block PlaceEvents and sends them to the message preparer.
     *
     * @param event The captured PlaceEvent.
     */
    @EventHandler
    public void capturePlaceEvent(BlockPlaceEvent event) {
        logAndSend(getLoggableBlockBreakPlaceEvent(BlockEventAction.PLACE, event));
    }

    @EventHandler
    public void captureIgniteEvent(BlockIgniteEvent event) {

        String cause = null;
        switch ( event.getCause())
        {
            case ENDER_CRYSTAL:
                cause = "ENDER_CRYSTAL";
                break;
            case EXPLOSION:
                cause = "EXPLOSION";
                break;
            case FIREBALL:
                cause = "FIREBALL";
                break;
            case FLINT_AND_STEEL:
                cause = "FLINT_AND_STEEL";
                break;
            case LAVA:
                cause = "LAVA";
                break;
            case LIGHTNING:
                cause = "LIGHTNING";
                break;
            case SPREAD:
                cause = "SPREAD";
                break;
        }
        logAndSend(getLoggableBlockBreakPlaceEvent(BlockEventAction.IGNITE, event,cause));

    }

    private LoggableBlockEvent getLoggableBlockBreakPlaceEvent(BlockEventAction action, BlockEvent event) {
        return getLoggableBlockBreakPlaceEvent(action, event, null);
    }

    private LoggableBlockEvent getLoggableBlockBreakPlaceEvent(BlockEventAction action, BlockEvent event, String tool_used) {



        final Block block = event.getBlock();
        final Location location = event.getBlock().getLocation();
        final String baseType = block.getType().name();

        final World world = block.getWorld();


            if (tool_used.equals("AIR")) {
                tool_used = "FIST";
            }

        String name;
        switch (block.getType()) {
            case STONE:
                String[] StoneBlockNames = {
                        "STONE", "GRANITE", "POLISHED GRANITE", "DIORITE", "POLISHED_DIORITE",
                        "ANDESITE", "POLISHED_ANDESITE"
                };
                name = StoneBlockNames[block.getState().getData().toItemStack(1).getDurability()];
                break;
            case DIRT:
                String[] DirtBlockNames = {
                        "DIRT", "COARSE DIRT", "PODZOL"
                };
                name = DirtBlockNames[block.getState().getData().toItemStack(1).getDurability()];
                break;
            case SAND:
                String[] SandBlockNames = {
                        "SAND", "RED_SAND"
                };
                name = SandBlockNames[block.getState().getData().toItemStack(1).getDurability()];
                break;
            case LOG:
                String[] LogBlockNames = {
                        "OAK_LOG", "SPRUCE_LOG", "BIRCH_LOG", "JUNGLE_LOG"
                };
                name = LogBlockNames[block.getState().getData().toItemStack(1).getDurability()];
                break;
            case LOG_2:
                String[] Log2BlockNames = {
                        "ACACIA_LOG", "DARK_OAK_LOG"
                };
                name = Log2BlockNames[block.getState().getData().toItemStack(1).getDurability()];
                break;
            case LEAVES:
                // multiple values represent decay states.
                String[] LeavesBlockNames = {
                        "OAK_LEAVES", "SPRUCE_LEAVES", "BIRCH_LEAVES", "JUNGLE_LEAVES",
                        "OAK_LEAVES", "SPRUCE_LEAVES", "BIRCH_LEAVES", "JUNGLE_LEAVES",
                        "OAK_LEAVES", "SPRUCE_LEAVES", "BIRCH_LEAVES", "JUNGLE_LEAVES",
                        "OAK_LEAVES", "SPRUCE_LEAVES", "BIRCH_LEAVES", "JUNGLE_LEAVES"

                };
                name = LeavesBlockNames[block.getState().getData().toItemStack(1).getDurability()];
                break;
            case LEAVES_2:
                // multiple values represent decay states.
                String[] Leaves2BlockNames = {
                        "ACACIA_LEAVES", "DARK_OAK_LEAVES",
                        "ACACIA_LEAVES", "DARK_OAK_LEAVES",
                        "ACACIA_LEAVES", "DARK_OAK_LEAVES",
                        "ACACIA_LEAVES", "DARK_OAK_LEAVES"

                };
                name = Leaves2BlockNames[block.getState().getData().toItemStack(1).getDurability()];
                break;
            case WOOD:
                String[] woodBlockNames = {
                        "OAK_WOOD_PLANKS", "SPRUCE_WOOD_PLANKS", "BIRCH_WOOD_PLANKS", "JUNGLE_WOOD_PLANKS",
                        "ACACIA_WOOD_PLANKS", "DARK_OAK_WOOD_PLANKS"
                };
                name = woodBlockNames[block.getState().getData().toItemStack(1).getDurability()];
                break;
            case SANDSTONE:
                // multiple values represent decay states.
                String[] SandStoneBlockNames = {
                        "SANDSTONE", "CHISELED_SANDSTONE", "SMOOTH_SANDSTONE"
                };
                name = SandStoneBlockNames[block.getState().getData().toItemStack(1).getDurability()];
                break;
            case RED_SANDSTONE:
                // multiple values represent decay states.
                String[] RedSandStoneBlockNames = {
                        "RED_SANDSTONE", "CHISELED_RED_SANDSTONE", "SMOOTH_RED_SANDSTONE"
                };
                name = RedSandStoneBlockNames[block.getState().getData().toItemStack(1).getDurability()];
                break;
            case RED_ROSE:
                String[] RedRoseBlockNames = {
                        "POPPY", "BLUE_ORCHID", "ALLIUM", "AZURE_BLUET", "RED_TULIP", "ORANGE_TULIP", "WHITE_TULIP",
                        "PINK_TULIP", "OXEYE_DAISY"
                };
                name = RedRoseBlockNames[block.getState().getData().toItemStack(1).getDurability()];
                break;
            case YELLOW_FLOWER:
                String[] YellowFlowerBlockNames = {
                        "DANDELION"
                };
                name = YellowFlowerBlockNames[block.getState().getData().toItemStack(1).getDurability()];
                break;
            case DOUBLE_PLANT:
                String[] DoublePlantBlockNames = {
                        "SUNFLOWER", "LILAC", "DOUBLE_TALLGRASS", "LARGE_FERN", "ROSE_BUSH", "PEONY"
                };
                name = DoublePlantBlockNames[block.getState().getData().toItemStack(1).getDurability()];
                break;
            case HARD_CLAY:
                String[] HardenedClayBlockNames = {
                        "WHITE_HARDENED_CLAY", "ORANGE_HARDENED_CLAY", "MAGENTA_HARDENED_CLAY", "LIGHTBLUE_HARDENED_CLAY",
                        "YELLOW_HARDENED_CLAY", "LIME_HARDENED_CLAY", "PINK_HARDENED_CLAY", "GRAY_HARDENED_CLAY",
                        "LIGHTGRAY_HARDENED_CLAY", "CYAN_HARDENED_CLAY", "PURPLE_HARDENED_CLAY", "BLUE_HARDENED_CLAY",
                        "BROWN_HARDENED_CLAY", "GREEN_HARDENED_CLAY", "RED_HARDENED_CLAY", "BLACK_HARDENED_CLAY"
                };
                name = HardenedClayBlockNames[block.getState().getData().toItemStack(1).getDurability()];
                break;
            case STAINED_CLAY:
                String[] StainedClayBlockNames = {
                        "WHITE_STAINED_CLAY", "ORANGE_STAINED_CLAY", "MAGENTA_STAINED_CLAY", "LIGHTBLUE_STAINED_CLAY",
                        "YELLOW_STAINED_CLAY", "LIME_STAINED_CLAY", "PINK_STAINED_CLAY", "GRAY_STAINED_CLAY",
                        "LIGHTGRAY_STAINED_CLAY", "CYAN_STAINED_CLAY", "PURPLE_STAINED_CLAY", "BLUE_STAINED_CLAY",
                        "BROWN_STAINED_CLAY", "GREEN_STAINED_CLAY", "RED_STAINED_CLAY", "BLACK_STAINED_CLAY"
                };
                name = StainedClayBlockNames[block.getState().getData().toItemStack(1).getDurability()];
                break;
            case WOOL:
                String[] WoolBlockNames = {
                        "WHITE_WOOL", "ORANGE_WOOL", "MAGENTA_WOOL", "LIGHTBLUE_WOOL",
                        "YELLOW_WOOL", "LIME_WOOL", "PINK_WOOL", "GRAY_WOOL",
                        "LIGHTGRAY_WOOL", "CYAN_WOOL", "PURPLE_WOOL", "BLUE_WOOL",
                        "BROWN_WOOL", "GREEN_WOOL", "RED_WOOL", "BLACK_WOOL"
                };
                name = WoolBlockNames[block.getState().getData().toItemStack(1).getDurability()];
                break;
            case SPONGE:
                String[] SpongeBlockNames = {
                        "SPONGE", "WET_SPONGE"
                };

                name = SpongeBlockNames[block.getState().getData().toItemStack(1).getDurability()];
                break;
            case DOUBLE_STEP:
                String[] DoubleStoneSlabBlockNames = {"DOUBLE_STONE_SLAB", "DOUBLE_SANDSTONE_SLAB", "DOUBLE_WOODEN_SLAB", "DOUBLE_COBBLESTONE_SLAB",
                        "DOUBLE_BRICKS_SLAB", "DOUBLE_STONE_BRICK_SLAB", "DOUBLE_NETHER_BRICK_SLAB", "DOUBLE_QUARTZ_SLAB",
                        "SMOOTH_DOUBLE_STONE_SLAB", "SMOOTH_DOUBLE_SANDSTONE_SLAB"
                };
                name = DoubleStoneSlabBlockNames[block.getState().getData().toItemStack(1).getDurability()];
                break;
            case STEP:
                String[] StoneSlabBlockNames = {
                        "STONE_SLAB", "SANDSTONE_SLAB", "WOODEN_SLAB", "COBBLESTONE_SLAB",
                        "BRICKS_SLAB", "STONE_BRICK_SLAB", "NETHER_BRICK_SLAB", "QUARTZ_SLAB",
                        "SMOOTH_STONE_SLAB", "SMOOTH_SANDSTONE_SLAB"
                };
                name = StoneSlabBlockNames[block.getState().getData().toItemStack(1).getDurability()];
                break;
            case WOOD_STEP:
                String[] WoodSlabBlockNames = {
                        "OAK_WOOD_SLAB", "SPRUCE_WOOD_SLAB", "BIRCH_WOOD_SLAB", "JUNGLE_WOOD_SLAB",
                        "ACACIA_WOOD_SLAB", "DARK_OAK_WOOD_SLAB"
                };
                name = WoodSlabBlockNames[block.getState().getData().toItemStack(1).getDurability()];
                break;
            case PRISMARINE:
                String[] PrismarineBLockNames = {
                        "PRISMARINE", "PRISMARINE_BRICKS", "DARK_PRISMARINE"
                };
                name = PrismarineBLockNames[block.getState().getData().toItemStack(1).getDurability()];
                break;
            default:
                name = block.getType().name();
                break;
        }


        final Point3dLong coords = new Point3dLong(location.getX(), location.getY(), location.getZ());
        String playerName = null;

        if (event instanceof BlockBreakEvent) {
            playerName = ((BlockBreakEvent) event).getPlayer().getName();
        } else if (event instanceof BlockPlaceEvent) {
            playerName = ((BlockPlaceEvent) event).getPlayer().getName();
        }

        return new LoggableBlockEvent(action, world.getFullTime(), world.getName(), coords).setBlockName(name)
                .setPlayerName(playerName).setBaseType(baseType).setToolUsed(tool_used);
    }
}

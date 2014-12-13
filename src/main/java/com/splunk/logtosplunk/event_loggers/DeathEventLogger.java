package com.splunk.logtosplunk.event_loggers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.splunk.logtosplunk.LogToSplunkMod;
import com.splunk.logtosplunk.SplunkMessagePreparer;
import com.splunk.logtosplunk.actions.DeathEventAction;
import com.splunk.logtosplunk.loggable_events.LoggableDeathEvent;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Handles the logging of death events.
 */
public class DeathEventLogger {
    private static final String LOG_NAME_MODIFIER = " - DEATH";
    private static final Logger logger = LogManager.getLogger(LogToSplunkMod.LOGGER_NAME + LOG_NAME_MODIFIER);

    /**
     * Whether to turn off logging non-player related monster deaths. Monsters causing their own death generates a lot
     * of spammy events. E.g. Bats flying into lava...
     */
    public static final boolean IGNORE_MONSTER_ACCIDENTS = true;

    /**
     * Prepares and forwards data to be sent to splunk.
     */
    private SplunkMessagePreparer messagePreparer;

    /**
     * Constructor.
     *
     * @param messagePreparer used to process this class' captured data.
     */
    public DeathEventLogger(SplunkMessagePreparer messagePreparer) {
        this.messagePreparer = messagePreparer;
    }

    /**
     * Captures DeathEvents and sends them to the message preparer.
     *
     * @param event The captured BreakEvent.
     */
    @SubscribeEvent
    @SideOnly(Side.SERVER)
    public void captureDeathEvent(LivingDeathEvent event) {
        Boolean playerDied = event.entity instanceof EntityPlayer;
        String killer = null;
        if (event.source.getEntity() == null) {
            if (!playerDied && IGNORE_MONSTER_ACCIDENTS) {
                return;
            }
        } else {
            killer = event.source.getEntity().getName().replace(' ', '_');
        }
        final DeathEventAction deathAction = playerDied ? DeathEventAction.PLAYER_DIED : DeathEventAction.MOB_DIED;

        final String victim = event.entity.getName().replace(' ', '_');
        final String damageSource = event.source.getDamageType().replace(' ', '_');

        final World world = event.entity.getEntityWorld();
        final long gameTime = world.getWorldTime();
        final Vec3 position = event.entity.getPositionVector();
        final String worldName = world.getWorldInfo().getWorldName();

        // Death messages can be inferred so no need to get the death message...
        //System.out.println(event.source.getDeathMessage(event.entityLiving).getUnformattedTextForChat());

        logAndSend(
                new LoggableDeathEvent(deathAction, gameTime, worldName, position).setKiller(killer).setVicitim(victim)
                        .setDamageSource(damageSource));
    }

    /**
     * Logs via Log4j and forwards the messgage to the message preparer.
     *
     * @param loggable The message to log.
     */
    private void logAndSend(LoggableDeathEvent loggable) {
        logger.info(loggable);
        messagePreparer.writeMessage(loggable);
    }
}

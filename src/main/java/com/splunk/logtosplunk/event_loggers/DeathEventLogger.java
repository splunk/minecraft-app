package com.splunk.logtosplunk.event_loggers;

import com.splunk.logtosplunk.Point3dLong;
import com.splunk.logtosplunk.SplunkMessagePreparer;
import com.splunk.logtosplunk.loggable_events.LoggableDeathEvent;
import com.splunk.logtosplunk.loggable_events.LoggableDeathEvent.DeathEventAction;

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
public class DeathEventLogger extends AbstractEventLogger {

    /**
     * Whether to turn off logging non-player related monster deaths. Monsters causing their own death generates a lot
     * of spammy events. E.g. Bats flying into lava...
     */
    public static final boolean IGNORE_MONSTER_ACCIDENTS = true;

    /**
     * Constructor.
     *
     * @param messagePreparer used to process this class' captured data.
     */
    public DeathEventLogger(SplunkMessagePreparer messagePreparer) {
        super(messagePreparer);
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
            killer = event.source.getEntity().getDisplayName().getUnformattedText().replace(' ', '_');
        }
        final DeathEventAction deathAction = playerDied ? DeathEventAction.PLAYER_DIED : DeathEventAction.MOB_DIED;

        final String victim = event.entity.getDisplayName().getUnformattedText().replace(' ', '_');
        final String damageSource = event.source.getDamageType().replace(' ', '_');

        final World world = event.entity.getEntityWorld();
        final long gameTime = world.getWorldTime();
        Vec3 entityPos = event.entity.getPositionVector();
        final Point3dLong position = new Point3dLong(entityPos.xCoord, entityPos.yCoord, entityPos.zCoord);
        final String worldName = world.getWorldInfo().getWorldName();

        // Death messages can be inferred so no need to get the death message...
        //System.out.println(event.source.getDeathMessage(event.entityLiving).getUnformattedTextForChat());

        logAndSend(
                new LoggableDeathEvent(deathAction, gameTime, worldName, position).setKiller(killer).setVicitim(victim)
                        .setDamageSource(damageSource));
    }
}

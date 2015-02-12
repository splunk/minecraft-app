package com.splunk.logtosplunk.event_loggers;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;

import com.splunk.logtosplunk.SplunkMessagePreparerSpy;
import com.splunk.logtosplunk.loggable_events.LoggableDeathEvent;
import com.splunk.logtosplunk.loggable_events.LoggableDeathEvent.DeathEventAction;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraft.world.storage.WorldInfo;
import net.minecraftforge.event.entity.living.LivingDeathEvent;

public class TestDeathEventLogger {

    private DeathEventLogger logger;

    private SplunkMessagePreparerSpy spy;

    @Test
    public void testNoKillerPlayerVictim() {
        DamageSource source = mock(DamageSource.class);
        EntityPlayer victim = mock(EntityPlayer.class);

        when(source.getEntity()).thenReturn(null); //for illustration
        when(source.getDamageType()).thenReturn("FIERY DOOM");

        IChatComponent waldo = mock(IChatComponent.class);
        when(waldo.getUnformattedText()).thenReturn("Waldo");

        when(victim.getDisplayName()).thenReturn(waldo);
        setUpStandardMock(victim);
        LivingDeathEvent deathEvent = new LivingDeathEvent(victim, source);

        LoggableDeathEvent expected = getExpected(DeathEventAction.PLAYER_DIED, null, "Waldo", "FIERY_DOOM");

        logger.captureDeathEvent(deathEvent);
        assertEquals(expected, spy.getLoggable());
    }

    /**
     * Right now logging monster 'accidents' is disabled (bats flying into lava would generate spammy events).
     */
    @Test
    public void testNoKillerMonsterVictim() {
        DamageSource source = mock(DamageSource.class);
        EntityLiving victim = mock(EntityLiving.class);

        IChatComponent waldo = mock(IChatComponent.class);
        when(waldo.getUnformattedText()).thenReturn("Waldo");

        when(source.getEntity()).thenReturn(null); //for illustration
        when(source.getDamageType()).thenReturn("FIERY DOOM");
        when(victim.getDisplayName()).thenReturn(waldo);
        setUpStandardMock(victim);
        LivingDeathEvent deathEvent = new LivingDeathEvent(victim, source);

        logger.captureDeathEvent(deathEvent);
        assertEquals(null, spy.getLoggable());
    }

    @Test
    public void testPlayerVictim(){
        DamageSource source = mock(DamageSource.class);
        EntityPlayer victim = mock(EntityPlayer.class);
        EntityLiving killer = mock(EntityLiving.class);

        IChatComponent waldo = mock(IChatComponent.class);
        when(waldo.getUnformattedText()).thenReturn("Waldo");

        IChatComponent antiWaldo = mock(IChatComponent.class);
        when(antiWaldo.getUnformattedText()).thenReturn("anti-waldo");

        when(source.getEntity()).thenReturn(killer);
        when(source.getDamageType()).thenReturn("FIERY DOOM");
        when(victim.getDisplayName()).thenReturn(waldo);
        when(killer.getDisplayName()).thenReturn(antiWaldo);

        setUpStandardMock(victim);
        LivingDeathEvent deathEvent = new LivingDeathEvent(victim, source);

        LoggableDeathEvent expected = getExpected(DeathEventAction.PLAYER_DIED, "anti-waldo", "Waldo", "FIERY_DOOM");

        logger.captureDeathEvent(deathEvent);
        assertEquals(expected, spy.getLoggable());
    }

    @Test
    public void testMonsterVictim(){
        DamageSource source = mock(DamageSource.class);
        EntityLiving victim = mock(EntityLiving.class);
        EntityLiving killer = mock(EntityLiving.class);

        IChatComponent monsterWaldo = mock(IChatComponent.class);
        when(monsterWaldo.getUnformattedText()).thenReturn("MonsterWaldo");

        IChatComponent antiWaldo = mock(IChatComponent.class);
        when(antiWaldo.getUnformattedText()).thenReturn("anti-waldo");

        when(source.getEntity()).thenReturn(killer);
        when(source.getDamageType()).thenReturn("FIERY DOOM");
        when(victim.getDisplayName()).thenReturn(monsterWaldo);
        when(killer.getDisplayName()).thenReturn(antiWaldo);

        setUpStandardMock(victim);
        LivingDeathEvent deathEvent = new LivingDeathEvent(victim, source);

        LoggableDeathEvent expected = getExpected(DeathEventAction.MOB_DIED, "anti-waldo", "MonsterWaldo", "FIERY_DOOM");

        logger.captureDeathEvent(deathEvent);
        assertEquals(expected, spy.getLoggable());
    }

    private LoggableDeathEvent getExpected(
            DeathEventAction actionType, String killer, String victim, String damageSource) {
        return new LoggableDeathEvent(actionType, 1000, "woName", new Vec3(10, 10, 10)).setKiller(killer)
                .setVicitim(victim).setDamageSource(damageSource);
    }

    private void setUpStandardMock(Entity entity) {
        spy = new SplunkMessagePreparerSpy();
        logger = new DeathEventLogger(spy);

        when(entity.getPositionVector()).thenReturn(new Vec3(10, 10, 10));

        final World world = mock(World.class);
        when(entity.getEntityWorld()).thenReturn(world);

        when(world.getWorldTime()).thenReturn(1000L);

        WorldInfo info = mock(WorldInfo.class);
        when(info.getWorldName()).thenReturn("woName");
        when(world.getWorldInfo()).thenReturn(info);
    }
}

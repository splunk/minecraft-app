package com.splunk.sharedmc.loggable_events;

import static org.junit.Assert.assertTrue;
import com.splunk.sharedmc.Point3dLong;
import org.junit.Test;

public class LoggableCombatEventTest {
    @Test
    public void damage_carriesAttackerVictimAndAmount() {
        LoggableCombatEvent e = new LoggableCombatEvent(
                LoggableCombatEvent.CombatAction.DAMAGE, 0L, "world", new Point3dLong(1, 2, 3));
        e.setVictim("Steve").setSource("Zombie").setAmount(4.5).setCause("ENTITY_ATTACK");
        String json = e.toJson();
        assertTrue(json.contains("victim"));
        assertTrue(json.contains("Steve"));
        assertTrue(json.contains("source"));
        assertTrue(json.contains("4.5"));
        assertTrue(json.contains("damage"));
    }

    @Test
    public void hunger_carriesFoodLevel() {
        LoggableCombatEvent e = new LoggableCombatEvent(
                LoggableCombatEvent.CombatAction.HUNGER, 0L, "world", null);
        e.setVictim("Alex").setFoodLevel(7);
        String json = e.toJson();
        assertTrue(json.contains("food_level"));
        assertTrue(json.contains("hunger"));
    }
}

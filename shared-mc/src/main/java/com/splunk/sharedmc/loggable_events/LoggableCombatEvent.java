package com.splunk.sharedmc.loggable_events;

import com.splunk.sharedmc.Point3dLong;

/**
 * Combat and survival events: damage dealt/taken, kills, healing, hunger, respawn.
 */
public class LoggableCombatEvent extends AbstractLoggableEvent {

    public LoggableCombatEvent(CombatAction action, long gameTime, String worldName, Point3dLong location) {
        super(LoggableEventType.COMBAT, gameTime, worldName, location);
        this.addField(ACTION, action.asString());
    }

    public LoggableCombatEvent setVictim(String victim) {
        this.addField("victim", victim);
        return this;
    }

    public LoggableCombatEvent setSource(String source) {
        this.addField("source", source);
        return this;
    }

    public LoggableCombatEvent setAmount(double amount) {
        this.addField("amount", amount);
        return this;
    }

    public LoggableCombatEvent setCause(String cause) {
        this.addField(CAUSE, cause);
        return this;
    }

    public LoggableCombatEvent setFoodLevel(int foodLevel) {
        this.addField("food_level", foodLevel);
        return this;
    }

    public LoggableCombatEvent setHealthRemaining(double health) {
        this.addField("health_remaining", health);
        return this;
    }

    public enum CombatAction {
        DAMAGE("damage"),
        KILL("kill"),
        HEAL("heal"),
        HUNGER("hunger"),
        RESPAWN("respawn");

        private final String action;
        CombatAction(String action) { this.action = action; }
        public String asString() { return action; }
    }
}

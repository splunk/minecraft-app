package com.splunk.logtosplunk.loggable_events;

import com.splunk.logtosplunk.Point3dLong;

/**
 * Almost pojo with fields for information that might be associated with a block event.
 */
public class LoggableDeathEvent extends AbstractLoggableEvent {
    private String killer;
    private String vicitim;
    private String damageSource;
    private final DeathEventAction action;

    /**
     * Constructor.
     *
     * @param action The type of block action this represents, e.g. 'break'.
     */
    public LoggableDeathEvent(DeathEventAction action, long gameTime, String worldName, Point3dLong location) {
        super(LoggableEventType.DEATH, gameTime, worldName, location);
        this.action = action;
    }

    public String getVictim() {
        return vicitim;
    }

    public LoggableDeathEvent setVicitim(String vicitim) {
        this.vicitim = vicitim;
        return this;
    }

    public String getKiller() {
        return killer;
    }

    public LoggableDeathEvent setKiller(String killer) {
        this.killer = killer;
        return this;
    }

    public String getDamageSource() {
        return damageSource;
    }

    public LoggableDeathEvent setDamageSource(String damageSource) {
        this.damageSource = damageSource;
        return this;
    }

    public DeathEventAction getAction() {
        return action;
    }

    @Override
    public String toString() {
        final StringBuilder b = new StringBuilder();
        b.append(getType().getEventName()).append(": ").append(getAction().asString());
        if (killer != null) {
            b.append(", Killer: ").append(killer);
        }

        if (vicitim != null) {
            b.append(", Victim: ").append(vicitim);
        }

        if (damageSource != null) {
            b.append(", source: ").append(damageSource);
        }

        b.append(' ').append(getLocation());

        return b.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final LoggableDeathEvent that = (LoggableDeathEvent) o;

        if (action != that.action) {
            return false;
        }
        if (getCoordinates() != null ? !getCoordinates().equals(that.getCoordinates()) :
                that.getCoordinates() != null) {
            return false;
        }

        if (killer != null ? !killer.equals(that.killer) : that.killer != null) {
            return false;
        }

        if (damageSource != null ? !damageSource.equals(that.damageSource) : that.damageSource != null) {
            return false;
        }

        if (this.getWorldTime() != that.getWorldTime()) {
            return false;
        }
        if (getWorldName() != null ? !getWorldName().equals(that.getWorldName()) : that.getWorldName() != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = killer != null ? killer.hashCode() : 0;
        result = 31 * result + (action != null ? action.hashCode() : 0);
        result = 31 * result + (vicitim != null ? vicitim.hashCode() : 0);
        result = 31 * result + (damageSource != null ? damageSource.hashCode() : 0);
        return result;
    }

    /**
     * Different types of actions that can occur as part of a DeathEvent.
     */
    public enum DeathEventAction {
        MOB_DIED("mob_died"),
        PLAYER_DIED("player_died");

        /**
         * The name of the action.
         */
        private final String action;

        DeathEventAction(String action) {
            this.action = action;
        }

        /**
         * String representation of the action.
         *
         * @return The action in friendly String format.
         */
        public String asString() {
            return action;
        }
    }
}

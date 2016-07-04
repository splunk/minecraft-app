package com.splunk.sharedmc.loggable_events;

import com.splunk.sharedmc.Point3dLong;
import com.splunk.sharedmc.Utilities;


/**
 * Almost pojo with fields for information that might be associated with a block event.
 */
public class LoggableDeathEvent extends AbstractLoggableEvent {
    public static final String VICTIM = "victim";
    public static final String KILLER = "killer";
    public static final String DAMAGE_SOURCE = "damage_source";
    public static final String INSTRUMENT = "killing_instrument";

    private String _killer;
    private String _victim;
    private String _damageSource;
    private final DeathEventAction action;
    private String _instrument;

    /**
     * Constructor.
     *
     * @param action The type of block action this represents, e.g. 'break'.
     */
    public LoggableDeathEvent(DeathEventAction action, long gameTime, String worldName, Point3dLong location) {
        super(LoggableEventType.DEATH, gameTime, worldName, location);
        this.action = action;
        this.addField(ACTION, action.asString());
    }

    public String getVictim() {
        return _victim;
    }

    public LoggableDeathEvent setVictim(String victim) {
        this._victim = victim;
        this.addField(VICTIM, Utilities.sanitizeString(this._victim));
        return this;
    }

    public String getKiller() {
        return _killer;
    }

    public LoggableDeathEvent setKiller(String killer) {
        this._killer = killer;
        this.addField(KILLER, Utilities.sanitizeString(this._killer));
        return this;
    }

    public String getDamageSource() {
        return _damageSource;
    }

    public LoggableDeathEvent setDamageSource(String damageSource) {
        this._damageSource = damageSource;
        this.addField(DAMAGE_SOURCE, Utilities.sanitizeString(this._damageSource));
        return this;
    }

    public String getInstrument() {
        return _instrument;
    }

    public LoggableDeathEvent setInstrument(String instrument) {
        this._instrument = instrument;
        this.addField(INSTRUMENT, Utilities.removeSpaces(this._instrument));
        return this;
    }

    public DeathEventAction getAction() {
        return action;
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

        if (_killer != null ? !_killer.equals(that._killer) : that._killer != null) {
            return false;
        }

        if (_damageSource != null ? !_damageSource.equals(that._damageSource) : that._damageSource != null) {
            return false;
        }

        if (_instrument != null ? !_instrument.equals(that._instrument) : that._instrument != null) {
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
        int result = _killer != null ? _killer.hashCode() : 0;
        result = 31 * result + (action != null ? action.hashCode() : 0);
        result = 31 * result + (_victim != null ? _victim.hashCode() : 0);
        result = 31 * result + (_damageSource != null ? _damageSource.hashCode() : 0);
        result = 31 * result + (_instrument != null ? _instrument.hashCode() : 0);
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

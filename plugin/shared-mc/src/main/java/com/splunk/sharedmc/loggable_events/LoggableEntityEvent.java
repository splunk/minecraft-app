package com.splunk.sharedmc.loggable_events;

import com.splunk.sharedmc.Point3dLong;


public class LoggableEntityEvent extends AbstractLoggableEvent {


    // Strings for localization
    public static final String REASON = "reason";


    private final EntityEventAction action;

    // Private properties
    private String _entityName;
    private String _reason;

    /**
     * Default Constructor for the LoggableEntity class
     *
     * @param action    the type of entity action that is being raised.
     * @param gameTime  the game time the event took place.
     * @param worldname the name of the world in which the action took place.
     * @param location  the coordinates in the world that the event took place.
     */
    public LoggableEntityEvent(EntityEventAction action, long gameTime, String worldname, Point3dLong location) {
        super(LoggableEventType.ENTITY, gameTime, worldname, location);
        this.addField(ACTION, action.asString());
        this.action = action;

    }

    public String getEntityName() {
        return _entityName;
    }

    public LoggableEntityEvent setEntityName(String entityName) {
        this._entityName = entityName;
        this.addField(ENTITY_NAME, entityName);
        return this;
    }

    public String getReason() { return _reason;}

    public LoggableEntityEvent setReason(String reason) {
        this._reason = reason;
        this.addField(REASON, reason);
        return this;
    }

    /*

     */
    @Override
    public boolean equals(Object o) {

        if (this == o) {
            return true;
        }

        if (!(o instanceof LoggableEntityEvent)) {
            return false;
        }

        final LoggableEntityEvent that = (LoggableEntityEvent) o;

        if (action != that.action) {
            return false;
        }
        if (_entityName != null ? !_entityName.equals(that._entityName) : that._entityName != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = _entityName != null ? _entityName.hashCode() : 0;
        return result;
    }


    public enum EntityEventAction {
        ENTITY_SPAWN("entity_spawned"),
        ENTITY_BREAD("entity_bread");

        private final String action;

        EntityEventAction(String action) {
            this.action = action;
        }

        public String asString() {
            return action;
        }
    }
}
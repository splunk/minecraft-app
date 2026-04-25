package com.splunk.sharedmc.loggable_events;

import com.splunk.sharedmc.Point3dLong;

/** Almost pojo with fields for information that might be associated with a block event. */
public class LoggableDeathEvent extends AbstractLoggableEvent {
  public static final String VICTIM = "victim";
  public static final String KILLER = "killer";
  public static final String DAMAGE_SOURCE = "damage_source";

  /**
   * Constructor.
   *
   * @param action The type of block action this represents, e.g. 'break'.
   */
  public LoggableDeathEvent(
      DeathEventAction action,
      long gameTime,
      String worldName,
      Point3dLong location,
      String killer,
      String victim,
      String damageSource) {
    super(LoggableEventType.DEATH, gameTime, worldName, location);
    this.addField(ACTION, action.asString());
    this.addField(VICTIM, victim);
    this.addField(KILLER, killer);
    this.addField(DAMAGE_SOURCE, damageSource);
  }

  /** Different types of actions that can occur as part of a DeathEvent. */
  public enum DeathEventAction {
    MOB_DIED("mob_died"),
    PLAYER_DIED("player_died");

    /** The name of the action. */
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

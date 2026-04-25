package com.splunk.sharedmc.loggable_events;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.splunk.logging.SplunkCimLogEvent;
import com.splunk.sharedmc.Point3dLong;

/**
 * Classes extending this benefit from a convenient way to get a Json representation, time of
 * creation and event type, world name, coordinates and location.
 */
public class AbstractLoggableEvent extends SplunkCimLogEvent implements LoggableEvent {
  private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
  public static final String PLAYER_NAME = "player";
  public static final String CAUSE = "cause";
  public static final String ACTION = "action";

  /**
   * Constructor. Enforces that subclasses must have a loggable event type.
   *
   * @param type The type of event that this is.
   */
  public AbstractLoggableEvent(
      LoggableEventType type, long worldTime, String worldName, Point3dLong coordinates) {
    super(type.getEventName(), "");

    this.addField("time", System.currentTimeMillis());

    this.addField("game_time", worldTime);
    if (worldName != null) {
      this.addField("world", worldName);
    }
    this.addField("xCoord", coordinates.xCoord);
    this.addField("yCoord", coordinates.yCoord);
    this.addField("zCoord", coordinates.zCoord);
  }

  @Override
  public String toJson() {
    return gson.toJson(this);
  }

  @Override
  public void addField(String key, Object value) {
    if (value == null) {
      return;
    }
    super.addField(key, value);
  }
}

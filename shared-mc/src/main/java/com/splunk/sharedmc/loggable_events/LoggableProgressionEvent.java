package com.splunk.sharedmc.loggable_events;

/**
 * Player progression and activity: XP, level, enchant, craft, fish, command.
 */
public class LoggableProgressionEvent extends AbstractLoggableEvent {

    public LoggableProgressionEvent(ProgressionAction action, long gameTime, String worldName) {
        super(LoggableEventType.PROGRESSION, gameTime, worldName);
        this.addField(ACTION, action.asString());
    }

    public LoggableProgressionEvent setPlayerName(String playerName) {
        this.addField(PLAYER_NAME, playerName);
        return this;
    }

    public LoggableProgressionEvent setNewLevel(int level) {
        this.addField("new_level", level);
        return this;
    }

    public LoggableProgressionEvent setExpAmount(int exp) {
        this.addField("exp_amount", exp);
        return this;
    }

    /** Free-form detail: command text, enchant name, crafted item, fish caught. */
    public LoggableProgressionEvent setDetail(String detail) {
        this.addField("detail", detail);
        return this;
    }

    public enum ProgressionAction {
        EXP_CHANGE("exp_change"),
        LEVEL_CHANGE("level_change"),
        ENCHANT("enchant"),
        CRAFT("craft"),
        FISH("fish"),
        COMMAND("command");

        private final String action;
        ProgressionAction(String action) { this.action = action; }
        public String asString() { return action; }
    }
}

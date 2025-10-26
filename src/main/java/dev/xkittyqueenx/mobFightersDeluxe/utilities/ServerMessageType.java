package dev.xkittyqueenx.mobFightersDeluxe.utilities;

public enum ServerMessageType {
    GAME("<yellow>Game><gray> "),
    RECHARGE("<yellow>Recharge><gray> "),
    DEATH("<gray>Death><gray> "),
    SKILL("<blue>Skill><gray> "),
    ENERGY("<yellow>Energy><gray> "),
    ULTIMATE("<red>Ultimate><gray> "),
    DEBUG("<gray>Debug><gray> "),
    CONDITION("<yellow>Condition><gray> "),
    COMMAND("<yellow>Command><gray> "),
    ADMIN("<red>Admin><gray> ");

    private String message;

    ServerMessageType(String message) {
        this.message = message;
    }

    public String toString() {
        return message;
    }

}

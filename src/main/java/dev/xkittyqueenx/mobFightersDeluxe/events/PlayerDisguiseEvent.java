package dev.xkittyqueenx.mobFightersDeluxe.events;

import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class PlayerDisguiseEvent extends Event {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    private final Player player;

    private final Mob disguise;

    public PlayerDisguiseEvent(Player player, Mob disguise) {
        this.player = player;
        this.disguise = disguise;
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    public Player getPlayer() {
        return player;
    }

    public Mob getDisguise() {
        return disguise;
    }

}

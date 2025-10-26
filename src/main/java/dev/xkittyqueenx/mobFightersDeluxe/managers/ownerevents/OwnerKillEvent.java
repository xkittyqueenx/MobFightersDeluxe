package dev.xkittyqueenx.mobFightersDeluxe.managers.ownerevents;

import dev.xkittyqueenx.mobFightersDeluxe.events.SmashDamageEvent;
import org.bukkit.entity.Player;

public interface OwnerKillEvent {

    public abstract void onOwnerKillEvent(Player damager, Player damagee, SmashDamageEvent record);

}

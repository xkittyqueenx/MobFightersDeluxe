package dev.xkittyqueenx.mobFightersDeluxe.abilities.zombie;

import dev.xkittyqueenx.mobFightersDeluxe.MobFightersDeluxe;
import dev.xkittyqueenx.mobFightersDeluxe.abilities.Ability;
import dev.xkittyqueenx.mobFightersDeluxe.attributes.debuffs.Stunned;
import dev.xkittyqueenx.mobFightersDeluxe.fighters.Fighter;
import dev.xkittyqueenx.mobFightersDeluxe.managers.FighterManager;
import dev.xkittyqueenx.mobFightersDeluxe.managers.ownerevents.OwnerRightClickEvent;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Pose;
import org.bukkit.event.player.PlayerInteractEvent;

public class InfectiousSwipe extends Ability implements OwnerRightClickEvent, Stunned {

    private final double damage;

    private int swipe_task = -1;

    public InfectiousSwipe() {
        super();
        this.name = "Infectious Swipe";

        YamlConfiguration config = MobFightersDeluxe.getInstance().getConfigManager().getFightersConfig();
        this.damage = config.getDouble("zombie.infectious-swipe.damage");
        this.cooldownTime = config.getDouble("zombie.infectious-swipe.cooldown");

    }

    public void onOwnerRightClick(PlayerInteractEvent e) {
        Fighter fighter = FighterManager.getPlayerFighters().get(owner);
        if (fighter != null && fighter.isStunned()) {
            e.setCancelled(true);
            return;
        }
        if (scheduler.isCurrentlyRunning(swipe_task) || scheduler.isQueued(swipe_task)) {
            e.setCancelled(true);
            return;
        }
        checkAndActivate();
    }

    public void activate() {

    }

    @Override
    public void onStunned() {
        owner.setRiptiding(true);
    }

    public double getDamage() {
        return damage;
    }
}

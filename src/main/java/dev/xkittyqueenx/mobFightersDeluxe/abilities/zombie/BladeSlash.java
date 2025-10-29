package dev.xkittyqueenx.mobFightersDeluxe.abilities.zombie;

import dev.xkittyqueenx.mobFightersDeluxe.MobFightersDeluxe;
import dev.xkittyqueenx.mobFightersDeluxe.abilities.Ability;
import dev.xkittyqueenx.mobFightersDeluxe.attributes.interfaces.Stunned;
import dev.xkittyqueenx.mobFightersDeluxe.events.SmashDamageEvent;
import dev.xkittyqueenx.mobFightersDeluxe.fighters.Fighter;
import dev.xkittyqueenx.mobFightersDeluxe.managers.FighterManager;
import dev.xkittyqueenx.mobFightersDeluxe.managers.ownerevents.OwnerRightClickEvent;
import dev.xkittyqueenx.mobFightersDeluxe.utilities.DamageUtil;
import dev.xkittyqueenx.mobFightersDeluxe.utilities.Utils;
import org.bukkit.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Mob;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class BladeSlash extends Ability implements OwnerRightClickEvent, Stunned {

    private final double damage;
    private double hitbox_radius;
    protected long slash_duration_ms;

    private int start_task = -1;
    private int slash_task = -1;
    private final List<Mob> entitiesHit = new ArrayList<>();

    public BladeSlash() {
        super();
        this.name = "Blade Slash";
        this.isActiveAbility = true;

        YamlConfiguration config = MobFightersDeluxe.getInstance().getConfigManager().getFightersConfig();
        this.damage = config.getDouble("zombie.blade-slash.damage");
        this.hitbox_radius = config.getDouble("zombie.blade-slash.radius");
        this.cooldownTime = config.getDouble("zombie.blade-slash.cooldown");
        this.slash_duration_ms = config.getLong("zombie.blade-slash.duration");

    }

    @Override
    public void onStunned() {
        if (scheduler.isCurrentlyRunning(start_task) || scheduler.isQueued(start_task)) {
            scheduler.cancelTask(start_task);
        }
    }

    public void onOwnerRightClick(PlayerInteractEvent e) {
        Fighter fighter = FighterManager.getPlayerFighters().get(owner);
        if (fighter != null && fighter.isStunned()) {
            e.setCancelled(true);
            return;
        }
        checkAndActivate();
    }

    public void activate() {
        if (scheduler.isCurrentlyRunning(slash_task) || scheduler.isQueued(slash_task)) {
            scheduler.cancelTask(slash_task);
        }
        isActive = true;
        owner.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 10, 5, false, false, false));
        owner.swingMainHand();
        entitiesHit.clear();
        start_task = scheduler.scheduleSyncDelayedTask(plugin, () -> {
            if (owner == null) {
                scheduler.cancelTask(start_task);
                return;
            }
            isActive = false;
            slash_task = scheduler.scheduleSyncRepeatingTask(plugin, new Runnable() {
                final long start_time_ms = System.currentTimeMillis();
                private final Vector direction = owner.getLocation().getDirection();
                private final Location spiral_location = owner.getLocation().add(new Vector(0, 1, 0)).add(direction.clone().multiply(2));
                @Override
                public void run() {
                    if (owner == null || System.currentTimeMillis() - start_time_ms >= slash_duration_ms) {
                        scheduler.cancelTask(slash_task);
                        return;
                    }
                    Location old_location = spiral_location.clone();
                    double total_distance = 0.7;
                    spiral_location.add(direction.clone().multiply(total_distance));

                    // Create a single slash effect in the center
                    Location slashLocation = old_location.clone();
                    final double radius = hitbox_radius *= -1;
                    Particle.SWEEP_ATTACK.builder()
                            .location(slashLocation)
                            .count(0)
                            .receivers(32, true)
                            .offset(radius, 0, 0)
                            .spawn();
                    slashLocation.getWorld().playSound(slashLocation, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.2f, 0.75f);

                    // Move the slash forward
                    old_location.add(direction.clone().multiply(total_distance));

                    // Damage Portion
                    List<Mob> mobs = Utils.getNearby(spiral_location, hitbox_radius);
                    for (Mob mob : mobs) {
                        if (entitiesHit.contains(mob)) continue;
                        if (mob.hasMetadata(owner.getUniqueId().toString())) continue;
                        if (!DamageUtil.canDamage(mob, owner)) continue;
                        entitiesHit.add(mob);
                        SmashDamageEvent smashDamageEvent = new SmashDamageEvent(mob, owner, damage);
                        smashDamageEvent.multiplyKnockback(1.5);
                        smashDamageEvent.setKnockbackOrigin(spiral_location.subtract(direction));
                        smashDamageEvent.setIgnoreDamageDelay(true);
                        smashDamageEvent.setReason(name);
                        smashDamageEvent.callEvent();
                    }

                }
            }, 0L, 0L);
        }, 8L);
    }

    public double getDamage() {
        return damage;
    }

    public double getHitbox_radius() {
        return hitbox_radius;
    }

    public void setHitbox_radius(double hitbox_radius) {
        this.hitbox_radius = hitbox_radius;
    }
}

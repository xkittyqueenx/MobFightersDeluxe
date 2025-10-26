package dev.xkittyqueenx.mobFightersDeluxe.events;

import dev.xkittyqueenx.mobFightersDeluxe.projectiles.SmashProjectile;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityDamageEvent;

public class SmashDamageEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private boolean isCancelled = false;
    private LivingEntity damagee;
    private LivingEntity damager;
    private final long damage_time_ms;
    private double damage;
    private double knockbackMultiplier;
    private boolean ignoreArmor;
    private boolean ignoreDamageDelay;
    private EntityDamageEvent.DamageCause damageCause;
    private Location origin;
    private String reason;
    private Projectile projectile;
    private SmashProjectile smashProjectile;
    private String damagee_name;
    private String damager_name;
    private boolean display_as_last_damage = true;
    private ChatColor reason_color = ChatColor.GREEN;

    public SmashDamageEvent(LivingEntity damagee, LivingEntity damager, double damage) {
        if (damagee == null) {
            Bukkit.broadcastMessage(ChatColor.RED + "Warning: Damagee was null in SmashDamageEvent!");
        }
        this.damagee = damagee;
        this.damager = damager;
        this.damage_time_ms = System.currentTimeMillis();
        this.damage = damage;
        this.knockbackMultiplier = 1;
        this.ignoreArmor = false;
        this.ignoreDamageDelay = false;
        this.damageCause = EntityDamageEvent.DamageCause.CUSTOM;
        this.origin = null;
        this.reason = null;
        this.projectile = null;
        this.smashProjectile = null;
        if (damagee != null) {
            this.damagee_name = damagee.getName();
        }
        if (damager != null) {
            this.damager_name = damager.getName();
        }
    }

    public void setDamagee(LivingEntity damagee) {
        this.damagee = damagee;
    }

    public LivingEntity getDamagee() {
        return damagee;
    }

    public void setDamager(LivingEntity damager) {
        this.damager = damager;
    }

    public LivingEntity getDamager() {
        return damager;
    }

    public long getDamageTimeMs() {
        return damage_time_ms;
    }

    public void setDamage(double damage) {
        this.damage = damage;
    }

    public double getDamage() {
        return damage;
    }

    public SmashProjectile getSmashProjectile() { return smashProjectile; }

    public void setSmashProjectile(SmashProjectile smashProjectile) {
        this.smashProjectile = smashProjectile;
    }

    public void multiplyKnockback(double knockbackMultiplier) {
        this.knockbackMultiplier *= knockbackMultiplier;
    }

    public double getKnockbackMultiplier() {
        return knockbackMultiplier;
    }

    public void setIgnoreArmor(boolean ignoreArmor) {
        this.ignoreArmor = ignoreArmor;
    }

    public boolean getIgnoreArmor() {
        return ignoreArmor;
    }

    public void setIgnoreDamageDelay(boolean ignoreDamageDelay) {
        this.ignoreDamageDelay = ignoreDamageDelay;
    }

    public boolean getIgnoreDamageDelay() {
        return ignoreDamageDelay;
    }

    public void setDamageCause(EntityDamageEvent.DamageCause damageCause) {
        this.damageCause = damageCause;
        if (reason != null) {
            return;
        }
        String got_reason = "N/A";
        if (damageCause == EntityDamageEvent.DamageCause.PROJECTILE) {
            got_reason = "Projectile";
        } else if (damageCause == EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
            got_reason = "Attack";
        } else if (damageCause == EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK) {
            got_reason = "Attack";
        } else if (damageCause == EntityDamageEvent.DamageCause.VOID) {
            got_reason = "Void";
        } else if (damageCause == EntityDamageEvent.DamageCause.CUSTOM) {
            got_reason = "Custom";
        } else if (damageCause == EntityDamageEvent.DamageCause.LAVA) {
            got_reason = "Lava";
        } else if (damageCause == EntityDamageEvent.DamageCause.STARVATION) {
            got_reason = "Starvation";
        } else if (damageCause == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION) {
            got_reason = "Explosion";
        } else if (damageCause == EntityDamageEvent.DamageCause.FALL) {
            got_reason = "Falling";
        } else if (damageCause == EntityDamageEvent.DamageCause.FIRE_TICK) {
            got_reason = "Fire";
        } else if (damageCause == EntityDamageEvent.DamageCause.FIRE) {
            got_reason = "Fire";
        } else if (damageCause == EntityDamageEvent.DamageCause.POISON) {
            got_reason = "Poison";
        } else if (damageCause == EntityDamageEvent.DamageCause.SUFFOCATION) {
            got_reason = "Suffocation";
        } else if (damageCause == EntityDamageEvent.DamageCause.DROWNING) {
            got_reason = "Drowning";
        } else if (damageCause == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION) {
            got_reason = "Explosion";
        } else if (damageCause == EntityDamageEvent.DamageCause.CONTACT) {
            got_reason = "Contact";
        } else if (damageCause == EntityDamageEvent.DamageCause.FALLING_BLOCK) {
            got_reason = "Falling Block";
        } else if (damageCause == EntityDamageEvent.DamageCause.WITHER) {
            got_reason = "Wither";
        } else if (damageCause == EntityDamageEvent.DamageCause.THORNS) {
            got_reason = "Thorns";
        } else if (damageCause == EntityDamageEvent.DamageCause.LIGHTNING) {
            got_reason = "Lightning";
        } else if (damageCause == EntityDamageEvent.DamageCause.MAGIC) {
            got_reason = "Magic";
        } else if (damageCause == EntityDamageEvent.DamageCause.MELTING) {
            got_reason = "Melting";
        } else if (damageCause == EntityDamageEvent.DamageCause.SUICIDE) {
            got_reason = "Suicide";
        }
        setReason(got_reason);
        if (damager_name != null) {
            return;
        }
        setDamagerName(got_reason);
    }

    public EntityDamageEvent.DamageCause getDamageCause() {
        return damageCause;
    }

    public void setKnockbackOrigin(Location origin) {
        this.origin = origin;
    }

    public Location getKnockbackOrigin() {
        return origin;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }

    public void setProjectile(Projectile projectile) {
        this.projectile = projectile;
    }

    public Projectile getProjectile() {
        return projectile;
    }

    public void setDamageeName(String damagee_name) {
        this.damagee_name = damagee_name;
    }

    public String getDamageeName() {
        return damagee_name;
    }

    public void setDamagerName(String damager_name) {
        this.damager_name = damager_name;
    }

    public String getDamagerName() {
        return damager_name;
    }

    public void setDisplayAsLastDamage(boolean display_as_last_damage) {
        this.display_as_last_damage = display_as_last_damage;
    }

    public boolean getDisplayAsLastDamage() {
        return display_as_last_damage;
    }

    public void setReasonColor(ChatColor reason_color) {
        this.reason_color = reason_color;
    }

    public ChatColor getReasonColor() {
        return reason_color;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    @Override
    public void setCancelled(boolean isCancelled) {
        this.isCancelled = isCancelled;
    }

    @Override
    public boolean isCancelled() {
        return isCancelled;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof SmashDamageEvent)) {
            return false;
        }
        SmashDamageEvent check = (SmashDamageEvent) o;
        boolean to_return = true;
        if (check.getDamagerName() == null) {
            to_return = to_return && getDamagerName() == null;
        } else {
            to_return = to_return && check.getDamagerName().equals(getDamagerName());
        }
        if (check.getDamageeName() == null) {
            to_return = to_return && getDamageeName() == null;
        } else {
            to_return = to_return && check.getDamageeName().equals(getDamageeName());
        }
        if (check.getReason() == null) {
            to_return = to_return && getReason() == null;
        } else {
            to_return = to_return && check.getReason().equals(getReason());
        }
        return to_return;
    }
}

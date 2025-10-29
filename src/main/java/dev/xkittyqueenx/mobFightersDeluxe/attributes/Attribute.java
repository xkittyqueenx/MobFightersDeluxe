package dev.xkittyqueenx.mobFightersDeluxe.attributes;

import dev.xkittyqueenx.mobFightersDeluxe.MobFightersDeluxe;
import dev.xkittyqueenx.mobFightersDeluxe.attributes.interfaces.Stunned;
import dev.xkittyqueenx.mobFightersDeluxe.fighters.Fighter;
import dev.xkittyqueenx.mobFightersDeluxe.managers.FighterManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;

public abstract class Attribute extends BukkitRunnable implements Listener {

    public static MiniMessage mm = MiniMessage.miniMessage();

    public ItemStack getItemStack() {
        return itemStack;
    }

    public void setItemStack(ItemStack itemStack) {
        this.itemStack = itemStack;
    }

    public enum AbilityUsage {
        LEFT_CLICK("Left-Click"),
        RIGHT_CLICK("Right-Click"),
        RIGHT_CLICK_TOGGLE("Right-Click/Toggle"),
        BLOCKING("Hold/Release Right-Click"),
        RIGHT_CLICK_HOLD("Right-Click/Hold"),
        HOLD_BLOCKING("Hold Right-Click"),
        CHARGE_BOW("Charge Bow"),
        CHARGE_CROSSBOW("Charge Crossbow"),
        LEFT_RIGHT_CLICK("Left/Right-Click"),
        CROUCH("Crouch"),
        RIGHT_CLICK_CROUCH("Right-Click/Crouch"),
        DOUBLE_JUMP("Double Jump"),
        PASSIVE("Passive"),
        DOUBLE_RIGHT_CLICK("Double Right-Click"),
        RIGHT_CLICK_DROP("Right-Click/Drop"),
        LEFT_RIGHT_DROP("Left/Right-Click/Drop"),
        RIGHT_OFF_HAND("Right-Click/Off-Hand"),
        OFF_HAND("Toggle Off-Hand (F)"),
        ULTIMATE("Ultimate"),
        NONE("");

        private String message;

        AbilityUsage(String message) {
            this.message = message;
        }

        public String toString() {
            return message;
        }
    }

    public String name = "No Set Name.";
    protected List<Component> lore = new ArrayList<>();
    protected Plugin plugin;
    protected Player owner;
    protected BukkitTask task;

    protected ItemStack itemStack = null;

    public double cooldownTime = 0;
    public float expUsed = 0;
    protected boolean needsExactXP = true;

    protected AbilityUsage usage = AbilityUsage.RIGHT_CLICK;
    public BukkitScheduler scheduler = Bukkit.getScheduler();

    public Attribute() {
        this.plugin = MobFightersDeluxe.getInstance();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public boolean check() {
        if (owner == null)
            return false;
        Fighter kit = FighterManager.getPlayerFighters().get(owner);
        if (kit != null && !kit.isActive())
            return false;
        if (hasCooldown())
            return false;
        if (needsExactXP && expUsed > 0 && owner.getExp() < expUsed)
            return false;
        if(!needsExactXP && owner.getExp() <= 0)
            return false;
        if (kit != null) {
            if (this instanceof Stunned && kit.isStunned()) return false;
        }
        return true;
    }

    public void checkAndActivate() {
        if (owner == null)
            return;
        Fighter kit = FighterManager.getPlayerFighters().get(owner);
        if (kit != null && !kit.isActive()) {
            return;
        }
        if (hasCooldown()) {
            String time_string = String.format("%.1f seconds", MobFightersDeluxe.getInstance().getCooldownManager().getRemainingTimeFor(this, owner) / 1000.0);
            owner.sendMessage(MiniMessage.miniMessage().deserialize("<#D3D3D3>You cannot use <gold>" + name + " <#D3D3D3>for <yellow>" + time_string));
            return;
        }
        if (!hasCooldown()) {
            if (!check()) {
                return;
            }
            if (expUsed > 0) {
                owner.setExp(Math.max(owner.getExp() - expUsed, 0));
            }
            applyCooldown();
            activate();
        }
    }

    public abstract void activate();

    public boolean hasCooldown() {
        return (MobFightersDeluxe.getInstance().getCooldownManager().getRemainingTimeFor(this, owner) > 0);
    }

    public void applyCooldown() {
        applyCooldown(cooldownTime);
    }

    public void applyCooldown(double cooldownTime) {
        MobFightersDeluxe.getInstance().getCooldownManager().addCooldown(this, (long) (cooldownTime * 1000), owner);
    }

    public void afterRemoval(Player player) {

    }

    public void remove() {
        this.setOwner(null);
        cancelTask();
        HandlerList.unregisterAll(this);
    }

    public boolean cancelTask() {
        if (task != null) {
            task.cancel();
            return true;
        }
        return false;
    }

    @Override
    public void run() {
        cancelTask();
    }

    public void setOwner(Player owner) {
        this.owner = owner;
    }

    public Player getOwner() {
        return owner;
    }

    public AbilityUsage getUsage() {
        return usage;
    }

    public List<Component> getLore() {
        return lore;
    }

}

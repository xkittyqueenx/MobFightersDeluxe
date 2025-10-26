package dev.xkittyqueenx.mobFightersDeluxe.attributes;

import dev.xkittyqueenx.mobFightersDeluxe.events.SmashDamageEvent;
import dev.xkittyqueenx.mobFightersDeluxe.fighters.Fighter;
import dev.xkittyqueenx.mobFightersDeluxe.managers.FighterManager;
import dev.xkittyqueenx.mobFightersDeluxe.managers.GameManager;
import dev.xkittyqueenx.mobFightersDeluxe.managers.ownerevents.OwnerDealSmashDamageEvent;
import dev.xkittyqueenx.mobFightersDeluxe.managers.ownerevents.OwnerKillEvent;
import dev.xkittyqueenx.mobFightersDeluxe.managers.smashserver.SmashServer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;

public class Ultimate extends Attribute implements OwnerDealSmashDamageEvent, OwnerKillEvent {

    public float ultimateCharge = 0;
    public float ultimateChargeIncrease;
    public float ultimateMaxCharge;

    public double charge_multiplier = 1.0;

    public Ultimate(float ultimateChargeIncrease, float ultimateMaxCharge) {
        super();
        this.name = "Ultimate Charge";
        this.ultimateChargeIncrease = ultimateChargeIncrease;
        this.ultimateMaxCharge = ultimateMaxCharge;

        task = this.runTaskTimer(plugin, 0, 20L);
    }

    @Override
    public void run() {
        checkAndActivate();
    }

    public void activate() {
        this.ultimateCharge = (Math.min(((ultimateCharge) + ultimateChargeIncrease), ultimateMaxCharge));
        updateItemDurability();
    }

    public void updateItemDurability() {
        if (owner == null) return;
        ItemStack item = owner.getInventory().getItem(4);
        if (item == null) {
            return;
        }
        short maxDurability = item.getType().getMaxDurability();
        // Ensure durability is between 0 and maxDurability
        int newDurability = Math.max(0, Math.min(maxDurability,
                (int) (maxDurability - (ultimateCharge / ultimateMaxCharge) * maxDurability)));

        Damageable itemMeta = (Damageable) item.getItemMeta();
        itemMeta.setDamage(newDurability);
        item.setItemMeta(itemMeta);
        owner.getInventory().setItem(4, item);
    }

    @Override
    public void onOwnerDealSmashDamageEvent(SmashDamageEvent e) {
        Fighter fighter = FighterManager.getPlayerFighters().get(owner);
        if (owner == null || !fighter.isActive()) {
            return;
        }
        if (!e.isCancelled()) {
            if (!(e.getDamagee() instanceof Player player)) {
                return;
            }
            if (e.getDamagee().equals(e.getDamager())) {
                return;
            }
            SmashServer server = GameManager.getPlayerServer(player);
            this.ultimateCharge = (float) Math.min((ultimateCharge + (e.getDamage() * (1.25 * charge_multiplier))), ultimateMaxCharge);
            updateItemDurability();
        }
    }

    @Override
    public void onOwnerKillEvent(Player player, Player damagee, SmashDamageEvent record) {
        if(!player.equals(owner)) {
            return;
        }
        this.ultimateCharge = (float) Math.min((ultimateCharge + ((ultimateMaxCharge / 13) * charge_multiplier)), ultimateMaxCharge);
    }

    public float getUltimateCharge(Fighter kit) {
        return ultimateCharge;
    }

    public float getUltimateMaxCharge(Fighter kit) {
        return ultimateMaxCharge;
    }

    public float setUltimateCharge(float charge, Fighter kit) {
        return ultimateCharge = charge;
    }


}

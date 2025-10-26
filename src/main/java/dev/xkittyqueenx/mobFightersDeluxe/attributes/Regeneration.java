package dev.xkittyqueenx.mobFightersDeluxe.attributes;

import dev.xkittyqueenx.mobFightersDeluxe.managers.GameManager;
import dev.xkittyqueenx.mobFightersDeluxe.managers.smashserver.SmashServer;
import org.bukkit.Sound;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.potion.PotionEffectType;

public class Regeneration extends Attribute {

    public double regen_amount;
    protected double delay_ticks;

    public boolean isHealingBoosted = false;
    public boolean isAntiHealed = false;

    public Regeneration() {
        this(0.25);
    }

    public Regeneration(double regen_amount) {
        this(regen_amount, 20);
    }

    public Regeneration(double regen_amount, long delay_ticks) {
        super();
        this.name = "Regeneration";
        this.regen_amount = regen_amount;
        this.delay_ticks = delay_ticks;
        task = this.runTaskTimer(plugin, 0, delay_ticks);
    }

    @Override
    public void run() {
        if (owner == null) {
            this.cancel();
            return;
        }
        if (owner.getPotionEffect(PotionEffectType.WITHER) != null) {
            owner.playSound(owner.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1.25f);
            return;
        }
        checkAndActivate();
    }

    public void activate() {
        if (!owner.isDead() && owner.getFoodLevel() > 0) {
            // Use packets here to make the visual effect on the healthbar appear
            if (isHealingBoosted) {
                owner.heal(regen_amount * 2, EntityRegainHealthEvent.RegainReason.CUSTOM);
            } else {
                owner.heal(regen_amount, EntityRegainHealthEvent.RegainReason.CUSTOM);
            }
            playHealthAnimation();
        }
    }

    public void playHealthAnimation() {
        owner.sendHealthUpdate();
    }

}

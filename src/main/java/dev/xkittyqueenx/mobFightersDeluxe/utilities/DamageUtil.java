package dev.xkittyqueenx.mobFightersDeluxe.utilities;

import dev.xkittyqueenx.mobFightersDeluxe.events.SmashDamageEvent;
import dev.xkittyqueenx.mobFightersDeluxe.fighters.Fighter;
import dev.xkittyqueenx.mobFightersDeluxe.fighters.SpectatorFighter;
import dev.xkittyqueenx.mobFightersDeluxe.managers.FighterManager;
import dev.xkittyqueenx.mobFightersDeluxe.managers.GameManager;
import dev.xkittyqueenx.mobFightersDeluxe.managers.gamestate.GameState;
import dev.xkittyqueenx.mobFightersDeluxe.managers.smashserver.SmashServer;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;

public class DamageUtil {

    private static final HashMap<LivingEntity, DamageRateTracker> damageRateTrackers = new HashMap<LivingEntity, DamageRateTracker>();

    public static void cleansePlayer(Player player) {
        player.removePotionEffect(PotionEffectType.SLOWNESS);
        player.removePotionEffect(PotionEffectType.WEAKNESS);
        player.removePotionEffect(PotionEffectType.POISON);
        player.removePotionEffect(PotionEffectType.WITHER);
        player.setFireTicks(0);
    }

    public static void borderKill(Player player, boolean lightning) {
        SmashServer server = GameManager.getPlayerServer(player);
        Fighter fighter = FighterManager.getPlayerFighters().get(player);
        if (lightning && fighter != null && !(fighter instanceof SpectatorFighter)) {
            player.getWorld().strikeLightningEffect(player.getLocation());
        }
        double pre_lives = 0;
        if(server != null) {
            pre_lives = server.getLives(player);
        }
        SmashDamageEvent smashDamageEvent = new SmashDamageEvent(player, null, 1000);
        smashDamageEvent.multiplyKnockback(0);
        smashDamageEvent.setIgnoreArmor(true);
        smashDamageEvent.setIgnoreDamageDelay(true);
        smashDamageEvent.setDamageCause(EntityDamageEvent.DamageCause.VOID);
        smashDamageEvent.setDamagerName("Void");
        smashDamageEvent.setReason("World Border");
        Bukkit.getServer().getPluginManager().callEvent(smashDamageEvent);
        if (pre_lives > 0) {
            return;
        }
        player.teleport(player.getWorld().getSpawnLocation());
    }

    public static boolean canDamage(LivingEntity damagee, LivingEntity damager) {
        if(damagee.equals(damager)) {
            return true;
        }
        if (damagee instanceof Player player) {
            if (player.getGameMode() != GameMode.SURVIVAL && player.getGameMode() != GameMode.ADVENTURE) {
                return false;
            }
            Fighter fighter = FighterManager.getPlayerFighters().get(player);
            if (fighter != null && fighter.isInvincible()) {
                return false;
            }
            SmashServer server = GameManager.getPlayerServer(player);
            if(server != null) {
                return (server.getState() == GameState.GAME_PLAYING);
            } else {
                return false;
            }
        }
        return true;
    }

    public static DamageRateTracker getDamageRateTracker(LivingEntity living) {
        DamageRateTracker tracker = damageRateTrackers.get(living);
        if(tracker == null) {
            tracker = new DamageRateTracker(living);
            damageRateTrackers.put(living, tracker);
        }
        return tracker;
    }

    public static class DamageRateTracker {

        private LivingEntity living;
        private HashMap<LivingEntity, Long> lastHurt = new HashMap<>();
        private HashMap<LivingEntity, Long> lastHurtBy = new HashMap<>();
        private long lastHurtByWorld = 0;

        public DamageRateTracker(LivingEntity living) {
            this.living = living;
        }

        public boolean canBeHurtBy(LivingEntity damager) {
            if (damager == null) {
                if (System.currentTimeMillis() - lastHurtByWorld > 250) {
                    lastHurtByWorld = System.currentTimeMillis();
                    return true;
                }
                return false;
            }
            if (!lastHurtBy.containsKey(damager)) {
                lastHurtBy.put(damager, System.currentTimeMillis());
                return true;
            }
            if (System.currentTimeMillis() - lastHurtBy.get(damager) > 400) {
                lastHurtBy.put(damager, System.currentTimeMillis());
                return true;
            }
            return false;
        }

        public boolean canHurt(LivingEntity damagee) {
            if (damagee == null) {
                return true;
            }
            if (!lastHurt.containsKey(damagee)) {
                lastHurt.put(damagee, System.currentTimeMillis());
                return true;
            }
            if (System.currentTimeMillis() - lastHurt.get(damagee) > 400) {
                lastHurt.put(damagee, System.currentTimeMillis());
                return true;
            }
            return false;
        }

    }
}

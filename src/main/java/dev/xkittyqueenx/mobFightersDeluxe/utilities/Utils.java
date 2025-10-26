package dev.xkittyqueenx.mobFightersDeluxe.utilities;

import dev.xkittyqueenx.mobFightersDeluxe.MobFightersDeluxe;
import dev.xkittyqueenx.mobFightersDeluxe.attributes.ExpCharge;
import dev.xkittyqueenx.mobFightersDeluxe.fighters.Fighter;
import dev.xkittyqueenx.mobFightersDeluxe.managers.FighterManager;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Utils {

    public static void refreshDisguises() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
            players.removeIf(check -> check.equals(player));

            for (Player other : players) {
                Fighter fighter = FighterManager.getPlayerFighters().get(other);
                if (fighter == null || fighter.getDisguise() == null) {
                    player.showPlayer(MobFightersDeluxe.getInstance(), other);
                    continue;
                }
                player.hidePlayer(MobFightersDeluxe.getInstance(), other);
            }
        }
    }

    public static Player getPlayerFromMob(Mob mob) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            String uuid = player.getUniqueId().toString();
            if (mob.hasMetadata(uuid)) {
                return player;
            }
        }
        return null;
    }

    public static void sendActionBarMessage(String message, Audience player) {
        Audience.audience(player).sendActionBar(MiniMessage.miniMessage().deserialize(message));
    }

    public static double msToSeconds(long milliseconds) {
        return (long) (milliseconds / (double) 100) / (double) 10;
    }

    public static void fullHeal(LivingEntity livingEntity) {
        livingEntity.setFireTicks(0);
        if (livingEntity instanceof Player player) {
            player.setGameMode(GameMode.ADVENTURE);
            player.setHealthScaled(true);
            Objects.requireNonNull(player.getAttribute(Attribute.SCALE)).setBaseValue(1);
            Objects.requireNonNull(player.getAttribute(Attribute.ARMOR)).setBaseValue(0);
            player.setGravity(true);
            player.setFoodLevel(20);
            player.setSaturation(3);
            player.setLevel(0);
            player.setExp(0);
            Objects.requireNonNull(player.getAttribute(Attribute.ATTACK_SPEED)).setBaseValue(1000);
            Fighter fighter = FighterManager.getPlayerFighters().get(player);
            if(fighter != null && fighter.getAttributeByClass(ExpCharge.class) != null) {
                player.setExp(1f);
            }
        }
    }

    public static boolean entityIsOnGround(Entity ent) {
        return entityIsOnGround(ent, 0.5);
    }

    public static boolean entityIsDirectlyOnGround(Entity ent) {
        return entityIsOnGround(ent, 0.01);
    }

    public static boolean entityIsOnGround(Entity ent, double distance) {
        if (ent == null) {
            return false;
        }

        if (ent.isOnGround()) {
            return true;
        }

        World world = ent.getWorld();
        Location feetLocation = ent.getLocation().subtract(0, distance, 0);

        // Check the block below the feet
        Material blockBelow = world.getBlockAt(feetLocation).getType();
        if (blockBelow == Material.LILY_PAD) {
            return true;
        }
        Material beneath = ent.getLocation().add(0, -1.5, 0).getBlock().getType();
        if (ent.getLocation().getY() % 0.5 == 0 &&
                (beneath.toString().contains("FENCE") || beneath.toString().contains("WALL"))) {
            return true;
        } else {
            return blockBelow.isSolid();
        }
    }

}

package dev.xkittyqueenx.mobFightersDeluxe.managers;

import dev.xkittyqueenx.mobFightersDeluxe.MobFightersDeluxe;
import dev.xkittyqueenx.mobFightersDeluxe.abilities.Ability;
import dev.xkittyqueenx.mobFightersDeluxe.attributes.Attribute;
import dev.xkittyqueenx.mobFightersDeluxe.events.SmashDamageEvent;
import dev.xkittyqueenx.mobFightersDeluxe.fighters.Fighter;
import dev.xkittyqueenx.mobFightersDeluxe.managers.ownerevents.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.*;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

public class EventManager implements Listener {

    private static EventManager ourInstance;
    private Plugin plugin = MobFightersDeluxe.getInstance();

    public EventManager() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        ourInstance = this;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        Ability ability = FighterManager.getCurrentAbility(player);
        if (ability == null) {
            return;
        }
        if (ability instanceof OwnerRightClickEvent rightClick && (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK)) {
            rightClick.onOwnerRightClick(e);
        }
    }

    // left click in adventure mode for skeleton + zombie
    // the reason why it randomly doesn't work sometimes
    @EventHandler
    public void onPlayerLeftClick(PlayerAnimationEvent e) {
        Player player = e.getPlayer();
        Ability ability = FighterManager.getCurrentAbility(player);
        if (ability == null) {
            return;
        }
        if (ability instanceof OwnerLeftClickEvent && (e.getAnimationType() == PlayerAnimationType.ARM_SWING)) {
            OwnerLeftClickEvent leftClick = (OwnerLeftClickEvent) ability;
            leftClick.onOwnerLeftClick(e);
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent e) {
        Player player = e.getPlayer();
        if(FighterManager.getPlayerFighters().get(player) == null) {
            return;
        }
        List<Attribute> attributes = FighterManager.getPlayerFighters().get(player).getAttributes();
        List<OwnerDropItemEvent> to_call = new ArrayList<>();
        for (Attribute attribute : attributes) {
            if (attribute instanceof OwnerDropItemEvent dropEvent) {
                to_call.add(dropEvent);
            }
        }
        for(OwnerDropItemEvent dropEvent : to_call) {
            dropEvent.onOwnerDropItem(e);
        }
    }

    @EventHandler
    public void onToggleSneak(PlayerToggleSneakEvent e) {
        Player player = e.getPlayer();
        if(FighterManager.getPlayerFighters().get(player) == null) {
            return;
        }
        List<Attribute> attributes = FighterManager.getPlayerFighters().get(player).getAttributes();
        for (Attribute attribute : attributes) {
            if (attribute instanceof OwnerToggleSneakEvent sneakEvent) {
                sneakEvent.onOwnerToggleSneak(e);
            }
        }
    }

    @EventHandler
    public void onPlayerToggleOffHand(PlayerSwapHandItemsEvent e) {
        Player player = e.getPlayer();
        if (FighterManager.getPlayerFighters().get(player) == null) return;
        List<Attribute> attributes = FighterManager.getPlayerFighters().get(player).getAttributes();
        for (Attribute attribute : attributes) {
            if (attribute instanceof OwnerToggleOffHandEvent offHandEvent) {
                offHandEvent.onOwnerToggleOffHand(e);
            }
        }
    }

    @EventHandler
    public void onPlayerSwapHotbar(PlayerItemHeldEvent e) {
        Player player = e.getPlayer();
        if (FighterManager.getPlayerFighters().get(player) == null) return;
        List<Attribute> attributes = FighterManager.getPlayerFighters().get(player).getAttributes();
        for (Attribute attribute : attributes) {
            if (attribute instanceof OwnerSwapHotbarEvent swapHotbar) {
                swapHotbar.onOwnerSwapHotbar(e);
            }
        }
    }

    @EventHandler
    public void onSmashDamage(SmashDamageEvent e) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        Entity damagee = e.getDamagee();
        Entity damager = e.getDamager();
        if (damager instanceof Player) {
            Player player = (Player) damager;
            if(FighterManager.getPlayerFighters().get(player) == null) {
                return;
            }
            List<Attribute> attributes = FighterManager.getPlayerFighters().get(player).getAttributes();
            for (Attribute attribute : attributes) {
                if (attribute instanceof OwnerDealSmashDamageEvent damageEntityEvent) {
                    damageEntityEvent.onOwnerDealSmashDamageEvent(e);
                }
            }
        }
        if(damagee instanceof Player player) {
            if(FighterManager.getPlayerFighters().get(player) == null) {
                return;
            }
            List<Attribute> attributes = FighterManager.getPlayerFighters().get(player).getAttributes();
            for (Attribute attribute : attributes) {
                if (attribute instanceof OwnerTakeSmashDamageEvent damageOwnerEvent) {
                    damageOwnerEvent.onOwnerTakeSmashDamageEvent(e);
                }
            }
        }
    }

    @EventHandler
    public void checkFiredBow(EntityShootBowEvent e) {
        if (e.getEntity() instanceof Player player) {
            Fighter fighter = FighterManager.getPlayerFighters().get(player);
            if (fighter.isActive()) {
                return;
            }
            e.setCancelled(true);
        }
    }

    public static EventManager getInstance() { return ourInstance; }

}

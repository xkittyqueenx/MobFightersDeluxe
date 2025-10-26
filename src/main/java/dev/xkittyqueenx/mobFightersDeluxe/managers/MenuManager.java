package dev.xkittyqueenx.mobFightersDeluxe.managers;

import dev.xkittyqueenx.mobFightersDeluxe.MobFightersDeluxe;
import dev.xkittyqueenx.mobFightersDeluxe.managers.smashmenu.MenuRunnable;
import dev.xkittyqueenx.mobFightersDeluxe.managers.smashmenu.SmashMenu;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;

public class MenuManager implements Listener {

    public static MenuManager ourInstance;
    private static Plugin plugin = MobFightersDeluxe.getInstance();
    private static HashMap<Player, SmashMenu> player_menus = new HashMap<>();

    public MenuManager() {
        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
        ourInstance = this;
    }

    public static SmashMenu createPlayerMenu(Player player, Inventory inventory) {
        SmashMenu menu = new SmashMenu(inventory);
        player_menus.put(player, menu);
        return menu;
    }

    public static SmashMenu getCurrentPlayerMenu(Player player) {
        return player_menus.get(player);
    }

    public static void removePlayerFromMenu(Player player) {
        player_menus.remove(player);
    }

    @EventHandler
    public void onPlayerClick(InventoryClickEvent e) {
        if(!(e.getWhoClicked() instanceof Player player)) {
            return;
        }
        SmashMenu menu = player_menus.get(player);
        if(menu == null || menu.getInventory() == null || !menu.getInventory().equals(e.getClickedInventory())) {
            return;
        }
        MenuRunnable runnable = menu.getActionFromSlot(e.getRawSlot());
        if(runnable != null) {
            runnable.run(e);
        }
    }

    @EventHandler
    public void onPlayerCloseInventory(InventoryCloseEvent e) {
        if(!(e.getPlayer() instanceof Player player)) {
            return;
        }
        if (e.getReason() == InventoryCloseEvent.Reason.OPEN_NEW) {
            return;
        }
        player_menus.remove(player);
    }

}

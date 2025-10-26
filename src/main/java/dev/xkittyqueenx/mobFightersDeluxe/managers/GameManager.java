package dev.xkittyqueenx.mobFightersDeluxe.managers;

import dev.xkittyqueenx.mobFightersDeluxe.MobFightersDeluxe;
import dev.xkittyqueenx.mobFightersDeluxe.managers.gamemodes.SmashGamemode;
import dev.xkittyqueenx.mobFightersDeluxe.managers.smashmenu.SmashMenu;
import dev.xkittyqueenx.mobFightersDeluxe.managers.smashserver.SmashServer;
import io.papermc.paper.datacomponent.DataComponentTypes;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class GameManager implements Listener {

    private static Plugin plugin = MobFightersDeluxe.getInstance();
    public static List<SmashServer> servers = new ArrayList<>();

    public GameManager() {
        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public static SmashServer createSmashServer(SmashGamemode gamemode) {
        if(gamemode == null) {
            gamemode = new SmashGamemode();
        }
        SmashServer server = new SmashServer();
        server.setCurrentGamemode(gamemode);
        servers.add(server);
        return server;
    }

    public static void deleteSmashServer(SmashServer server) {
        if(server.getLobbyMap() != null) {
            server.getLobbyMap().deleteWorld();
        }
        server.getCurrentGamemode().deleteMaps();
        server.getScoreboard().server = null;
        servers.remove(server);
    }

    public static SmashServer getPlayerServer(Player player) {
        for(SmashServer server : servers) {
            if(server.players.contains(player)) {
                return server;
            }
        }
        return null;
    }

    public static boolean isAlive(Player player) {
        SmashServer server = getPlayerServer(player);
        return (server != null && server.lives.containsKey(player));
    }

    public static void teleportPlayerToLobby(Player player) {
        Location location = Bukkit.getWorlds().getFirst().getSpawnLocation();
        player.teleport(location);
    }

    public static void openServerMenu(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 9 * 6, "Select a Server");
        SmashMenu menu = MenuManager.createPlayerMenu(player, inventory);

        int slot = 0;
        for (SmashServer server : servers) {
            ItemStack itemStack = new ItemStack(Material.EMERALD_BLOCK);
            itemStack.setData(DataComponentTypes.ITEM_NAME, MiniMessage.miniMessage().deserialize(server.getCurrentGamemode().getShortName()).decoration(TextDecoration.ITALIC, false));
            inventory.setItem(slot, itemStack);
            menu.setActionFromSlot(slot, (e -> {
                if (!(e.getWhoClicked() instanceof Player clicked)) return;
                clicked.closeInventory();
                server.teleportToServer(clicked);
            }));
            slot++;
        }

        player.openInventory(inventory);
    }

    @EventHandler
    public void PlayerJoin(PlayerJoinEvent e) {
        teleportPlayerToLobby(e.getPlayer());
        FighterManager.unequipPlayer(e.getPlayer());
        e.getPlayer().setHealth(Objects.requireNonNull(e.getPlayer().getAttribute(Attribute.MAX_HEALTH)).getBaseValue());
        e.getPlayer().setFoodLevel(20);
        e.getPlayer().setGameMode(GameMode.ADVENTURE);
    }

}

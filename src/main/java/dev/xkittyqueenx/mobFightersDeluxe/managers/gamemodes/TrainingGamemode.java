package dev.xkittyqueenx.mobFightersDeluxe.managers.gamemodes;

import dev.xkittyqueenx.mobFightersDeluxe.events.GameStateChangeEvent;
import dev.xkittyqueenx.mobFightersDeluxe.fighters.Fighter;
import dev.xkittyqueenx.mobFightersDeluxe.managers.FighterManager;
import dev.xkittyqueenx.mobFightersDeluxe.managers.gamestate.GameState;
import dev.xkittyqueenx.mobFightersDeluxe.managers.maps.GameMap;
import dev.xkittyqueenx.mobFightersDeluxe.utilities.MapPool;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.WeakHashMap;

public class TrainingGamemode extends SmashGamemode {

    public HashMap<Player, Fighter> preferredFighter = new HashMap<>();

    public TrainingGamemode(MapPool mapPool) {
        super();
        this.name = "Training";
        this.short_name = "TRAINING";
        this.maps_folder_name = mapPool;
        this.description = List.of(
                mm.deserialize("<gray>This server is for training."),
                mm.deserialize(" "),
                mm.deserialize("<gray>Type <#A7C7E7>/fighter<gray> or click <#A7C7E7><click:run_command:'/mf fighter'>here</click> <gray>to change fighters."),
                mm.deserialize("<gray>Type <#A7C7E7>/hub<gray> or click <#A7C7E7><click:run_command:'/mf hub'>here</click> <gray>to go back to lobby.")
        );
        this.players_to_start = 1;
        this.max_players = 99;
    }

    @Override
    public void setPlayerLives(WeakHashMap<Player, Integer> lives) {
        for(Player player : server.players) {
            lives.put(player, 99);
        }
    }

    public void updatePlayerKit(Player player, Fighter kit) {
        FighterManager.equipPlayer(player, kit);
    }

    @Override
    public void updateAllowedMaps() {
        try {
            allowed_maps.clear();
            File maps_folder = new File("maps");
            if (!maps_folder.exists()) {
                if (!maps_folder.mkdir()) {
                    Bukkit.broadcastMessage(ChatColor.RED + "Failed to make Maps Folder");
                }
            }
            File gamemode_maps_folder = new File("maps/" + maps_folder_name.getMapPool());
            if (!gamemode_maps_folder.exists()) {
                if (!gamemode_maps_folder.mkdir()) {
                    Bukkit.broadcastMessage(ChatColor.RED + "Failed to make Maps Folder: " + maps_folder_name.getMapPool());
                }
            }
            File[] files = gamemode_maps_folder.listFiles();
            if (files == null) {
                return;
            }
            for (File file : files) {
                if (!file.isDirectory()) {
                    continue;
                }
                File region_directory = new File(file.getPath() + "/region");
                if (!region_directory.exists()) {
                    continue;
                }
                allowed_maps.add(new GameMap(file));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void update() {
        if (server.getState() == GameState.GAME_PLAYING) {
            for (Player player : server.players) {
                server.lives.put(player, 99);
                Fighter fighter = FighterManager.getPlayerFighters().get(player);
                if(fighter == null) {
                    if (!preferredFighter.containsKey(player)) {
                        FighterManager.equipPlayer(player, allowed_fighters.getFirst());
                    } else {
                        FighterManager.equipPlayer(player, preferredFighter.get(player));
                    }
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        player.closeInventory(InventoryCloseEvent.Reason.OPEN_NEW);
                        player.performCommand("fighter");
                    }, 20L);
                } else {
                    preferredFighter.put(player, fighter);
                }
                player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, 20, 20, false, false));
            }
        }
    }

    @Override
    public boolean isGameEnded(WeakHashMap<Player, Integer> lives) {
        return (lives.size() <= 0);
    }

    @EventHandler
    public void onStateChange(GameStateChangeEvent e) {
        if (this.server == null) {
            return;
        }
        server.setTimeLeft(0);
    }

}

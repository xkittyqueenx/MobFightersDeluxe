package dev.xkittyqueenx.mobFightersDeluxe.managers.gamemodes;

import dev.xkittyqueenx.mobFightersDeluxe.MobFightersDeluxe;
import dev.xkittyqueenx.mobFightersDeluxe.fighters.Fighter;
import dev.xkittyqueenx.mobFightersDeluxe.fighters.SpectatorFighter;
import dev.xkittyqueenx.mobFightersDeluxe.fighters.original.SkeletonFighter;
import dev.xkittyqueenx.mobFightersDeluxe.fighters.original.ZombieFighter;
import dev.xkittyqueenx.mobFightersDeluxe.managers.FighterManager;
import dev.xkittyqueenx.mobFightersDeluxe.managers.maps.GameMap;
import dev.xkittyqueenx.mobFightersDeluxe.managers.scoreboard.SmashScoreboard;
import dev.xkittyqueenx.mobFightersDeluxe.managers.smashserver.SmashServer;
import dev.xkittyqueenx.mobFightersDeluxe.utilities.MapPool;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.io.File;
import java.util.*;

public class SmashGamemode implements Listener {

    protected String name = "N/A";
    protected String short_name = "N/A";
    public MapPool maps_folder_name = MapPool.SSM;
    protected List<Component> description = new ArrayList<>();
    protected List<GameMap> allowed_maps = new ArrayList<>();
    protected List<Fighter> allowed_fighters = new ArrayList<>();
    protected int players_to_start = 2;
    public int max_players = 4;
    public SmashServer server = null;
    protected Plugin plugin;

    public static MiniMessage mm = MiniMessage.miniMessage();

    public SmashGamemode() {
        this.plugin = MobFightersDeluxe.getInstance();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void updateAllowedKits() {
        allowed_fighters.clear();
        allowed_fighters.add(new SkeletonFighter());
        allowed_fighters.add(new ZombieFighter());
    }

    public void updateAllowedMaps() {
        try {
            allowed_maps.clear();
            File maps_folder = new File("maps");
            if (!maps_folder.exists()) {
                if (!maps_folder.mkdir()) {
                    Bukkit.broadcastMessage(ChatColor.RED + "Failed to make dev.urpcketgf.mobfighters.Main Maps Folder");
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

    public void setPlayerLives(WeakHashMap<Player, Integer> lives) {
        for (Player player : server.players) {
            lives.put(player, 4);
        }
    }

    public void setPlayerKit(Player player) {
        Fighter fighter = FighterManager.getPlayerFighters().get(player);
        if (fighter == null) {
            FighterManager.equipPlayer(player, allowed_fighters.getFirst());
        }
    }

    public void update() {
        return;
    }

    public void deleteMaps() {
        for (GameMap map : allowed_maps) {
            map.deleteWorld();
        }
        allowed_maps.clear();
    }

    public Location getRandomRespawnPoint(Player player) {
        if (server.getGameMap().getRespawnPoints().isEmpty()) {
            return server.getGameMap().getWorld().getSpawnLocation();
        }
        // Calculate closest player to each respawn point, pick the one furthest from players
        // If they are on a team pick the one closest to their teammates
        HashMap<Location, Double> closest_enemy_distance = new HashMap<Location, Double>();
        double max_distance_check = 1000;
        double maximum_enemy_distance = 0;
        for (Location respawn_point : server.getGameMap().getRespawnPoints()) {
            double closest_enemy = max_distance_check;
            for (Player check : server.getGameMap().getWorld().getPlayers()) {
                if (player.equals(check)) {
                    continue;
                }
                if (!server.lives.containsKey(check)) {
                    continue;
                }
                Fighter fighter = FighterManager.getPlayerFighters().get(check);
                if(fighter instanceof SpectatorFighter) {
                    continue;
                }
                closest_enemy = Math.min(closest_enemy, respawn_point.distance(check.getLocation()));
            }
            maximum_enemy_distance = Math.max(maximum_enemy_distance, closest_enemy);
            closest_enemy_distance.put(respawn_point, closest_enemy);
        }
        Location selected_point = null;
        for (Location respawn_point : closest_enemy_distance.keySet()) {
            if (closest_enemy_distance.get(respawn_point) >= maximum_enemy_distance) {
                selected_point = respawn_point;
            }
        }
        if (selected_point == null || maximum_enemy_distance >= 1000) {
            selected_point = server.getGameMap().getRespawnPoints().get((int) (Math.random() * server.getGameMap().getRespawnPoints().size()));
        }
        // Face towards map center
        Vector difference = server.getGameMap().getWorld().getSpawnLocation().toVector().subtract(selected_point.toVector());
        if (Math.abs(difference.getX()) > Math.abs(difference.getZ())) {
            selected_point.setDirection(new Vector(difference.getX(), 0, 0));
        } else {
            selected_point.setDirection(new Vector(0, 0, difference.getZ()));
        }
        return selected_point;
    }

    public List<String> getLivesScoreboard() {
        // This is less terrible but still forgive my laziness
        List<Player> least_to_greatest = new ArrayList<Player>();
        HashMap<Player, Integer> lives_copy = new HashMap<Player, Integer>(server.lives);
        while (!lives_copy.isEmpty()) {
            int min_value = 0;
            Player min_player = null;
            for (Player check : lives_copy.keySet()) {
                if (min_player == null) {
                    min_player = check;
                    min_value = lives_copy.get(check);
                    continue;
                }
                if (lives_copy.get(check) < min_value) {
                    min_player = check;
                    min_value = lives_copy.get(check);
                }
            }
            least_to_greatest.add(min_player);
            lives_copy.remove(min_player);
        }
        List<String> scoreboard_string = new ArrayList<String>();
        for (Player add : least_to_greatest) {
            Fighter fighter = FighterManager.getPlayerFighters().get(add);
            String glyph = "";
            if (fighter != null) {
                glyph = fighter.getGlyph();
            }
            if (!(server.getCurrentGamemode() instanceof TrainingGamemode)) {
                scoreboard_string.add(SmashScoreboard.getPlayerColor(add, true) + glyph + " " + add.getName() + " " + server.getLives(add));
            } else {
                scoreboard_string.add(SmashScoreboard.getPlayerColor(add, true) + glyph + " " + add.getName());
            }
        }
        return scoreboard_string;
    }

    public void setPlayerLobbyItems(Player player) {
        player.getInventory().setItem(0, MobFightersDeluxe.KIT_SELECTOR_ITEM);
        player.getInventory().setItem(1, MobFightersDeluxe.VOTING_MENU_ITEM);
        player.getInventory().setItem(8, MobFightersDeluxe.TELEPORT_HUB_ITEM);
    }

    public void afterGameEnded(Player player) {
        Objects.requireNonNull(player.getAttribute(Attribute.BLOCK_INTERACTION_RANGE)).setBaseValue(3);
        return;
    }

    public boolean isGameEnded(WeakHashMap<Player, Integer> lives) {
        return (lives.size() <= 1);
    }

    public String getName() {
        return name;
    }

    public String getShortName() {
        return short_name;
    }

    public String getMapPool() {
        return maps_folder_name.getMapPool();
    }

    public List<Component> getDescription() {
        return description;
    }

    public List<Fighter> getAllowedFighters() {
        return allowed_fighters;
    }

    public List<GameMap> getAllowedMaps() {
        return allowed_maps;
    }

    public int getPlayersToStart() {
        return players_to_start;
    }

}

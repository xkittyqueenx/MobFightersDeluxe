package dev.xkittyqueenx.mobFightersDeluxe.managers.smashserver;

import dev.xkittyqueenx.mobFightersDeluxe.MobFightersDeluxe;
import dev.xkittyqueenx.mobFightersDeluxe.abilities.Ability;
import dev.xkittyqueenx.mobFightersDeluxe.attributes.Attribute;
import dev.xkittyqueenx.mobFightersDeluxe.attributes.Ultimate;
import dev.xkittyqueenx.mobFightersDeluxe.events.GameStateChangeEvent;
import dev.xkittyqueenx.mobFightersDeluxe.events.PlayerLostLifeEvent;
import dev.xkittyqueenx.mobFightersDeluxe.events.SmashDamageEvent;
import dev.xkittyqueenx.mobFightersDeluxe.fighters.Fighter;
import dev.xkittyqueenx.mobFightersDeluxe.fighters.SpectatorFighter;
import dev.xkittyqueenx.mobFightersDeluxe.managers.DamageManager;
import dev.xkittyqueenx.mobFightersDeluxe.managers.FighterManager;
import dev.xkittyqueenx.mobFightersDeluxe.managers.GameManager;
import dev.xkittyqueenx.mobFightersDeluxe.managers.MenuManager;
import dev.xkittyqueenx.mobFightersDeluxe.managers.gamemodes.SmashGamemode;
import dev.xkittyqueenx.mobFightersDeluxe.managers.gamemodes.TrainingGamemode;
import dev.xkittyqueenx.mobFightersDeluxe.managers.gamestate.GameState;
import dev.xkittyqueenx.mobFightersDeluxe.managers.maps.GameMap;
import dev.xkittyqueenx.mobFightersDeluxe.managers.maps.LobbyMap;
import dev.xkittyqueenx.mobFightersDeluxe.managers.ownerevents.OwnerDeathEvent;
import dev.xkittyqueenx.mobFightersDeluxe.managers.ownerevents.OwnerKillEvent;
import dev.xkittyqueenx.mobFightersDeluxe.managers.scoreboard.SmashScoreboard;
import dev.xkittyqueenx.mobFightersDeluxe.managers.smashmenu.SmashMenu;
import dev.xkittyqueenx.mobFightersDeluxe.utilities.DamageUtil;
import dev.xkittyqueenx.mobFightersDeluxe.utilities.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.DisplaySlot;

import java.io.File;
import java.util.*;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.GRAY;
import static net.kyori.adventure.text.format.TextColor.color;

public class SmashServer implements Listener, Runnable {

    private static Plugin plugin = MobFightersDeluxe.getInstance();
    private short state = GameState.LOBBY_WAITING;
    private long time_remaining_ms = 0;
    protected SmashScoreboard scoreboard;
    protected SmashGamemode current_gamemode;
    protected LobbyMap lobby_map;
    public GameMap game_map = null;
    public List<Player> players = new ArrayList<>();
    public WeakHashMap<Player, Integer> lives = new WeakHashMap<>();
    public Player[] deaths = new Player[2];

    public WeakHashMap<Player, Fighter> pre_selected_fighters = new WeakHashMap<>();
    public WeakHashMap<Player, Fighter> selected_fighter = new WeakHashMap<>();

    public static MiniMessage mm = MiniMessage.miniMessage();

    public SmashServer() {
        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
        scoreboard = new SmashScoreboard();
        scoreboard.server = this;
        File lobby_directory = new File("maps/lobby_world");
        lobby_map = new LobbyMap(lobby_directory);
        lobby_map.createWorld();
        Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this, 0L, 0L);
        setState(GameState.LOBBY_WAITING);
    }

    @Override
    public void run() {
        current_gamemode.update();
        scoreboard.buildScoreboard();
        if (state == GameState.LOBBY_WAITING) {
            doLobbyWaiting();
            return;
        }
        if (state == GameState.LOBBY_VOTING) {
            doLobbyVoting();
            return;
        }
        if (state == GameState.LOBBY_STARTING) {
            doLobbyStarting();
            return;
        }
        if (state == GameState.GAME_STARTING) {
            doGameStarting();
            return;
        }
        if (state == GameState.GAME_PLAYING) {
            doGamePlaying();
            return;
        }
        if (state == GameState.GAME_ENDING) {
            doGameEnding();
            return;
        }
    }

    private void doLobbyWaiting() {
        for(Player player : players) {
            Fighter fighter = FighterManager.getPlayerFighters().get(player);
            if(fighter != null) {
                FighterManager.unequipPlayer(player);
                setPlayerLobbyItems(player);
            }
        }
        if (getActivePlayerCount() >= current_gamemode.getPlayersToStart()) {
            setTimeLeft(30);
            setState(GameState.LOBBY_VOTING);
            return;
        }
    }

    private void doLobbyVoting() {
        if (getActivePlayerCount() < current_gamemode.getPlayersToStart()) {
            setState(GameState.LOBBY_WAITING);
            return;
        }
        if (time_remaining_ms <= 0) {
            if(game_map != null) {
                game_map.unloadWorld();
            }
            game_map = getChosenMap();
            for (Player player : players) {
                player.sendMessage(MiniMessage.miniMessage().deserialize("<#A7C7E7><b>" + game_map.getName() + " won the vote!"));
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_GUITAR, 1, 1);
                Fighter pre_selected_fighter = pre_selected_fighters.get(player);
                if (pre_selected_fighter != null) {
                    FighterManager.equipPlayer(player, pre_selected_fighter);
                }
            }
            game_map.createWorld();
            setTimeLeft(10);
            setState(GameState.LOBBY_STARTING);
            return;
        }
        time_remaining_ms -= 50;
    }

    private void doLobbyStarting() {
        if (getActivePlayerCount() < current_gamemode.getPlayersToStart()) {
            setState(GameState.LOBBY_WAITING);
            return;
        }
        if (time_remaining_ms <= 0) {
            current_gamemode.setPlayerLives(lives);
            if (game_map == null) {
                return;
            }
            pre_selected_fighters.clear();
            for (GameMap map : current_gamemode.getAllowedMaps()) {
                map.clearVoted();
            }
            for (Player player : players) {
                Location starting_point = current_gamemode.getRandomRespawnPoint(player);
                player.teleport(starting_point);
            }
            for (Player player : players) {
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
                for (int i = 0; i < 6 - current_gamemode.getDescription().size(); i++) {
                    player.sendMessage("");
                }
                player.sendMessage(ChatColor.DARK_GREEN + "" + ChatColor.STRIKETHROUGH + "=============================================");
                player.sendMessage(MiniMessage.miniMessage().deserialize("<#F4BB44>Game - <gold><b>" + current_gamemode.getName()));
                player.sendMessage("");
                for (Component description : current_gamemode.getDescription()) {
                    player.sendMessage(description);
                }
                player.sendMessage("");
                player.sendMessage(game_map.toComponent());
                player.sendMessage(ChatColor.DARK_GREEN + "" + ChatColor.STRIKETHROUGH + "=============================================");
            }
            Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                @Override
                public void run() {
                    for (Player player : players) {
                        Fighter fighter = FighterManager.getPlayerFighters().get(player);
                        if (fighter == null) {
                            // If the player has no kit, assign one
                            current_gamemode.setPlayerKit(player);
                        }
                    }
                }
            }, 20L);
            setTimeLeft(10);
            setState(GameState.GAME_STARTING);
            return;
        }
        for (Player player : players) {
            if (time_remaining_ms % 1000 == 0 && time_remaining_ms <= 4000) {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
            }
        }
        time_remaining_ms -= 50;
    }

    private void doGameStarting() {
        if (getActivePlayerCount() <= 0) {
            setTimeLeft(0);
            setState(GameState.GAME_ENDING);
            return;
        }
        if (time_remaining_ms <= 0) {
            setState(GameState.GAME_PLAYING);
            for (Player player : players) {
                Fighter fighter = FighterManager.getPlayerFighters().get(player);
                if (fighter != null) {
                    fighter.updatePlaying(GameState.GAME_PLAYING, true);
                }
            }
            return;
        }
        for (Player player : players) {
            int barLength = 24;
            int startRedBarInterval = barLength - (int) ((time_remaining_ms / (double) 10000) * barLength);
            StringBuilder sb = new StringBuilder("      <white>Game Start ");
            for (int i = 0; i < barLength; i++) {
                if (i < startRedBarInterval) {
                    sb.append("<green>▌");
                } else {
                    sb.append("<red>▌");
                }
            }
            sb.append(" <white>");
            sb.append(Utils.msToSeconds(time_remaining_ms));
            sb.append(" Seconds");
            Utils.sendActionBarMessage(sb.toString(), player);
            if (time_remaining_ms % 1000 == 0) {
                player.playSound(player, Sound.UI_BUTTON_CLICK, 1f, 1f);
            }
        }
        time_remaining_ms -= 50;
    }

    private void doGamePlaying() {
        if (getActivePlayerCount() <= 0) {
            setTimeLeft(0);
            setState(GameState.GAME_ENDING);
            return;
        }
        if (current_gamemode.isGameEnded(lives)) {
            for (Player player : players) {
                current_gamemode.afterGameEnded(player);
            }
            lives.clear();
            deaths = new Player[2];
            setTimeLeft(10);
            setState(GameState.GAME_ENDING);
            return;
        }
    }

    private void doGameEnding() {
        if (time_remaining_ms <= 0) {
            lives.clear();
            for (Player player : players) {
                FighterManager.unequipPlayer(player);
                player.teleport(lobby_map.getWorld().getSpawnLocation());
            }
            game_map.unloadWorld();
            game_map = null;
            setState(GameState.LOBBY_WAITING);
            return;
        }
        time_remaining_ms -= 50;
    }

    public void death(Player player) {
        player.getInventory().close();
        player.sendEntityEffect(EntityEffect.ENTITY_DEATH, player);
        Fighter kit = FighterManager.getPlayerFighters().get(player);
        String glyph;
        String role;
        SmashServer server = GameManager.getPlayerServer(player);
        if (server == null) {
            return;
        }
        if (kit != null) {
            glyph = kit.getGlyph();
        } else {
            glyph = "";
        }
        if (lives.get(player) == null) {
            return;
        }
        Map<Player, Float> ultimateChargeMap = new HashMap<>();
        if (kit != null) {
            for (Attribute attribute : kit.getAttributes()) {
                if (attribute instanceof Ultimate) {
                    Ultimate ultimate = (Ultimate) attribute;
                    ultimateChargeMap.put(player, ultimate.ultimateCharge);
                }
            }
        }
        SmashDamageEvent record = DamageManager.getLastDamageEvent(player);
        for(Player message : players) {
            if (record.getDamager() != null && record.getDamager() instanceof Player damager) {
                Fighter damager_kit = FighterManager.getPlayerFighters().get(damager);
                if (damager_kit == null) {
                    String output = record.getReason();
                    TextComponent ability = text(output);
                    final Component component = text()
                            .content("DEATH> ").color(color(0xaa4a44))
                            .append(text(glyph + " ").color(color(255, 255, 255)))
                            .append(text(player.getName()).color(color(0xa7c7e7)))
                            .append(text(" killed by ", GRAY))
                            .append(text(damager.getName()).color(color(0xfaa0a0)))
                            .append(text(" with ", GRAY))
                            .append(ability).color(color(0xf4bb44))
                            .append(text(".", GRAY))
                            .build();
                    message.sendMessage(component);
                    return;
                }
                Ability ability1 = damager_kit.getAbilityInSlot(0);
                Ability ability2 = damager_kit.getAbilityInSlot(1);
                Ability ability3 = damager_kit.getAbilityInSlot(2);
                Ability ability4 = damager_kit.getAbilityInSlot(4);
                if (ability1 != null && ability1.name.equalsIgnoreCase(record.getReason())) {
                    ItemStack itemStack = damager.getInventory().getItem(0);
                    if (itemStack != null) {
                        String output = record.getReason();
                        TextComponent ability = text(output).hoverEvent(itemStack);
                        final Component component = text()
                                .content("DEATH> ").color(color(0xaa4a44))
                                .append(text(glyph + " ").color(color(255, 255, 255)))
                                .append(text(player.getName()).color(color(0xa7c7e7)))
                                .append(text(" killed by ", GRAY))
                                .append(text(damager_kit.getGlyph() + " "))
                                .append(text(damager.getName()).color(color(0xfaa0a0)))
                                .append(text(" with ", GRAY))
                                .append(ability).color(color(0xf4bb44))
                                .append(text(".", GRAY))
                                .build();
                        message.sendMessage(component);
                    } else {
                        String output = record.getReason();
                        TextComponent ability = text(output);
                        final Component component = text()
                                .content("DEATH> ").color(color(0xaa4a44))
                                .append(text(glyph + " ").color(color(255, 255, 255)))
                                .append(text(player.getName()).color(color(0xa7c7e7)))
                                .append(text(" killed by ", GRAY))
                                .append(text(damager_kit.getGlyph() + " "))
                                .append(text(damager.getName()).color(color(0xfaa0a0)))
                                .append(text(" with ", GRAY))
                                .append(ability).color(color(0xf4bb44))
                                .append(text(".", GRAY))
                                .build();
                        message.sendMessage(component);
                    }
                } else if (ability2 != null && ability2.name.equalsIgnoreCase(record.getReason())) {
                    ItemStack itemStack = damager.getInventory().getItem(1);
                    if (itemStack != null) {
                        String output = record.getReason();
                        TextComponent ability = text(output).hoverEvent(itemStack);
                        final Component component = text()
                                .content("DEATH> ").color(color(0xaa4a44))
                                .append(text(glyph + " ").color(color(255, 255, 255)))
                                .append(text(player.getName()).color(color(0xa7c7e7)))
                                .append(text(" killed by ", GRAY))
                                .append(text(damager_kit.getGlyph() + " "))
                                .append(text(damager.getName()).color(color(0xfaa0a0)))
                                .append(text(" with ", GRAY))
                                .append(ability).color(color(0xf4bb44))
                                .append(text(".", GRAY))
                                .build();
                        message.sendMessage(component);
                    } else {
                        String output = record.getReason();
                        TextComponent ability = text(output);
                        final Component component = text()
                                .content("DEATH> ").color(color(0xaa4a44))
                                .append(text(glyph + " ").color(color(255, 255, 255)))
                                .append(text(player.getName()).color(color(0xa7c7e7)))
                                .append(text(" killed by ", GRAY))
                                .append(text(damager_kit.getGlyph() + " "))
                                .append(text(damager.getName()).color(color(0xfaa0a0)))
                                .append(text(" with ", GRAY))
                                .append(ability).color(color(0xf4bb44))
                                .append(text(".", GRAY))
                                .build();
                        message.sendMessage(component);
                    }
                } else if (ability3 != null && ability3.name.equalsIgnoreCase(record.getReason())) {
                    ItemStack itemStack = damager.getInventory().getItem(2);
                    if (itemStack != null) {
                        String output = record.getReason();
                        TextComponent ability = text(output).hoverEvent(itemStack);
                        final Component component = text()
                                .content("DEATH> ").color(color(0xaa4a44))
                                .append(text(glyph + " ").color(color(255, 255, 255)))
                                .append(text(player.getName()).color(color(0xa7c7e7)))
                                .append(text(" killed by ", GRAY))
                                .append(text(damager_kit.getGlyph() + " "))
                                .append(text(damager.getName()).color(color(0xfaa0a0)))
                                .append(text(" with ", GRAY))
                                .append(ability).color(color(0xf4bb44))
                                .append(text(".", GRAY))
                                .build();
                        message.sendMessage(component);
                    } else {
                        String output = record.getReason();
                        TextComponent ability = text(output);
                        final Component component = text()
                                .content("DEATH> ").color(color(0xaa4a44))
                                .append(text(glyph + " ").color(color(255, 255, 255)))
                                .append(text(player.getName()).color(color(0xa7c7e7)))
                                .append(text(" killed by ", GRAY))
                                .append(text(damager_kit.getGlyph() + " "))
                                .append(text(damager.getName()).color(color(0xfaa0a0)))
                                .append(text(" with ", GRAY))
                                .append(ability).color(color(0xf4bb44))
                                .append(text(".", GRAY))
                                .build();
                        message.sendMessage(component);
                    }
                } else if (ability4 != null && ability4.name.equalsIgnoreCase(record.getReason())) {
                    ItemStack itemStack = damager.getInventory().getItem(4);
                    if (itemStack != null) {
                        String output = record.getReason();
                        TextComponent ability = text(output).hoverEvent(itemStack);
                        final Component component = text()
                                .content("DEATH> ").color(color(0xaa4a44))
                                .append(text(glyph + " ").color(color(255, 255, 255)))
                                .append(text(player.getName()).color(color(0xa7c7e7)))
                                .append(text(" killed by ", GRAY))
                                .append(text(damager_kit.getGlyph() + " "))
                                .append(text(damager.getName()).color(color(0xfaa0a0)))
                                .append(text(" with ", GRAY))
                                .append(ability).color(color(0xf4bb44))
                                .append(text(".", GRAY))
                                .build();
                        message.sendMessage(component);
                    }
                } else {
                    String output = record.getReason();
                    TextComponent ability = text(output);
                    final Component component = text()
                            .content("DEATH> ").color(color(0xaa4a44))
                            .append(text(glyph + " ").color(color(255, 255, 255)))
                            .append(text(player.getName()).color(color(0xa7c7e7)))
                            .append(text(" killed by ", GRAY))
                            .append(text(damager_kit.getGlyph() + " "))
                            .append(text(damager.getName()).color(color(0xfaa0a0)))
                            .append(text(" with ", GRAY))
                            .append(ability).color(color(0xf4bb44))
                            .append(text(".", GRAY))
                            .build();
                    message.sendMessage(component);
                }
            } else {
                String output = record.getReason();
                TextComponent ability = text(output);
                final Component component = text()
                        .content("DEATH> ").color(color(0xaa4a44))
                        .append(text(glyph + " ").color(color(255, 255, 255)))
                        .append(text(player.getName()).color(color(0xa7c7e7)))
                        .append(text(" killed by ", GRAY))
                        .append(text(record.getDamagerName()).color(color(0xfaa0a0)))
                        .append(text(" with ", GRAY))
                        .append(ability).color(color(0xf4bb44))
                        .append(text(".", GRAY))
                        .build();
                message.sendMessage(component);
            }
        }
        DamageManager.deathReport(player, true);
        if (record.getDamager() != null && record.getDamager() instanceof Player damager) {
            Fighter damage = FighterManager.getPlayerFighters().get(damager);
            if (damage != null) {
                List<Attribute> attributes = damage.getAttributes();
                for (Attribute attribute : attributes) {
                    if (attribute instanceof OwnerKillEvent killEvent) {
                        killEvent.onOwnerKillEvent(damager, player, record);
                    }
                }
            }
        }
        if (kit != null) {
            List<Attribute> attributes = kit.getAttributes();
            for (Attribute attribute : attributes) {
                if (attribute instanceof OwnerDeathEvent deathEvent) {
                    deathEvent.onOwnerDeathEvent(player);
                }
            }
        }
        lives.put(player, lives.get(player) - 1);
        PlayerLostLifeEvent playerLostLifeEvent = new PlayerLostLifeEvent(player, lives.get(player));
        Bukkit.getPluginManager().callEvent(playerLostLifeEvent);
        if (lives.get(player) <= 0) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<#FAA0A0><b>You ran out of lives!"));
            player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 2f, 1f);
            lives.remove(player);
            deaths[1] = deaths[0];
            deaths[0] = player;
            FighterManager.equipPlayer(player, new SpectatorFighter());
            player.setGameMode(GameMode.SPECTATOR);
            return;
        }
        player.sendMessage(MiniMessage.miniMessage().deserialize("<#FAA0A0><b>You have died!"));
        if (lives.get(player) > 90) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<#FAA0A0><b>You will respawn soon!"));
        } else {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<#FAA0A0><b>You have " + lives.get(player) + " " + (lives.get(player) == 1 ? "life" : "lives") + " left!"));
        }
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 2f, 0.5f);
        player.setGameMode(GameMode.SPECTATOR);
        FighterManager.equipPlayer(player, new SpectatorFighter(glyph));
        player.setGameMode(GameMode.SPECTATOR);
        FighterManager.getPlayerFighters().get(player).setGlyph("☠ " + glyph);
        Bukkit.getScheduler().runTaskLater(plugin, () -> FighterManager.openKitMenu(player), 20L);
        Bukkit.getScheduler().scheduleSyncDelayedTask(MobFightersDeluxe.getInstance(), new Runnable() {
            @Override
            public void run() {
                if (state == GameState.GAME_PLAYING && getLives(player) > 0) {
                    if (player.getGameMode() != GameMode.SPECTATOR) {
                        return;
                    }
                    player.teleport(current_gamemode.getRandomRespawnPoint(player));
                    if (selected_fighter.containsKey(player)) {
                        Fighter kit_new = selected_fighter.get(player);
                        player.setGameMode(GameMode.ADVENTURE);
                        FighterManager.equipPlayer(player, kit_new);
                        selected_fighter.remove(player);
                    } else {
                        if (kit != null && !kit.getName().equals("Temporary Spectator")) {
                            player.setGameMode(GameMode.ADVENTURE);
                            FighterManager.equipPlayer(player, kit);
                        }
                        if (ultimateChargeMap.containsKey(player)) {
                            if (kit != null && !kit.getName().equals("Temporary Spectator")) {
                                FighterManager.equipPlayer(player, kit, ultimateChargeMap);
                            }
                        } else if (kit != null && !kit.getName().equals("Temporary Spectator")) {
                            FighterManager.equipPlayer(player, kit, ultimateChargeMap);
                        }
                    }
                }
            }
        }, 80L);
    }

    public GameMap getGameMap() {
        return game_map;
    }

    public SmashScoreboard getScoreboard() {
        return scoreboard;
    }

    public SmashGamemode getCurrentGamemode() {
        return current_gamemode;
    }

    public LobbyMap getLobbyMap() {
        return lobby_map;
    }

    public void setGameMap(File file) {
        game_map = new GameMap(file);
    }

    public void setState(short value) {
        GameStateChangeEvent event = new GameStateChangeEvent(this, state, value);
        Bukkit.getPluginManager().callEvent(event);
        boolean changed = (state != value);
        state = value;
        if (changed) {
            run();
        }
    }

    public short getState() {
        return state;
    }

    public boolean isStarting() {
        return GameState.isStarting(state);
    }

    public boolean isPlaying() {
        return GameState.isPlaying(state);
    }

    public void setTimeLeft(double time) {
        time_remaining_ms = (long) (time * 1000.0);
    }

    public int getTimeLeft() {
        return (int) (time_remaining_ms / 1000.0);
    }

    public int getActivePlayerCount() {
        return players.size();
    }

    public int getLives(Player player) {
        if (lives.get(player) == null) {
            return 0;
        }
        return lives.get(player);
    }

    public GameMap getChosenMap() {
        if (current_gamemode.getAllowedMaps().isEmpty()) {
            Bukkit.broadcastMessage("SSM Maps Folder Empty, loading Default World");
            return new GameMap(new File("maps/lobby_world"));
        }
        // Calculate max voted map
        int max = 0;
        for (GameMap map : current_gamemode.getAllowedMaps()) {
            List<Player> votes = map.getVoted();
            if (votes != null && votes.size() > max) {
                max = votes.size();
            }
        }
        if (max == 0) {
            return current_gamemode.getAllowedMaps().get((int) (Math.random() * current_gamemode.getAllowedMaps().size()));
        }
        // Get tied maps
        List<GameMap> tied = new ArrayList<>();
        for (GameMap map : current_gamemode.getAllowedMaps()) {
            List<Player> votes = map.getVoted();
            if (votes.size() >= max) {
                tied.add(map);
            }
        }
        // Choose random from tied list
        return tied.get((int) (Math.random() * tied.size()));
    }

    public void setPlayerLobbyItems(Player player) {
        if(current_gamemode == null) {
            return;
        }
        current_gamemode.setPlayerLobbyItems(player);
    }

    public void openVotingMenu(Player player) {
        List<GameMap> sortedMaps = new ArrayList<>(current_gamemode.getAllowedMaps());
        sortedMaps.sort(Comparator.comparing(GameMap::getName));

        int mapsPerPage = 21;
        int totalPages = (int) Math.ceil((double) sortedMaps.size() / mapsPerPage);
        int currentPage = 1;

        openMapSelectionPage(player, currentPage, totalPages, sortedMaps);
    }

    private void openMapSelectionPage(Player player, int page, int totalPages, List<GameMap> sortedMaps) {
        int size = 6;
        Inventory selectMap = Bukkit.createInventory(player, 9 * size, "Choose a Stage - Page " + page + "/" + totalPages);
        SmashMenu menu = MenuManager.createPlayerMenu(player, selectMap);

        // Calculate which kits to display on this page
        int mapsPerPage = 21;
        int startIndex = (page - 1) * mapsPerPage;
        int endIndex = Math.min(startIndex + mapsPerPage, sortedMaps.size());

        // Display kits for current page
        int solo_slot = 10;
        int count = 0;

        for (int i = startIndex; i < endIndex; i++) {
            GameMap map = sortedMaps.get(i);

            ItemStack item = new ItemStack(
                    map.getVoted().contains(player) ? Material.MAP : Material.PAPER,
                    Math.max(1, map.getVoted().size())
            );
            ItemMeta itemMeta = item.getItemMeta();
            itemMeta.setDisplayName(ChatColor.YELLOW + map.getName());
            item.setItemMeta(itemMeta);

            selectMap.setItem(solo_slot, item);
            final GameMap selectedMap = map;

            menu.setActionFromSlot(solo_slot, (e) -> {
                if (e.getWhoClicked() instanceof Player clicked) {
                    clicked.playSound(clicked.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                    clicked.sendMessage(mm.deserialize("<#D3D3D3>You voted for <yellow>" + selectedMap.getName()));

                    for (GameMap remove : current_gamemode.getAllowedMaps()) {
                        remove.getVoted().remove(clicked);
                    }
                    selectedMap.getVoted().add(clicked);
                    clicked.closeInventory();
                }
            });
            solo_slot++;
            count++;
            if (count % 7 == 0) {
                solo_slot += 2;
            }
        }

        // Add navigation buttons
        // Previous page button
        if (page > 1) {
            ItemStack prevPage = new ItemStack(Material.ARROW);
            ItemMeta prevMeta = prevPage.getItemMeta();
            prevMeta.setDisplayName(ChatColor.GREEN + "Previous Page");
            prevPage.setItemMeta(prevMeta);
            selectMap.setItem(45, prevPage);

            final int prevPageNum = page - 1;
            menu.setActionFromSlot(45, (e) -> {
                if (e.getWhoClicked() instanceof Player clicked) {
                    clicked.playSound(clicked.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                    MenuManager.removePlayerFromMenu(clicked);
                    openMapSelectionPage(clicked, prevPageNum, totalPages, sortedMaps);
                }
            });
        }

        // Next page button
        if (page < totalPages) {
            ItemStack nextPage = new ItemStack(Material.ARROW);
            ItemMeta nextMeta = nextPage.getItemMeta();
            nextMeta.setDisplayName(ChatColor.GREEN + "Next Page");
            nextPage.setItemMeta(nextMeta);
            selectMap.setItem(53, nextPage);

            final int nextPageNum = page + 1;
            menu.setActionFromSlot(53, (e) -> {
                if (e.getWhoClicked() instanceof Player clicked) {
                    clicked.playSound(clicked.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                    MenuManager.removePlayerFromMenu(clicked);
                    openMapSelectionPage(clicked, nextPageNum, totalPages, sortedMaps);
                }
            });
        }

        // Exit button
        ItemStack exit = new ItemStack(Material.BARRIER);
        ItemMeta exitMeta = exit.getItemMeta();
        exitMeta.setDisplayName(ChatColor.RED + "Exit");
        exit.setItemMeta(exitMeta);
        selectMap.setItem(49, exit);
        menu.setActionFromSlot(49, (e) -> {
            if (e.getWhoClicked() instanceof Player clicked) {
                clicked.playSound(clicked.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 1.0f);
                clicked.closeInventory();
            }
        });

        player.openInventory(selectMap);
    }

    public void setCurrentGamemode(SmashGamemode gamemode) {
        if(current_gamemode != null) {
            current_gamemode.deleteMaps();
            current_gamemode.server = null;
            HandlerList.unregisterAll(current_gamemode);
        }
        current_gamemode = gamemode;
        current_gamemode.server = this;
        current_gamemode.updateAllowedKits();
        current_gamemode.updateAllowedMaps();
        Bukkit.getPluginManager().registerEvents(gamemode, MobFightersDeluxe.getInstance());
        // Clear existing entities
        for (Entity entity : lobby_map.getWorld().getEntities()) {
            if (!(entity instanceof Player)) {
                entity.remove();
            }
        }
        // Clear all current map votes
        for (GameMap map : current_gamemode.getAllowedMaps()) {
            map.clearVoted();
        }
        for(Player player : players) {
            Utils.sendActionBarMessage("Set " +
                    "<yellow>" + current_gamemode.getName() + "<gray>" +
                    " as the next gamemode.", player);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1, 1);
        }
        stopGame();
    }

    public void stopGame() {
        if (state >= GameState.GAME_STARTING) {
            setTimeLeft(0);
            setState(GameState.GAME_ENDING);
            for (Player player : players) {
                if (getCurrentGamemode() instanceof TrainingGamemode gamemode) {
                    setPlayerLobbyItems(player);
                    gamemode.updatePlayerKit(player, FighterManager.getPlayerFighters().get(player));
                } else {
                    FighterManager.unequipPlayer(player);
                    setPlayerLobbyItems(player);
                }
            }
        } else {
            setState(GameState.LOBBY_WAITING);
            for (Player player : players) {
                FighterManager.unequipPlayer(player);
                setPlayerLobbyItems(player);
            }
        }
    }

    public void teleportToServer(Player player) {
        if(game_map == null || state < GameState.GAME_STARTING) {
            player.teleport(lobby_map.getWorld().getSpawnLocation());
            return;
        }
        player.teleport(game_map.getWorld().getSpawnLocation());
    }

    @Override
    public String toString() {
        return current_gamemode.getShortName();
    }

    @EventHandler
    public void PlayerMove(PlayerMoveEvent e) {
        if (!isStarting()) {
            if (game_map != null) {
                if (game_map.isOutOfBounds(e.getPlayer())) {
                    DamageUtil.borderKill(e.getPlayer(), true);
                }
            }
            return;
        }
        if (!lives.containsKey(e.getPlayer())) {
            return;
        }
        Location to = e.getTo();
        if (!to.toVector().equals(e.getFrom().toVector())) {
            to = e.getFrom();
        }
        to.setPitch(e.getTo().getPitch());
        to.setYaw(e.getTo().getYaw());
        e.setTo(to);
    }

    @EventHandler
    public void playerQuit(PlayerQuitEvent e) {
        players.remove(e.getPlayer());
        lives.remove(e.getPlayer());
        pre_selected_fighters.remove(e.getPlayer());
        e.getPlayer().getScoreboard().clearSlot(DisplaySlot.SIDEBAR);
        FighterManager.unequipPlayer(e.getPlayer());
        for (Player message : players) {
            message.sendMessage(ChatColor.DARK_GRAY + "Quit> " + ChatColor.GRAY + e.getPlayer().getName());
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void playerLeftWorld(PlayerChangedWorldEvent e) {
        Player player = e.getPlayer();
        SmashServer server = GameManager.getPlayerServer(player);
        selected_fighter.remove(player);
        if(!players.contains(player)) {
            return;
        }
        if(lobby_map != null && player.getWorld().equals(lobby_map.getWorld())) {
            return;
        }
        if(game_map != null && player.getWorld().equals(game_map.getWorld())) {
            return;
        }
        players.remove(player);
        lives.remove(player);
        pre_selected_fighters.remove(player);
        FighterManager.unequipPlayer(player);
        player.getScoreboard().clearSlot(DisplaySlot.SIDEBAR);
        for (Player message : players) {
            message.sendMessage(ChatColor.DARK_GRAY + "Quit> " + ChatColor.GRAY + player.getName());
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void playerJoinedWorld(PlayerChangedWorldEvent e) {
        Player player = e.getPlayer();
        selected_fighter.remove(player);
        if(players.contains(player)) {
            if(lobby_map != null && lobby_map.getWorld().equals(player.getWorld())) {
                setPlayerLobbyItems(player);
            }
            return;
        }
        if(lobby_map != null && lobby_map.getWorld().equals(player.getWorld())) {
            for (Player message : players) {
                message.sendMessage(ChatColor.DARK_GRAY + "Join> " + ChatColor.GRAY + player.getName());
            }
            players.add(player);
            setPlayerLobbyItems(player);
            return;
        }
        if(game_map != null && game_map.getWorld().equals(player.getWorld())) {
            for (Player message : players) {
                message.sendMessage(ChatColor.DARK_GRAY + "Join> " + ChatColor.GRAY + player.getName());
            }
            players.add(player);
            if (getCurrentGamemode() instanceof TrainingGamemode) {
                player.teleport(current_gamemode.getRandomRespawnPoint(player));
                if (FighterManager.getPlayerFighters().get(player) == null) {
                    FighterManager.equipPlayer(player, getCurrentGamemode().getAllowedFighters().getFirst());
                    FighterManager.openKitMenu(player);
                }
                player.setGameMode(GameMode.ADVENTURE);
            } else {
                FighterManager.equipPlayer(player, new SpectatorFighter());
            }
            return;
        }
    }

}

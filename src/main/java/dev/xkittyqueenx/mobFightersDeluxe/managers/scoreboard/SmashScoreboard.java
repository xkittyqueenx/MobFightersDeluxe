package dev.xkittyqueenx.mobFightersDeluxe.managers.scoreboard;

import dev.xkittyqueenx.mobFightersDeluxe.MobFightersDeluxe;
import dev.xkittyqueenx.mobFightersDeluxe.managers.GameManager;
import dev.xkittyqueenx.mobFightersDeluxe.managers.smashserver.SmashServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.HashMap;
import java.util.Objects;
import java.util.Random;

public class SmashScoreboard implements Listener {

    public SmashServer server;
    private HashMap<Player, Scoreboard> player_scoreboards = new HashMap<>();
    private static final Random random = new Random();

    MiniMessage mm = MiniMessage.miniMessage();

    final Component component = MiniMessage.miniMessage().deserialize(
            "<gradient:#32CD32:#E37383><b>MobFighters</gradient>"
    );

    public SmashScoreboard() {
        Bukkit.getServer().getPluginManager().registerEvents(this, MobFightersDeluxe.getInstance());
    }

    public void buildScoreboard() {

    }

    public void setScore(Scoreboard scoreboard, String to_display, int score) {
        Objective obj = scoreboard.getObjective(DisplaySlot.SIDEBAR);
        if(obj == null) {
            return;
        }

        // Parse the text using MiniMessage
        MiniMessage mm = MiniMessage.miniMessage();
        Component message = mm.deserialize(to_display);

        // Convert to legacy format for length checking (since teams still have 16 char limit)
        String legacy = PlainTextComponentSerializer.plainText().serialize(message);

        // Split into prefix and suffix for 16 char limit
        Component prefix;
        Component suffix = Component.empty();

        // Extract color codes at the beginning of the string
        String colorPrefix = "";
        if (to_display.startsWith("<") && to_display.contains(">")) {
            colorPrefix = to_display.substring(0, to_display.indexOf(">") + 1);
        }

        if(legacy.length() > 16) {
            // For the prefix, keep the first 16 characters
            prefix = mm.deserialize(to_display.substring(0, Math.min(to_display.length(), 16)));

            // For the suffix, add the color code again to maintain color, then add remaining text
            if (to_display.length() > 16) {
                suffix = mm.deserialize(colorPrefix + to_display.substring(16));
            }
        } else {
            prefix = message;
        }

        String team_name = getTeamName(score);
        Team team = scoreboard.getTeam(team_name);
        if(team == null) {
            team = scoreboard.registerNewTeam(team_name);
            team.addEntry(team_name);
            obj.getScore(team_name).setScore(score);
        }

        // Update team prefix/suffix with components
        team.prefix(prefix);
        team.suffix(suffix);
    }

    public void clearScore(Scoreboard scoreboard, int score) {
        Objective obj = scoreboard.getObjective(DisplaySlot.SIDEBAR);
        if(obj == null) {
            return;
        }
        String team_name = getTeamName(score);
        Team team = scoreboard.getTeam(team_name);
        if(team != null) {
            scoreboard.resetScores(team_name);
            Objects.requireNonNull(scoreboard.getTeam(team_name)).unregister();
        }
    }

    public void clearScoresAbove(Scoreboard scoreboard, int score) {
        for(int i = score + 1; i < 24; i++) {
            clearScore(scoreboard, i);
        }
    }

    public String getTeamName(int score) {
        // Basically a lot of values
        return (ChatColor.values()[(score / ChatColor.values().length) % ChatColor.values().length] + "" + ChatColor.values()[score % ChatColor.values().length]);
    }

    public static String getPlayerColor(Player player, boolean lives_display) {
        SmashServer server = GameManager.getPlayerServer(player);
        if(server == null) {
            return "<yellow>";
        }
        if(!lives_display) {
            return "<yellow>";
        }
        return getLivesColor(player);
    }

    public static String getLivesColor(Player player) {
        SmashServer server = GameManager.getPlayerServer(player);
        if(server == null) {
            return getLivesColor(0);
        }
        return getLivesColor(server.getLives(player));
    }

    public static String getLivesColor(int lives) {
        if (lives > 4) {
            return "<reset>";
        } else if (lives == 4) {
            return "<green>";
        } else if (lives == 3) {
            return "<yellow>";
        } else if (lives == 2) {
            return "<gold>";
        } else if (lives == 1) {
            return "<red>";
        } else {
            return "<gray>";
        }
    }

}

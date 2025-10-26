package dev.xkittyqueenx.mobFightersDeluxe.managers;

import dev.xkittyqueenx.mobFightersDeluxe.MobFightersDeluxe;
import dev.xkittyqueenx.mobFightersDeluxe.managers.smashserver.SmashServer;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class ConfigManager {

    private YamlConfiguration fightersConfig;

    public ConfigManager() {
        File file = new File(MobFightersDeluxe.getInstance().getDataFolder(), "fighters.yml");
        fightersConfig = YamlConfiguration.loadConfiguration(file);
        try {
            fightersConfig.save(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public YamlConfiguration getFightersConfig() {
        return fightersConfig;
    }

    public void reloadFightersConfig() {
        File file = new File(MobFightersDeluxe.getInstance().getDataFolder(), "fighters.yml");
        fightersConfig = YamlConfiguration.loadConfiguration(file);
        for (SmashServer server : GameManager.servers) {
            server.getCurrentGamemode().updateAllowedKits();
        }
        MobFightersDeluxe.getInstance().getComponentLogger().info(MiniMessage.miniMessage().deserialize("Successfully reloaded!"));
    }

}

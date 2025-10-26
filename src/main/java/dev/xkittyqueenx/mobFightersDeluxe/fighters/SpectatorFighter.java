package dev.xkittyqueenx.mobFightersDeluxe.fighters;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

public class SpectatorFighter extends Fighter implements Listener {

    protected boolean hidePlayer = true;

    public SpectatorFighter() {
        super();
        this.name = "Temporary Spectator";
        invincible = true;
        intangible = true;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public SpectatorFighter(String glyph) {
        super();
        this.name = "Temporary Spectator";
        invincible = true;
        intangible = true;
        this.glyph = glyph;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void initializeKit() {

        owner.setAllowFlight(true);
        owner.setFlying(true);

        if (hidePlayer) {
            for (Player hidefrom : Bukkit.getOnlinePlayers()) {
                if (owner.equals(hidefrom)) {
                    continue;
                }
                hidefrom.hidePlayer(plugin, owner);
            }
        }
    }

    @Override
    public void destroy() {
        final Player player = owner;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player != null) {
                // Unhide owner
                for (Player hidefrom : Bukkit.getOnlinePlayers()) {
                    if (player.equals(hidefrom)) {
                        continue;
                    }
                    hidefrom.showPlayer(plugin, player);
                }
                player.setFlying(false);
                player.setAllowFlight(false);
            }
        }, 20L);
        super.destroy();
    }

    @Override
    public void setPreviewHotbar() {
        return;
    }

    @Override
    public void setGameHotbar() {

    }

}

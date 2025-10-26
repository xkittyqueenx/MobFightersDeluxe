package dev.xkittyqueenx.mobFightersDeluxe.attributes.doublejumps;

import dev.xkittyqueenx.mobFightersDeluxe.attributes.Attribute;
import dev.xkittyqueenx.mobFightersDeluxe.fighters.Fighter;
import dev.xkittyqueenx.mobFightersDeluxe.managers.FighterManager;
import dev.xkittyqueenx.mobFightersDeluxe.utilities.Utils;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerToggleFlightEvent;

public class DoubleJump extends Attribute {

    protected double height;
    protected double power;
    protected Sound double_jump_sound;
    protected long last_jump_time_ms = 0;
    protected long recharge_delay_ms = 0;
    protected float volume;
    protected float pitch;

    public DoubleJump(double power, double height, Sound double_jump_sound, float volume, float pitch) {
        super();
        this.power = power;
        this.height = height;
        this.volume = volume;
        this.pitch = pitch;
        this.double_jump_sound = double_jump_sound;
        this.runTaskTimer(plugin, 0L, 2L);
    }

    @Override
    public void run() {
        if(owner == null || owner.getGameMode() == GameMode.CREATIVE) {
            return;
        }

        // Cache KitManager.getPlayerKit result
        Fighter fighter = FighterManager.getPlayerFighters().get(owner);
        if(fighter != null && !fighter.isActive()) {
            owner.setAllowFlight(false);
            return;
        }

        // Only perform ground check if needed
        if(System.currentTimeMillis() - last_jump_time_ms >= recharge_delay_ms && groundCheck()) {
            owner.setAllowFlight(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void playerFlightEvent(PlayerToggleFlightEvent e) {
        if(!e.getPlayer().equals(owner)) {
            return;
        }
        Player player = e.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }
        e.setCancelled(true);
        player.setFlying(false);
        Fighter fighter = FighterManager.getPlayerFighters().get(player);
        if (fighter == null) return;
        if(!check() || fighter.isRooted()) {
            player.playSound(player, Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 1f);
            return;
        }
        player.setAllowFlight(false);
        playDoubleJumpSound();
        checkAndActivate();
        last_jump_time_ms = System.currentTimeMillis();
    }

    @Override
    public void activate() {
        return;
    }

    public boolean groundCheck() {
        return Utils.entityIsOnGround(owner);
    }

    public void playDoubleJumpSound() {
        owner.getWorld().playSound(owner.getLocation(), double_jump_sound, volume, pitch);
    }

    @Override
    public void setOwner(Player owner) {
        if(owner == null) {
            this.owner.setFlying(false);
            this.owner.setAllowFlight(false);
        }
        super.setOwner(owner);
    }

    public double getPower() {
        return power;
    }

    public double getHeight() {
        return height;
    }

    public void setPower(double power) {
        this.power = power;
    }

    public void setHeight(double height) {
        this.height = height;
    }

}

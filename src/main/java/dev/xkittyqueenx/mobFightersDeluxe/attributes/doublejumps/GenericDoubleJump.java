package dev.xkittyqueenx.mobFightersDeluxe.attributes.doublejumps;

import dev.xkittyqueenx.mobFightersDeluxe.utilities.VelocityUtil;
import org.bukkit.Particle;
import org.bukkit.Sound;

public class GenericDoubleJump extends DoubleJump {

    public GenericDoubleJump(double power, double height, Sound double_jump_sound, float volume, float pitch) {
        super(power, height, double_jump_sound, volume, pitch);
        this.name = "Double Jump";
    }

    @Override
    public void activate() {
        VelocityUtil.setVelocity(owner, owner.getLocation().getDirection(), power, true, power, 0, height, true);
        owner.getWorld().spawnParticle(Particle.CLOUD, owner.getLocation(), 5, 0.3f, 0.1f, 0.3f, 0.2f, null, true);
    }

}

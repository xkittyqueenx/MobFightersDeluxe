package dev.xkittyqueenx.mobFightersDeluxe.utilities;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class VelocityUtil {

    public static void setVelocity(Entity entity, Vector velocity) {
        if (entity instanceof Player player) {
            player.setVelocity(velocity);
        } else {
            entity.setVelocity(velocity);
        }
    }

    public static void setVelocity(Entity ent, Vector vec, double str, boolean ySet, double yBase, double yAdd, double yMax, boolean groundBoost) {
        if (Double.isNaN(vec.getX()) || Double.isNaN(vec.getY()) || Double.isNaN(vec.getZ()) || vec.length() == 0) {
            return;
        }

        //YSet
        if (ySet)
            vec.setY(yBase);

        //Modify
        vec.normalize();
        vec.multiply(str);

        //YAdd
        vec.setY(vec.getY() + yAdd);

        //Limit
        if (vec.getY() > yMax)
            vec.setY(yMax);

        if (groundBoost)
            if (Utils.entityIsOnGround(ent, 0.02))
                vec.setY(vec.getY() + 0.2);

        VelocityUtil.setVelocity(ent, vec);
        //Bukkit.broadcastMessage("Set Velocity: " + vec);
    }

    public static void setVelocity(Entity ent, Vector vec, double str, boolean ySet, double yBase, double yAdd, double yMax, boolean groundBoost, double oldVelocityMultiplier) {
        if (Double.isNaN(vec.getX()) || Double.isNaN(vec.getY()) || Double.isNaN(vec.getZ()) || vec.length() == 0) {
            return;
        }

        //YSet
        if (ySet)
            vec.setY(yBase);

        //Modify
        vec.normalize();
        vec.multiply(str);

        //YAdd
        vec.setY(vec.getY() + yAdd);

        //Limit
        if (vec.getY() > yMax)
            vec.setY(yMax);

        if (groundBoost)
            if (Utils.entityIsOnGround(ent, 0.02))
                vec.setY(vec.getY() + 0.2);


        Vector old_velocity = ent.getVelocity().multiply(oldVelocityMultiplier).clone();

        old_velocity.add(vec);

        VelocityUtil.setVelocity(ent, old_velocity);
        //Bukkit.broadcastMessage("Set Velocity: " + vec);
    }

}

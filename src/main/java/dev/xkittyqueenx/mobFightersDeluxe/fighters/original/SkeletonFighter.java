package dev.xkittyqueenx.mobFightersDeluxe.fighters.original;

import dev.xkittyqueenx.mobFightersDeluxe.fighters.Fighter;
import org.bukkit.entity.EntityType;

public class SkeletonFighter extends Fighter {

    public SkeletonFighter() {
        super();
        this.name = "Skeleton";
        this.damage = 5.0;
        this.armor = 5.0;
        this.regeneration = 0.25;
        this.knockback = 1.3;
        this.disguiseType = EntityType.SKELETON;
        this.icon = "2746";
    }

    @Override
    public void initializeKit() {

    }

    @Override
    public void setGameHotbar() {

    }

    @Override
    public void setPreviewHotbar() {

    }

}

package dev.xkittyqueenx.mobFightersDeluxe.fighters.original;

import dev.xkittyqueenx.mobFightersDeluxe.attributes.doublejumps.GenericDoubleJump;
import dev.xkittyqueenx.mobFightersDeluxe.fighters.Fighter;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;

public class ZombieFighter extends Fighter {

    public ZombieFighter() {
        super();
        this.name = "Zombie";
        this.damage = 6.0;
        this.armor = 4.0;
        this.regeneration = 0.25;
        this.knockback = 1.3;
        this.disguiseType = EntityType.ZOMBIE;
        this.icon = "4187";
    }

    @Override
    public void initializeKit() {
        addAttribute(new GenericDoubleJump(0.9, 0.9, Sound.ENTITY_GHAST_SHOOT, 1f, 1f));
    }

    @Override
    public void setGameHotbar() {

    }

    @Override
    public void setPreviewHotbar() {

    }

}

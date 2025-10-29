package dev.xkittyqueenx.mobFightersDeluxe.fighters.original;

import dev.xkittyqueenx.mobFightersDeluxe.MobFightersDeluxe;
import dev.xkittyqueenx.mobFightersDeluxe.abilities.zombie.BladeSlash;
import dev.xkittyqueenx.mobFightersDeluxe.attributes.Bloodlust;
import dev.xkittyqueenx.mobFightersDeluxe.attributes.Regeneration;
import dev.xkittyqueenx.mobFightersDeluxe.attributes.Ultimate;
import dev.xkittyqueenx.mobFightersDeluxe.attributes.doublejumps.GenericDoubleJump;
import dev.xkittyqueenx.mobFightersDeluxe.fighters.Fighter;
import io.papermc.paper.datacomponent.DataComponentTypes;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class ZombieFighter extends Fighter {

    public ZombieFighter() {
        super();
        this.name = "<dark_green>Zombie";

        YamlConfiguration fightersConfig = MobFightersDeluxe.getInstance().getConfigManager().getFightersConfig();
        this.damage = fightersConfig.getDouble("zombie.melee");
        this.armor = fightersConfig.getDouble("zombie.armor");
        this.regeneration = fightersConfig.getDouble("zombie.regen");
        this.knockback = fightersConfig.getDouble("zombie.knockback");

        this.lore = List.of(
                mm.deserialize(" "),
                mm.deserialize("<red>Melee: " + String.format("%.1f", this.damage)).decoration(TextDecoration.ITALIC, false),
                mm.deserialize("<blue>Armor: " + String.format("%.1f", this.armor)).decoration(TextDecoration.ITALIC, false),
                mm.deserialize("<gold>Regen: " + String.format("%.2f", this.regeneration) + " hp/s").decoration(TextDecoration.ITALIC, false),
                mm.deserialize("<#D3D3D3>Knockback: " + String.format("%.1f", (this.knockback * 100.0)) + "%").decoration(TextDecoration.ITALIC, false),
                mm.deserialize(" ")
        );
        this.disguiseType = EntityType.ZOMBIE;
        this.selectSound = Sound.ENTITY_HUSK_CONVERTED_TO_ZOMBIE;
        this.hurtSound = Sound.ENTITY_ZOMBIE_HURT;
        this.walkSound = Sound.ENTITY_ZOMBIE_STEP;
        this.icon = "4187";
    }

    @Override
    public void initializeKit() {

        setAbility(new BladeSlash(), 0);

        addAttribute(new Bloodlust());
        addAttribute(new GenericDoubleJump(0.9, 0.9, Sound.ENTITY_ZOMBIE_INFECT, 0.5f, 1.5f));
        addAttribute(new Ultimate(1f, 400.0f));
        addAttribute(new Regeneration(this.regeneration));

        resetCooldowns(owner);
    }

    @Override
    public void setPreviewHotbar() {

    }

    @Override
    public void setGameHotbar() {
        setItem(new ItemStack(Material.IRON_SWORD), 0);
        setItem(new ItemStack(Material.IRON_AXE), 1);
        setItem(new ItemStack(Material.IRON_SHOVEL), 2);

        ItemStack ultimateStack = new ItemStack(Material.NETHER_STAR);
        ultimateStack.setData(DataComponentTypes.TOOL, Material.DIAMOND_HOE.getDefaultData(DataComponentTypes.TOOL));
        setItem(ultimateStack, 4);

        setItem(new ItemStack(Material.RED_DYE), 8, getAttributeByClass(Bloodlust.class));
    }

}

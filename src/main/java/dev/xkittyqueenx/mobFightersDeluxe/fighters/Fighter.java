package dev.xkittyqueenx.mobFightersDeluxe.fighters;

import dev.xkittyqueenx.mobFightersDeluxe.MobFightersDeluxe;
import dev.xkittyqueenx.mobFightersDeluxe.abilities.Ability;
import dev.xkittyqueenx.mobFightersDeluxe.attributes.Attribute;
import dev.xkittyqueenx.mobFightersDeluxe.attributes.debuffs.Stunned;
import dev.xkittyqueenx.mobFightersDeluxe.events.PlayerDisguiseEvent;
import dev.xkittyqueenx.mobFightersDeluxe.events.PlayerUndisguiseEvent;
import dev.xkittyqueenx.mobFightersDeluxe.managers.GameManager;
import dev.xkittyqueenx.mobFightersDeluxe.managers.gamestate.GameState;
import dev.xkittyqueenx.mobFightersDeluxe.managers.smashserver.SmashServer;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import io.papermc.paper.datacomponent.item.TooltipDisplay;
import io.papermc.paper.datacomponent.item.UseCooldown;
import io.papermc.paper.datacomponent.item.consumable.ItemUseAnimation;
import io.papermc.paper.entity.LookAnchor;
import io.papermc.paper.entity.TeleportFlag;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class Fighter implements Listener, Runnable {

    protected String name = "";
    protected List<Component> lore = new ArrayList<>();

    protected double damage = 0.0;
    protected double armor = 0.0;
    protected double regeneration = 0.0;
    protected double knockback = 0.0;

    protected String icon = "";
    protected String glyph = "";
    public Sound selectSound = Sound.UI_BUTTON_CLICK;
    public Sound hurtSound = Sound.ENTITY_PLAYER_HURT;

    protected boolean invincible = false;
    protected boolean intangible = false;

    public int stunned_task = -1;
    public long lastStunTimeMs = 0;

    protected BukkitTask task;

    protected EntityType disguiseType = null;
    protected Mob disguise = null;
    protected TextDisplay textDisplay;

    protected boolean isStunned = false;
    protected boolean isRooted = false;

    protected Player owner = null;
    protected Plugin plugin;

    protected List<Attribute> attributes = new ArrayList<>();
    protected Ability[] hotbarAbilities = new Ability[9];

    private double knockbackPercentage = 0.0;

    private boolean created = false;
    private boolean preview_hotbar_equipped = false;
    private boolean game_hotbar_equipped = false;
    private boolean playing = false;

    protected MiniMessage mm = MiniMessage.miniMessage();

    public Fighter() {
        plugin = MobFightersDeluxe.getInstance();
        Bukkit.getPluginManager().registerEvents(this, MobFightersDeluxe.getInstance());
    }

    @Override
    public void run() {
        if (owner == null) {
            task.cancel();
            if (disguise != null) {
                disguise.remove();
                disguise = null;
            }
            if (textDisplay != null) {
                textDisplay.remove();
                textDisplay = null;
            }
            return;
        }
        if (disguise == null || textDisplay == null) return;

        disguise.teleport(owner.getLocation().clone().add(owner.getVelocity().clone()), TeleportFlag.Relative.VELOCITY_X, TeleportFlag.Relative.VELOCITY_Y, TeleportFlag.Relative.VELOCITY_Z, TeleportFlag.Relative.VELOCITY_ROTATION);

        textDisplay.teleport(disguise.getEyeLocation().clone().add(0, 0.5, 0));
        textDisplay.text(MiniMessage.miniMessage().deserialize("<#D3D3D3>" + owner.getName() + "<br>" +
                "<reset><#D3D3D3><b>[" + String.format("%.1f", knockbackPercentage) + "%]"));

        if (disguise.getEquipment().getItemInMainHand().getType() != owner.getInventory().getItemInMainHand().getType()) {
            disguise.getEquipment().setItemInMainHand(owner.getInventory().getItemInMainHand());
        }

        if (owner.isSneaking()) {
            disguise.setPose(Pose.SNEAKING, true);
        } else {
            disguise.setPose(Pose.STANDING, true);
        }

        updateInventory();

    }

    public abstract void setPreviewHotbar();

    public abstract void setGameHotbar();

    public void setOwner(Player player) {
        if (created) {
            return;
        }
        created = true;
        owner = player;
        player.getInventory().clear();
        player.getInventory().setHelmet(null);
        player.getInventory().setChestplate(null);
        player.getInventory().setLeggings(null);
        player.getInventory().setBoots(null);
        Objects.requireNonNull(owner.getAttribute(org.bukkit.attribute.Attribute.MOVEMENT_SPEED)).setBaseValue(0.1);
        Objects.requireNonNull(owner.getAttribute(org.bukkit.attribute.Attribute.JUMP_STRENGTH)).setBaseValue(0.4199999);
        player.setExp(0);
        player.setGameMode(GameMode.ADVENTURE);
        player.setFlying(false);
        player.setAllowFlight(true);
        hotbarAbilities = new Ability[9];
        initializeKit();
        SmashServer server = GameManager.getPlayerServer(owner);
        if(server == null) {
            updatePlaying(GameState.GAME_PLAYING, true);
            return;
        }
        updatePlaying(server.getState(), true);
        task = Bukkit.getScheduler().runTaskTimer(MobFightersDeluxe.getInstance(), this, 0L, 0L);
        equipDisguise();
    }

    public void updatePlaying(short new_state, boolean reload_hotbar) {
        if(owner == null) {
            return;
        }
        boolean game_hotbar = GameState.isStarting(new_state) || GameState.isPlaying(new_state) || new_state == GameState.GAME_ENDING;
        // Set hotbar and register or unregister dev.urpcketgf.mobfighters.events for dev.urpcketgf.mobfighters.attributes
        // Use booleans so we don't re-equip the same hotbar we already did
        if(game_hotbar && (!game_hotbar_equipped || reload_hotbar)) {
            owner.getInventory().clear();
            setGameHotbar();
            game_hotbar_equipped = true;
            preview_hotbar_equipped = false;
        }
        if(!game_hotbar && (!preview_hotbar_equipped || reload_hotbar)) {
            owner.getInventory().clear();
            setPreviewHotbar();
            preview_hotbar_equipped = true;
            game_hotbar_equipped = false;
        }
        if(GameState.isPlaying(new_state) || new_state == GameState.GAME_ENDING) {
            playing = true;
            for(Attribute attribute : attributes) {
                HandlerList.unregisterAll(attribute);
                Bukkit.getPluginManager().registerEvents(attribute, plugin);
            }
        }
        else {
            playing = false;
            for(Attribute attribute : attributes) {
                HandlerList.unregisterAll(attribute);
            }
        }
    }

    public <T extends Attribute> T getAttributeByClass(Class<T> check) {
        // Check for exact class
        for(Attribute attribute : attributes) {
            if(attribute.getClass() == check) {
                return check.cast(attribute);
            }
        }
        // Check if attribute is subclass of check
        for(Attribute attribute : attributes) {
            if(check.isInstance(attribute)) {
                return check.cast(attribute);
            }
        }
        return null;
    }

    public void applyStunned(double duration) {
        if (Bukkit.getScheduler().isCurrentlyRunning(stunned_task) || Bukkit.getScheduler().isQueued(stunned_task)) {
            Bukkit.getScheduler().cancelTask(stunned_task);
        }
        owner.getAttribute(org.bukkit.attribute.Attribute.MOVEMENT_SPEED).setBaseValue(0.0);
        owner.getAttribute(org.bukkit.attribute.Attribute.JUMP_STRENGTH).setBaseValue(0.0);
        owner.getAttribute(org.bukkit.attribute.Attribute.GRAVITY).setBaseValue(0.04);
        for (Attribute attribute : attributes) {
            if (attribute instanceof Stunned stunned) {
                stunned.onStunned();
            }
        }
        for (Ability ability : hotbarAbilities) {
            if (ability instanceof Stunned stunned) {
                stunned.onStunned();
            }
        }
        owner.setRiptiding(true);
        setStunned(true);
        lastStunTimeMs = System.currentTimeMillis();
        final Vector vector = owner.getLocation().getDirection().multiply(5.0).clone();
        owner.lookAt(owner.getLocation().clone().add(vector), LookAnchor.FEET);
        stunned_task = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
            final Player player = owner;
            final long start_time_ms = System.currentTimeMillis();
            @Override
            public void run() {
                if (owner == null) {
                    Bukkit.getScheduler().cancelTask(stunned_task);
                    player.setRiptiding(false);
                    Objects.requireNonNull(player.getAttribute(org.bukkit.attribute.Attribute.MOVEMENT_SPEED)).setBaseValue(0.1);
                    Objects.requireNonNull(player.getAttribute(org.bukkit.attribute.Attribute.JUMP_STRENGTH)).setBaseValue(0.4199999);
                    player.getAttribute(org.bukkit.attribute.Attribute.GRAVITY).setBaseValue(0.08);
                    return;
                }
                long elapsed = System.currentTimeMillis() - start_time_ms;
                if (elapsed >= (duration * 1000)) {
                    Bukkit.getScheduler().cancelTask(stunned_task);
                    setStunned(false);
                    owner.setRiptiding(false);
                    Objects.requireNonNull(owner.getAttribute(org.bukkit.attribute.Attribute.MOVEMENT_SPEED)).setBaseValue(0.1);
                    Objects.requireNonNull(owner.getAttribute(org.bukkit.attribute.Attribute.JUMP_STRENGTH)).setBaseValue(0.4199999);
                    owner.getAttribute(org.bukkit.attribute.Attribute.GRAVITY).setBaseValue(0.08);
                    return;
                }
            }
        }, 0L, 0L);
    }

    public abstract void initializeKit();

    public Ability getAbilityInSlot(int inventorySlot) {
        return hotbarAbilities[inventorySlot];
    }

    public List<Attribute> getAttributes() {
        return attributes;
    }

    public boolean isActive() {
        return playing;
    }

    public void addAttribute(Attribute attribute) {
        attributes.add(attribute);
        attribute.setOwner(owner);
        SmashServer server = GameManager.getPlayerServer(owner);
        if(server == null) {
            updatePlaying(GameState.GAME_PLAYING, false);
            return;
        }
        updatePlaying(server.getState(), false);
    }

    public void removeAttribute(Attribute attribute) {
        attributes.remove(attribute);
        attribute.afterRemoval(owner);
        attribute.remove();
    }

    public void updateInventory() {
        if (owner == null) return;
        for (int slot = 0; slot < 9; slot++) {  // Changed to < 9 to include all slots
            Ability ability = getAbilityInSlot(slot);
            if (ability == null) continue;

            ItemStack itemStack = new ItemStack(ability.getItemStack());

            // Check if ability should be disabled due to stun/root
            if (isStunned && ability instanceof Stunned) {
                itemStack = itemStack.withType(Material.BARRIER);
            }

            owner.getInventory().setItem(slot, itemStack);
        }
    }

    public void resetCooldowns(Player player) {
        Inventory inventory = player.getInventory();
        ItemStack item0 = inventory.getItem(0);
        if (item0 != null) {
            player.setCooldown(item0, 0);
        }
        ItemStack item1 = inventory.getItem(1);
        if (item1 != null) {
            player.setCooldown(item1, 0);
        }
        ItemStack item2 = inventory.getItem(2);
        if (item2 != null) {
            player.setCooldown(item2, 0);
        }
    }

    public void setAbility(Ability ability, int hotbarSlot) {
        Ability old = hotbarAbilities[hotbarSlot];
        if(old != null) {
            attributes.remove(old);
            old.afterRemoval(owner);
            old.remove();
        }
        addAttribute(ability);
        hotbarAbilities[hotbarSlot] = ability;
        ItemStack item = owner.getInventory().getItem(hotbarSlot);
        if(item != null) {
            setItem(item, hotbarSlot);
        }
    }

    public void setItem(ItemStack item, int hotbarSlot) {
        setItem(item, hotbarSlot, hotbarAbilities[hotbarSlot]);
    }

    public void setItem(ItemStack item, int hotbarSlot, Attribute attribute) {
        if (owner == null) {
            return;
        }
        if (attribute != null) {
            String used_name = attribute.name;
            item.setData(DataComponentTypes.ITEM_NAME, MiniMessage.miniMessage().deserialize("<gradient:#F4BB44:#FFA500><b> " + used_name + " </gradient><#FFFFFF><b>⏽ " + "<color:#A9A9A9><b>" + attribute.getUsage().toString()).decoration(TextDecoration.ITALIC, false));
            item.setData(DataComponentTypes.LORE, ItemLore.lore(attribute.getLore()));
            item.unsetData(DataComponentTypes.TOOL);
            item.setData(DataComponentTypes.UNBREAKABLE);
            item.setData(DataComponentTypes.TOOLTIP_DISPLAY, TooltipDisplay.tooltipDisplay()
                    .addHiddenComponents(DataComponentTypes.UNBREAKABLE)
                    .addHiddenComponents(DataComponentTypes.ENCHANTMENTS)
                    .build());
            item.setData(DataComponentTypes.USE_COOLDOWN, UseCooldown.useCooldown(1).cooldownGroup(Key.key(String.valueOf(hotbarSlot))).build());
            item.setData(DataComponentTypes.MAX_DAMAGE, 10000);
            item.unsetData(DataComponentTypes.ATTRIBUTE_MODIFIERS);
            item.setData(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, false); // Make it glow!
            if (item.getType() == Material.NETHER_STAR) {
                item.unsetData(DataComponentTypes.UNBREAKABLE);
            }
            attribute.setItemStack(item);
        }
        owner.getInventory().setItem(hotbarSlot, item);
    }

    public void setSwordItem(ItemStack item, int hotbarSlot) {
        setSwordItem(item, hotbarSlot, hotbarAbilities[hotbarSlot]);
    }

    public void setSwordItem(ItemStack item, int hotbarSlot, Attribute attribute) {
        if (owner == null) {
            return;
        }
        if (attribute != null) {
            String used_name = attribute.name;
            item.setData(DataComponentTypes.ITEM_NAME, MiniMessage.miniMessage().deserialize("<gradient:#F4BB44:#FFA500><b> " + used_name + " </gradient><#FFFFFF><b>⏽ " + "<color:#A9A9A9><b>" + attribute.getUsage().toString()).decoration(TextDecoration.ITALIC, false));
            item.setData(DataComponentTypes.LORE, ItemLore.lore(attribute.getLore()));
            item.unsetData(DataComponentTypes.TOOL);
            item.setData(DataComponentTypes.MAX_DAMAGE, 10000);
            item.unsetData(DataComponentTypes.ATTRIBUTE_MODIFIERS);
            item.setData(DataComponentTypes.UNBREAKABLE);
            item.setData(DataComponentTypes.TOOLTIP_DISPLAY, TooltipDisplay.tooltipDisplay()
                    .addHiddenComponents(DataComponentTypes.UNBREAKABLE)
                    .addHiddenComponents(DataComponentTypes.ENCHANTMENTS)
                    .build());
            item.setData(DataComponentTypes.USE_COOLDOWN, UseCooldown.useCooldown(1).cooldownGroup(Key.key(String.valueOf(hotbarSlot))).build());
            item.setData(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, false); // Make it glow!
            item.setData(DataComponentTypes.CONSUMABLE, io.papermc.paper.datacomponent.item.Consumable.consumable()
                    .consumeSeconds(Integer.MAX_VALUE)
                    .animation(ItemUseAnimation.BLOCK)
                    .hasConsumeParticles(false)
                    .build());
            attribute.setItemStack(item);
        }
        owner.getInventory().setItem(hotbarSlot, item);
    }

    public void equipDisguise() {
        if (owner == null || disguiseType == null) return;

        owner.setCollidable(false);

        disguise = (Mob) owner.getWorld().spawnEntity(owner.getLocation(), disguiseType, false);
        disguise.setCanPickupItems(false);
        disguise.setRemoveWhenFarAway(false);
        disguise.setLeftHanded(false);
        if (disguise instanceof Skeleton skeleton) {
            skeleton.setShouldBurnInDay(false);
        }
        if (disguise instanceof Zombie zombie) {
            zombie.setShouldBurnInDay(false);
        }
        Bukkit.getMobGoals().removeAllGoals(disguise);
        disguise.setAware(false);
        disguise.setAggressive(true);
        disguise.getEquipment().clear();
        disguise.setMetadata(owner.getUniqueId().toString(), new FixedMetadataValue(plugin, 1));

        textDisplay = owner.getWorld().spawn(owner.getEyeLocation(), TextDisplay.class);
        textDisplay.setPersistent(true);
        textDisplay.setTeleportDuration(2);
        textDisplay.setShadowed(false);
        textDisplay.setBackgroundColor(Color.fromARGB(0, 255, 255, 255));
        textDisplay.setSeeThrough(true);
        textDisplay.setBillboard(Display.Billboard.CENTER);
        textDisplay.setTransformationMatrix(new Matrix4f().scale(0.75f));

        owner.hideEntity(plugin, disguise);
        owner.hideEntity(plugin, textDisplay);

        PlayerDisguiseEvent disguiseEvent = new PlayerDisguiseEvent(owner, disguise);
        disguiseEvent.callEvent();
    }

    public void destroy() {
        if (owner == null) return;
        PlayerUndisguiseEvent playerUndisguiseEvent = new PlayerUndisguiseEvent(owner);
        playerUndisguiseEvent.callEvent();
        if (disguise != null) {
            owner.setCollidable(true);
            owner = null;
            disguise.remove();
            disguise = null;
        }
    }

    public Mob getDisguise() {
        return disguise;
    }

    public double getKnockbackPercentage() {
        return knockbackPercentage;
    }

    public void setKnockbackPercentage(double knockbackPercentage) {
        this.knockbackPercentage = knockbackPercentage;
    }

    public double getDamage() {
        return damage;
    }

    public void setDamage(double damage) {
        this.damage = damage;
    }

    public double getArmor() {
        return armor;
    }

    public void setArmor(double armor) {
        this.armor = armor;
    }

    public double getRegeneration() {
        return regeneration;
    }

    public void setRegeneration(double regeneration) {
        this.regeneration = regeneration;
    }

    public double getKnockback() {
        return knockback;
    }

    public void setKnockback(double knockback) {
        this.knockback = knockback;
    }

    public boolean isInvincible() {
        return invincible;
    }

    public void setInvincible(boolean invincible) {
        this.invincible = invincible;
    }

    public boolean isIntangible() {
        return intangible;
    }

    public void setIntangible(boolean intangible) {
        this.intangible = intangible;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getGlyph() {
        return glyph;
    }

    public void setGlyph(String glyph) {
        this.glyph = glyph;
    }

    public boolean isCreated() {
        return created;
    }

    public void setCreated(boolean created) {
        this.created = created;
    }

    public boolean isPreview_hotbar_equipped() {
        return preview_hotbar_equipped;
    }

    public void setPreview_hotbar_equipped(boolean preview_hotbar_equipped) {
        this.preview_hotbar_equipped = preview_hotbar_equipped;
    }

    public boolean isGame_hotbar_equipped() {
        return game_hotbar_equipped;
    }

    public void setGame_hotbar_equipped(boolean game_hotbar_equipped) {
        this.game_hotbar_equipped = game_hotbar_equipped;
    }

    public boolean isPlaying() {
        return playing;
    }

    public void setPlaying(boolean playing) {
        this.playing = playing;
    }

    public boolean isRooted() {
        return isRooted;
    }

    public void setRooted(boolean rooted) {
        isRooted = rooted;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Component> getLore() {
        return lore;
    }

    public void setLore(List<Component> lore) {
        this.lore = lore;
    }

    public boolean isStunned() {
        return isStunned;
    }

    public void setStunned(boolean stunned) {
        isStunned = stunned;
    }
}

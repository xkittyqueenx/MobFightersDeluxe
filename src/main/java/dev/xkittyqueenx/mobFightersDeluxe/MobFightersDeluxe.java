package dev.xkittyqueenx.mobFightersDeluxe;

import dev.xkittyqueenx.mobFightersDeluxe.commands.CommandMobFighters;
import dev.xkittyqueenx.mobFightersDeluxe.events.SmashDamageEvent;
import dev.xkittyqueenx.mobFightersDeluxe.fighters.Fighter;
import dev.xkittyqueenx.mobFightersDeluxe.fighters.original.ZombieFighter;
import dev.xkittyqueenx.mobFightersDeluxe.managers.*;
import dev.xkittyqueenx.mobFightersDeluxe.managers.gamemodes.TrainingGamemode;
import dev.xkittyqueenx.mobFightersDeluxe.managers.smashserver.SmashServer;
import dev.xkittyqueenx.mobFightersDeluxe.utilities.DamageUtil;
import dev.xkittyqueenx.mobFightersDeluxe.utilities.MapPool;
import dev.xkittyqueenx.mobFightersDeluxe.utilities.Utils;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class MobFightersDeluxe extends JavaPlugin implements Listener {

    private static MobFightersDeluxe ourInstance;

    private FighterManager fighterManager;
    private ConfigManager configManager;
    private GameManager gameManager;
    private BlockRestoreManager blockRestoreManager;
    private DamageManager damageManager;
    private EventManager eventManager;
    private MenuManager menuManager;
    private CooldownManager cooldownManager;

    public static ItemStack SERVER_BROWSER_ITEM;
    public static ItemStack KIT_SELECTOR_ITEM;
    public static ItemStack VOTING_MENU_ITEM;
    public static ItemStack TELEPORT_HUB_ITEM;

    public static MobFightersDeluxe getInstance() {
        return ourInstance;
    }

    @Override
    public void onEnable() {
        ourInstance = this;
        Bukkit.getPluginManager().registerEvents(this, this);
        saveResource("fighters.yml", false);
        fighterManager = new FighterManager();
        configManager = new ConfigManager();
        gameManager = new GameManager();
        blockRestoreManager = new BlockRestoreManager();
        damageManager = new DamageManager();
        eventManager = new EventManager();
        menuManager = new MenuManager();
        cooldownManager = new CooldownManager();

        SERVER_BROWSER_ITEM = new ItemStack(Material.COMPASS);
        KIT_SELECTOR_ITEM = new ItemStack(Material.SKELETON_SKULL);
        VOTING_MENU_ITEM = new ItemStack(Material.PAPER);
        TELEPORT_HUB_ITEM = new ItemStack(Material.CLOCK);

        ItemMeta hub_meta = TELEPORT_HUB_ITEM.getItemMeta();
        hub_meta.setDisplayName(ChatColor.GREEN + "Return to Hub");
        TELEPORT_HUB_ITEM.setItemMeta(hub_meta);

        ItemMeta server_meta = SERVER_BROWSER_ITEM.getItemMeta();
        ItemMeta kit_meta = KIT_SELECTOR_ITEM.getItemMeta();
        ItemMeta voting_meta = VOTING_MENU_ITEM.getItemMeta();

        kit_meta.setDisplayName(ChatColor.YELLOW + "Kit Selector");
        server_meta.setDisplayName(ChatColor.GREEN + "" + "Quick Compass");
        voting_meta.setDisplayName(ChatColor.GREEN + "Vote for Stage");

        VOTING_MENU_ITEM.setItemMeta(voting_meta);
        KIT_SELECTOR_ITEM.setItemMeta(kit_meta);
        SERVER_BROWSER_ITEM.setItemMeta(server_meta);

        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
            commands.registrar().register(CommandMobFighters.fightersCommand());
            commands.registrar().register(CommandMobFighters.hubCommand());
            commands.registrar().register(CommandMobFighters.kitCommand());
        });
        for (Player player : Bukkit.getOnlinePlayers()) {
            equipPlayerHub(player);
        }
        GameManager.createSmashServer(new TrainingGamemode(MapPool.SSM));
        for (World world : Bukkit.getWorlds()) {
            world.setAutoSave(false);
        }
    }

    @Override
    public void onDisable() {
        Bukkit.getWorlds().getFirst().getLivingEntities().forEach(Entity::remove);
    }

    public FighterManager getFighterManager() {
        return fighterManager;
    }

    public void equipPlayerHub(Player player) {
        Fighter kit = FighterManager.getPlayerFighters().get(player);
        if (kit != null) {
            FighterManager.unequipPlayer(player);
        }
        player.getInventory().clear();
        player.getInventory().setItem(0, SERVER_BROWSER_ITEM);
        Objects.requireNonNull(player.getAttribute(Attribute.MOVEMENT_SPEED)).setBaseValue(0.1);
        Objects.requireNonNull(player.getAttribute(Attribute.JUMP_STRENGTH)).setBaseValue(0.4199999);
        Objects.requireNonNull(player.getAttribute(Attribute.BLOCK_INTERACTION_RANGE)).setBaseValue(1);
        Utils.fullHeal(player);
    }

    @EventHandler
    public void onPlayerLoseHunger(FoodLevelChangeEvent e) {
        e.setCancelled(true);
        e.setFoodLevel(20);
        if (e.getEntity() instanceof Player player) {
            player.setSaturation(10f);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        e.setJoinMessage("");

        // Show the leaving player to all online players
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (!onlinePlayer.canSee(player)) {
                onlinePlayer.showPlayer(this, player);
            }
        }

        equipPlayerHub(player);

        player.setResourcePack("https://download.mc-packs.net/pack/a06aa20da5a12516673052a5af6f3b914e8dd242.zip", "a06aa20da5a12516673052a5af6f3b914e8dd242");
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        Player player = e.getPlayer();
        e.setQuitMessage("");

        // Show the leaving player to all online players
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (onlinePlayer.canSee(player)) {
                onlinePlayer.hidePlayer(this, player);
            }
        }

        player.teleport(Bukkit.getWorlds().getFirst().getSpawnLocation());

    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.LEFT_CLICK_AIR && e.getAction() != Action.LEFT_CLICK_BLOCK && e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (e.getPlayer().getInventory().getItemInMainHand().equals(SERVER_BROWSER_ITEM)) {
            GameManager.openServerMenu(e.getPlayer());
        } else if (e.getPlayer().getInventory().getItemInMainHand().equals(KIT_SELECTOR_ITEM)) {
            FighterManager.openKitMenu(e.getPlayer());
        } else if (e.getPlayer().getInventory().getItemInMainHand().equals(TELEPORT_HUB_ITEM)) {
            e.getPlayer().teleport(Bukkit.getWorlds().getFirst().getSpawnLocation());
        } else if (e.getPlayer().getInventory().getItemInMainHand().equals(VOTING_MENU_ITEM)) {
            SmashServer server = GameManager.getPlayerServer(e.getPlayer());
            if (server != null) {
                server.openVotingMenu(e.getPlayer());
            }
        }
    }

    @EventHandler
    public void onWeatherChangeEvent(WeatherChangeEvent e) {
        e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onItemPickup(PlayerPickupItemEvent e) {
        if (e.getPlayer().getGameMode() == GameMode.CREATIVE && e.getPlayer().isOp()) {
            return;
        }
        e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onItemDrop(PlayerDropItemEvent e) {
        if (e.getPlayer().getGameMode() == GameMode.CREATIVE && e.getPlayer().isOp()) {
            return;
        }
        e.setCancelled(true);
    }

    @EventHandler
    public void stopHealthRegen(EntityRegainHealthEvent e) {
        if (e.getRegainReason() == EntityRegainHealthEvent.RegainReason.SATIATED) {
            e.setCancelled(true);
        }
        if (e.getRegainReason() == EntityRegainHealthEvent.RegainReason.REGEN) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onLiquidFlow(BlockFromToEvent e) {
        if (e.getBlock().getType() == Material.WATER) {
            e.setCancelled(true);
        }
        if (e.getBlock().getType() == Material.LAVA) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onEatingFuckingFood(PlayerItemConsumeEvent e) {
        e.setCancelled(true);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent e) {
        Player player = e.getPlayer();
        Block blockIn = e.getTo().getBlock();
        SmashServer server = GameManager.getPlayerServer(player);
        if (server != null && blockIn.isLiquid() && DamageUtil.canDamage(player, null)) {
            Fighter fighter = FighterManager.getPlayerFighters().get(player);
            if (blockIn.getType() == Material.WATER) {
                if (fighter != null) {
                    return;
                }
            }
            boolean lighting = false;
            if (blockIn.getType() == Material.LAVA) {
                lighting = true;
            }
            if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
                return;
            }
            DamageUtil.borderKill(player, lighting);
        }
    }

    @EventHandler
    public void onCancelDamage(SmashDamageEvent e) {
        if (e.getDamageCause() == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION) {
            e.setCancelled(true);
        }
        if (e.getDamageCause() == EntityDamageEvent.DamageCause.WITHER) {
            e.setCancelled(true);
        }
        if (e.getDamageCause() == EntityDamageEvent.DamageCause.SUFFOCATION) {
            e.setCancelled(true);
        }
        if (e.getDamageCause() == EntityDamageEvent.DamageCause.FALL) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void clickEvent(InventoryClickEvent e) {
        if (e.getWhoClicked().getGameMode() == GameMode.CREATIVE && e.getWhoClicked().isOp()) {
            return;
        }
        e.setCancelled(true);
    }

    @EventHandler
    public void onToggleOffHand(PlayerSwapHandItemsEvent e) {
        e.setCancelled(true);
    }

    @EventHandler
    public void onEntityTarget(EntityTargetEvent e) {
        e.setCancelled(true);
    }

    @EventHandler
    public void onPlayerSwingHand(PlayerAnimationEvent e) {
        if (e.getAnimationType() != PlayerAnimationType.ARM_SWING) return;
        Player player = e.getPlayer();
        Fighter fighter = FighterManager.getPlayerFighters().get(player);
        if (fighter == null || fighter.getDisguise() == null) return;
        fighter.getDisguise().swingMainHand();
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public GameManager getGameManager() {
        return gameManager;
    }

    public BlockRestoreManager getBlockRestoreManager() {
        return blockRestoreManager;
    }

    public DamageManager getDamageManager() {
        return damageManager;
    }

    public EventManager getEventManager() {
        return eventManager;
    }

    public MenuManager getMenuManager() {
        return menuManager;
    }

    public CooldownManager getCooldownManager() {
        return cooldownManager;
    }
}

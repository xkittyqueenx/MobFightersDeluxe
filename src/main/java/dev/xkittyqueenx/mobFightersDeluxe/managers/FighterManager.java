package dev.xkittyqueenx.mobFightersDeluxe.managers;

import dev.xkittyqueenx.mobFightersDeluxe.MobFightersDeluxe;
import dev.xkittyqueenx.mobFightersDeluxe.abilities.Ability;
import dev.xkittyqueenx.mobFightersDeluxe.attributes.Attribute;
import dev.xkittyqueenx.mobFightersDeluxe.attributes.Ultimate;
import dev.xkittyqueenx.mobFightersDeluxe.events.PlayerDisguiseEvent;
import dev.xkittyqueenx.mobFightersDeluxe.fighters.Fighter;
import dev.xkittyqueenx.mobFightersDeluxe.fighters.SpectatorFighter;
import dev.xkittyqueenx.mobFightersDeluxe.managers.gamemodes.TrainingGamemode;
import dev.xkittyqueenx.mobFightersDeluxe.managers.gamestate.GameState;
import dev.xkittyqueenx.mobFightersDeluxe.managers.smashmenu.SmashMenu;
import dev.xkittyqueenx.mobFightersDeluxe.managers.smashserver.SmashServer;
import dev.xkittyqueenx.mobFightersDeluxe.utilities.Utils;
import me.arcaniax.hdb.api.HeadDatabaseAPI;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FighterManager implements Listener {

    private final static HashMap<Player, Fighter> playerFighters = new HashMap<>();
    private final Plugin plugin = MobFightersDeluxe.getInstance();

    public FighterManager() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public static void equipPlayer(Player player, Fighter check) {
        unequipPlayer(player);
        Fighter fighter;
        try {
            fighter = check.getClass().getDeclaredConstructor().newInstance();
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        playerFighters.put(player, fighter);
        Bukkit.getScheduler().runTaskLater(MobFightersDeluxe.getInstance(), () -> fighter.setOwner(player), 2L);
    }

    public static void equipPlayer(Player player, Fighter check, Map<Player, Float> ultimateChargeMap) {
        unequipPlayer(player);
        Fighter fighter;
        try {
            fighter = check.getClass().getDeclaredConstructor().newInstance();
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        playerFighters.put(player, fighter);
        Bukkit.getScheduler().runTaskLater(MobFightersDeluxe.getInstance(), () -> fighter.setOwner(player), 4L);
        if (ultimateChargeMap.containsKey(player)) {
            float storedCharge = ultimateChargeMap.get(player);
            for (Attribute attribute : fighter.getAttributes()) {
                if (attribute instanceof Ultimate ultimate) {
                    ultimate.setUltimateCharge(storedCharge, fighter);  // Restore charge value
                    ultimate.updateItemDurability();  // Ensure durability is updated
                }
            }
        }
    }

    public static void unequipPlayer(Player player) {
        Fighter fighter = playerFighters.get(player);
        if (fighter != null) {
            fighter.destroy();
            playerFighters.remove(player);
        }
    }

    public static void openKitMenu(Player player) {
        SmashServer server = GameManager.getPlayerServer(player);
        if(server == null) {
            return;
        }
        List<Fighter> allowed_fighters = server.getCurrentGamemode().getAllowedFighters();

        // Pagination variables
        int kitsPerPage = 21; // 7 kits per row × 3 rows
        int totalPages = (int) Math.ceil((double) allowed_fighters.size() / kitsPerPage);
        int currentPage = 1; // Start with page 1

        openKitSelectionPage(player, server, currentPage, totalPages, allowed_fighters);
    }

    private static void openKitSelectionPage(Player player, SmashServer server, int page, int totalPages, List<Fighter> allowed_fighters) {
        int size = 6; // Fixed size of 6 rows (54 slots)
        Inventory selectKit = Bukkit.createInventory(player, 9 * size, "Choose a Kit - Page " + page + "/" + totalPages);
        SmashMenu menu = MenuManager.createPlayerMenu(player, selectKit);
        HeadDatabaseAPI api = new HeadDatabaseAPI();

        // Calculate which kits to display on this page
        int kitsPerPage = 21;
        int startIndex = (page - 1) * kitsPerPage;
        int endIndex = Math.min(startIndex + kitsPerPage, allowed_fighters.size());

        // Display kits for current page
        int solo_slot = 10;
        int count = 0;

        for (int i = startIndex; i < endIndex; i++) {
            Fighter kit = allowed_fighters.get(i);
            ItemStack item = api.getItemHead(kit.getIcon());
            SkullMeta itemMeta = (SkullMeta) item.getItemMeta();
            itemMeta.customName(MiniMessage.miniMessage().deserialize(kit.getName()).decoration(TextDecoration.ITALIC, false));
            itemMeta.lore(kit.getLore());
            item.setItemMeta(itemMeta);

            selectKit.setItem(solo_slot, item);

            final Fighter finalKit = kit;
            menu.setActionFromSlot(solo_slot, (e) -> {
                if (e.getWhoClicked() instanceof Player clicked) {
                    if (server.getState() <= GameState.LOBBY_VOTING) {
                        server.pre_selected_fighters.put(clicked, finalKit);
                        clicked.playSound(clicked, finalKit.selectSound, 1f, 1f);
                    } else if (server.getState() == GameState.GAME_PLAYING) {
                        if (server.getCurrentGamemode() instanceof TrainingGamemode) {
                            if (playerFighters.get(clicked) instanceof SpectatorFighter) {
                                server.selected_fighter.put(clicked, finalKit);
                                clicked.playSound(clicked, finalKit.selectSound, 1f, 1f);
                                clicked.sendMessage(MiniMessage.miniMessage().deserialize("<gray>Your current fighter will be replaced with <white>" + finalKit.getGlyph() + " " + finalKit.getName() + " <gray>when you respawn."));
                            } else {
                                clicked.playSound(clicked, finalKit.selectSound, 1f, 1f);
                                FighterManager.equipPlayer(clicked, finalKit);
                            }
                        }
                    }
                    clicked.closeInventory();
                }
            });

            solo_slot++;
            count++;
            if (count % 7 == 0) {
                solo_slot += 2;
            }
        }

        // Add navigation buttons
        // Previous page button
        if (page > 1) {
            ItemStack prevPage = new ItemStack(Material.ARROW);
            ItemMeta prevMeta = prevPage.getItemMeta();
            prevMeta.setDisplayName(ChatColor.GREEN + "Previous Page");
            prevPage.setItemMeta(prevMeta);
            selectKit.setItem(45, prevPage);

            final int prevPageNum = page - 1;
            menu.setActionFromSlot(45, (e) -> {
                if (e.getWhoClicked() instanceof Player clicked) {
                    clicked.playSound(clicked.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                    MenuManager.removePlayerFromMenu(clicked);
                    openKitSelectionPage(clicked, server, prevPageNum, totalPages, allowed_fighters);
                }
            });
        }

        // Next page button
        if (page < totalPages) {
            ItemStack nextPage = new ItemStack(Material.ARROW);
            ItemMeta nextMeta = nextPage.getItemMeta();
            nextMeta.setDisplayName(ChatColor.GREEN + "Next Page");
            nextPage.setItemMeta(nextMeta);
            selectKit.setItem(53, nextPage);

            final int nextPageNum = page + 1;
            menu.setActionFromSlot(53, (e) -> {
                if (e.getWhoClicked() instanceof Player clicked) {
                    clicked.playSound(clicked.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                    MenuManager.removePlayerFromMenu(clicked);
                    openKitSelectionPage(clicked, server, nextPageNum, totalPages, allowed_fighters);
                }
            });
        }

        // Exit button
        ItemStack exit = new ItemStack(Material.BARRIER);
        ItemMeta exitMeta = exit.getItemMeta();
        exitMeta.setDisplayName(ChatColor.RED + "Exit");
        exit.setItemMeta(exitMeta);
        selectKit.setItem(49, exit);
        menu.setActionFromSlot(49, (e) -> {
            if (e.getWhoClicked() instanceof Player clicked) {
                clicked.playSound(clicked.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 1.0f);
                clicked.closeInventory();
            }
        });

        player.openInventory(selectKit);
    }

    public static Ability getCurrentAbility(Player player) {
        Fighter fighter = FighterManager.getPlayerFighters().get(player);
        if (fighter == null) {
            return null;
        }
        int currentSlot = player.getInventory().getHeldItemSlot();
        return fighter.getAbilityInSlot(currentSlot);
    }

    public static HashMap<Player, Fighter> getPlayerFighters() {
        return playerFighters;
    }

    @EventHandler
    public void onPlayerDisguise(PlayerDisguiseEvent e) {
        Utils.refreshDisguises();
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        playerFighters.remove(e.getPlayer());
    }

}

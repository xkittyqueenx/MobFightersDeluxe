package dev.xkittyqueenx.mobFightersDeluxe.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.xkittyqueenx.mobFightersDeluxe.MobFightersDeluxe;
import dev.xkittyqueenx.mobFightersDeluxe.managers.FighterManager;
import dev.xkittyqueenx.mobFightersDeluxe.managers.GameManager;
import dev.xkittyqueenx.mobFightersDeluxe.managers.smashserver.SmashServer;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.apache.commons.io.FileUtils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class CommandMobFighters {

    public static MiniMessage mm = MiniMessage.miniMessage();

    public static LiteralCommandNode<CommandSourceStack> hubCommand() {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("hub");
        root.executes(ctx -> {
            if (!(ctx.getSource().getExecutor() instanceof Player player)) {
                return Command.SINGLE_SUCCESS;
            }
            player.teleport(Bukkit.getWorlds().getFirst().getSpawnLocation());
            player.playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1.5f);
            return Command.SINGLE_SUCCESS;
        });
        return root.build();
    }

    public static LiteralCommandNode<CommandSourceStack> kitCommand() {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("fighter");
        root.executes(ctx -> {
            if (!(ctx.getSource().getExecutor() instanceof Player player)) {
                return Command.SINGLE_SUCCESS;
            }
            SmashServer server = GameManager.getPlayerServer(player);
            if(server == null) {
                return Command.SINGLE_SUCCESS;
            }
            player.playSound(player, Sound.ITEM_BOOK_PAGE_TURN, 1f, 1f);
            FighterManager.openKitMenu(player);
            return Command.SINGLE_SUCCESS;
        });
        return root.build();
    }

    public static LiteralCommandNode<CommandSourceStack> fightersCommand() {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("mf");
        root.then(Commands.literal("admin")
                .then(Commands.literal("reload")
                        .executes(ctx -> {
                            if (!(ctx.getSource().getExecutor() instanceof Player player)) {
                                return Command.SINGLE_SUCCESS;
                            }
                            if (!player.isOp()) {
                                return Command.SINGLE_SUCCESS;
                            }
                            MobFightersDeluxe.getInstance().getConfigManager().reloadFightersConfig();
                            return Command.SINGLE_SUCCESS;
                        })
                )
                .then(Commands.literal("world")
                        .executes(ctx -> {
                            if (!(ctx.getSource().getExecutor() instanceof Player player)) {
                                return Command.SINGLE_SUCCESS;
                            }
                            if (!player.isOp()) {
                                return Command.SINGLE_SUCCESS;
                            }
                            player.playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
                            printHeaderMessage(player, "World Commands");
                            printCommandMessage(player, "/mf admin world list", "Shows currently loaded worlds");
                            printCommandMessage(player, "/mf admin world create", "Copies a void world directory into the base maps folder with the name given");
                            printCommandMessage(player, "/mf admin world edit", "Loads and teleports you to one of the maps in the folder");
                            printCommandMessage(player, "/mf admin world unload", "Unloads the world by relative path to server folder");
                            printCommandMessage(player, "/mf admin world convert", "Converts an old world into a new world ready for deployment");
                            return Command.SINGLE_SUCCESS;
                        })
                        .then(Commands.literal("list")
                                .executes(ctx -> {
                                    if (!(ctx.getSource().getExecutor() instanceof Player player)) {
                                        Bukkit.shutdown();
                                        return Command.SINGLE_SUCCESS;
                                    }
                                    if (!player.isOp()) {
                                        return Command.SINGLE_SUCCESS;
                                    }
                                    listWorlds(player);
                                    return Command.SINGLE_SUCCESS;
                                })
                        )
                        .then(Commands.literal("create")
                                .then(Commands.argument("name", StringArgumentType.word())
                                        .executes(ctx -> {
                                            if (!(ctx.getSource().getExecutor() instanceof Player player)) {
                                                Bukkit.shutdown();
                                                return Command.SINGLE_SUCCESS;
                                            }
                                            if (!player.isOp()) {
                                                return Command.SINGLE_SUCCESS;
                                            }
                                            String name = ctx.getArgument("name", String.class);
                                            File maps_folder = new File("maps");
                                            if (!maps_folder.exists() || !maps_folder.isDirectory()) {
                                                player.sendMessage(mm.deserialize("<red>Could not find maps folder."));
                                                return Command.SINGLE_SUCCESS;
                                            }
                                            File void_map_directory = new File("maps/_VoidWorld");
                                            if (!void_map_directory.exists() || !void_map_directory.isDirectory()) {
                                                player.sendMessage(mm.deserialize("<red>Could not find void map folder."));
                                                return Command.SINGLE_SUCCESS;
                                            }
                                            File create_world_directory = new File(maps_folder.getPath() + "/" + name);
                                            if (create_world_directory.exists()) {
                                                player.sendMessage(mm.deserialize("<red>World folder already exists."));
                                                return Command.SINGLE_SUCCESS;
                                            }
                                            try {
                                                FileUtils.copyDirectory(void_map_directory, create_world_directory);
                                            } catch(Exception e) {
                                                e.printStackTrace();
                                            }
                                            if (create_world_directory.exists()) {
                                                player.sendMessage(mm.deserialize("<yellow>Successfully copied world folder."));
                                            } else {
                                                player.sendMessage(mm.deserialize("<red>Unable to create world folder."));
                                            }
                                            return Command.SINGLE_SUCCESS;
                                        })
                                )
                        )
                        .then(Commands.literal("edit")
                                .then(Commands.argument("name", StringArgumentType.greedyString())
                                        .executes(ctx -> {
                                            if (!(ctx.getSource().getExecutor() instanceof Player player)) {
                                                Bukkit.shutdown();
                                                return Command.SINGLE_SUCCESS;
                                            }
                                            if (!player.isOp()) {
                                                return Command.SINGLE_SUCCESS;
                                            }
                                            String name = ctx.getArgument("name", String.class);
                                            File maps_folder = new File("maps");
                                            if (!maps_folder.exists() || !maps_folder.isDirectory()) {
                                                player.sendMessage("Could not find maps folder.");
                                                return Command.SINGLE_SUCCESS;
                                            }
                                            File[] files = maps_folder.listFiles();
                                            List<File> world_directories = new ArrayList<File>();
                                            if (files == null) {
                                                player.sendMessage("Maps folder was empty.");
                                                return Command.SINGLE_SUCCESS;
                                            }
                                            for (File file : files) {
                                                if (!file.isDirectory()) {
                                                    continue;
                                                }
                                                if(file.getName().equalsIgnoreCase("_Copies")) {
                                                    continue;
                                                }
                                                File region_directory = new File(file.getPath() + "/region");
                                                if (region_directory.exists()) {
                                                    world_directories.add(file);
                                                    continue;
                                                }
                                                File[] sub_files = file.listFiles();
                                                if (sub_files == null) {
                                                    continue;
                                                }
                                                for (File sub_file : sub_files) {
                                                    if (!sub_file.isDirectory()) {
                                                        continue;
                                                    }
                                                    File sub_region_directory = new File(sub_file.getPath() + "/region");
                                                    if (sub_region_directory.exists()) {
                                                        world_directories.add(sub_file);
                                                    }
                                                }
                                            }
                                            for (File file : world_directories) {
                                                if (file.getPath().equalsIgnoreCase(name)) {
                                                    editWorld(player, file.getPath());
                                                    return Command.SINGLE_SUCCESS;
                                                }
                                            }
                                            // Check for matched names
                                            for (File file : world_directories) {
                                                if (file.getPath().contains(name)) {
                                                    editWorld(player, file.getPath());
                                                    return Command.SINGLE_SUCCESS;
                                                }
                                            }
                                            player.sendMessage("Could not find world.");
                                            return Command.SINGLE_SUCCESS;
                                        })
                                )
                        )
                        .then(Commands.literal("unload")
                                .then(Commands.argument("name", StringArgumentType.greedyString())
                                        .executes(ctx -> {
                                            if (!(ctx.getSource().getExecutor() instanceof Player player)) {
                                                Bukkit.shutdown();
                                                return Command.SINGLE_SUCCESS;
                                            }
                                            if (!player.isOp()) {
                                                return Command.SINGLE_SUCCESS;
                                            }
                                            String name = ctx.getArgument("name", String.class);
                                            for (World world : Bukkit.getWorlds()) {
                                                if (world.getName().contains(name)) {
                                                    if (unloadWorld(world.getName())) {
                                                        player.sendMessage("World Saved!");
                                                        return Command.SINGLE_SUCCESS;
                                                    }
                                                }
                                            }
                                            player.sendMessage("Could not find world specified.");
                                            return Command.SINGLE_SUCCESS;
                                        })
                                )
                        )
                        .then(Commands.literal("convert")
                                .executes(ctx -> {
                                    if (!(ctx.getSource().getExecutor() instanceof Player player)) {
                                        Bukkit.shutdown();
                                        return Command.SINGLE_SUCCESS;
                                    }
                                    if (!player.isOp()) {
                                        return Command.SINGLE_SUCCESS;
                                    }
                                    convertWorld(ctx.getSource().getSender());
                                    return Command.SINGLE_SUCCESS;
                                })
                        )
                )
        );
        return root.build();
    }

    public static void printHeaderMessage(Player player, String header_message) {
        player.sendMessage(mm.deserialize("<white>----- </white><yellow>" + header_message + "</yellow><white> -----</white>"));
    }

    public static void printCommandMessage(Player player, String command, String description) {
        player.sendMessage(mm.deserialize("<gray>" + command + "<white> - <gray>" + description));
    }

    public static void listWorlds(Player player) {
        printHeaderMessage(player, "World List");
        File maps_folder = new File("maps");
        if (!maps_folder.exists() || !maps_folder.isDirectory()) {
            player.sendMessage("Could not find maps folder.");
            return;
        }
        File[] files = maps_folder.listFiles();
        List<File> world_directories = new ArrayList<>();
        if (files == null) {
            player.sendMessage("Maps folder was empty.");
            return;
        }
        for (File file : files) {
            if (!file.isDirectory()) {
                continue;
            }
            if(file.getName().equalsIgnoreCase("_Copies")) {
                continue;
            }
            File region_directory = new File(file.getPath() + "/region");
            if (region_directory.exists()) {
                world_directories.add(file);
                continue;
            }
            File[] sub_files = file.listFiles();
            if (sub_files == null) {
                continue;
            }
            for (File sub_file : sub_files) {
                if (!sub_file.isDirectory()) {
                    continue;
                }
                File sub_region_directory = new File(sub_file.getPath() + "/region");
                if (sub_region_directory.exists()) {
                    world_directories.add(sub_file);
                }
            }
        }
        for (File file : world_directories) {
            player.sendMessage(mm.deserialize("<yellow>" + file.getPath()));
        }
    }

    public static void editWorld(Player player, String world_directory_path) {
        World world = loadWorld(world_directory_path);
        if (world == null) {
            player.sendMessage("Failed to load world.");
            return;
        }
        player.sendMessage("Started Editing: " + world_directory_path);
        player.teleport(world.getSpawnLocation());
    }

    public static World loadWorld(String world_directory_path) {
        WorldCreator worldCreator = new WorldCreator(world_directory_path);
        return worldCreator.createWorld();
    }

    public static boolean unloadWorld(String name) {
        World world = Bukkit.getWorld(name);
        if (world == null) {
            return false;
        }
        for (Player player : world.getPlayers()) {
            player.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
        }
        Bukkit.unloadWorld(world, true);
        return true;
    }

    public static void convertWorld(CommandSender commandSender) {
        if (!(commandSender instanceof Player player)) {
            return;
        }
        commandSender.sendMessage("Converting...");
        World world = player.getWorld();
        Block center = world.getSpawnLocation().getBlock();
        // Parse map for other objects
        int parse_radius = 150;
        for (int x = -parse_radius; x <= parse_radius; x++) {
            for (int y = -parse_radius; y <= parse_radius; y++) {
                for (int z = -parse_radius; z <= parse_radius; z++) {
                    Block parsed = center.getRelative(x, y, z);
                    if(isCenterPoint(parsed) || isRespawnPoint(parsed) || isBoundaryPoint(parsed) || isCapturePoint(parsed) || isKitChangePoint(parsed) || isSmallHealthPack(parsed) || isLargeHealthPack(parsed) || isDefenderSpawnPoint(parsed)) {
                        // Remove all existing item frames on the block
                        for(Entity entity : parsed.getWorld().getNearbyEntities(parsed.getLocation().add(0.5, 0.5, 0.5), 1, 1, 1)) {
                            if(entity instanceof ItemFrame) {
                                entity.remove();
                            }
                        }
                        // For some reason existing item frames take a while to remove so this has to be delayed
                        Bukkit.getScheduler().runTaskLater(MobFightersDeluxe.getInstance(), new Runnable() {
                            @Override
                            public void run() {
                                ItemFrame frame = world.spawn(parsed.getRelative(BlockFace.SOUTH).getLocation(), ItemFrame.class);
                                frame.setFacingDirection(BlockFace.SOUTH);
                            }
                        }, 40L);
                    }
                }
            }
        }
        Bukkit.getScheduler().runTaskLater(MobFightersDeluxe.getInstance(), () -> commandSender.sendMessage("Finished Converting."), 50L);
    }

    public static boolean isRespawnPoint(Block check) {
        if (check.getType() != Material.GREEN_WOOL) {
            return false;
        }
        Block plate = check.getRelative(0, 1, 0);
        return (plate.getType() == Material.LIGHT_WEIGHTED_PRESSURE_PLATE);
    }

    public static boolean isBoundaryPoint(Block check) {
        if (check.getType() != Material.RED_WOOL) {
            return false;
        }
        Block plate = check.getRelative(0, 1, 0);
        return (plate.getType() == Material.LIGHT_WEIGHTED_PRESSURE_PLATE);
    }

    public static boolean isCenterPoint(Block check) {
        if (check.getType() != Material.WHITE_WOOL) {
            return false;
        }
        Block plate = check.getRelative(0, 1, 0);
        return (plate.getType() == Material.LIGHT_WEIGHTED_PRESSURE_PLATE);
    }

    public static boolean isCapturePoint(Block check) {
        if (check.getType() != Material.BLUE_WOOL) {
            return false;
        }
        Block plate = check.getRelative(0, 1, 0);
        return (plate.getType() == Material.HEAVY_WEIGHTED_PRESSURE_PLATE);
    }

    public static boolean isKitChangePoint(Block check) {
        if (check.getType() != Material.ORANGE_WOOL) {
            return false;
        }
        Block plate = check.getRelative(0, 1, 0);
        return (plate.getType() == Material.LIGHT_WEIGHTED_PRESSURE_PLATE);
    }

    public static boolean isSmallHealthPack(Block check) {
        if (check.getType() != Material.PINK_WOOL) {
            return false;
        }
        Block plate = check.getRelative(0, 1, 0);
        return (plate.getType() == Material.AIR || plate.getType() == Material.LIGHT_WEIGHTED_PRESSURE_PLATE);
    }

    public static boolean isLargeHealthPack(Block check) {
        if (check.getType() != Material.YELLOW_WOOL) {
            return false;
        }
        Block plate = check.getRelative(0, 1, 0);
        return (plate.getType() == Material.AIR || plate.getType() == Material.LIGHT_WEIGHTED_PRESSURE_PLATE);
    }

    public static boolean isDefenderSpawnPoint(Block check) {
        if (check.getType() != Material.PURPLE_WOOL) {
            return false;
        }
        Block plate = check.getRelative(0, 1, 0);
        return (plate.getType() == Material.AIR || plate.getType() == Material.LIGHT_WEIGHTED_PRESSURE_PLATE);
    }

}

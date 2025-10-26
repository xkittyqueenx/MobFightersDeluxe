package dev.xkittyqueenx.mobFightersDeluxe.utilities;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class BlocksUtil {

    public static HashSet<Material> blockUseSet = new HashSet<>();;
    public static HashSet<Material> blockAirFoliageSet = new HashSet<>();

    static {
        // Air and foliage blocks
        blockAirFoliageSet.add(Material.AIR);
        blockAirFoliageSet.add(Material.SHORT_GRASS);
        blockAirFoliageSet.add(Material.TALL_GRASS);
        blockAirFoliageSet.add(Material.DEAD_BUSH);
        blockAirFoliageSet.add(Material.DANDELION);
        blockAirFoliageSet.add(Material.POPPY);
        blockAirFoliageSet.add(Material.BROWN_MUSHROOM);
        blockAirFoliageSet.add(Material.RED_MUSHROOM);
        blockAirFoliageSet.add(Material.FIRE);
        blockAirFoliageSet.add(Material.WHEAT);
        blockAirFoliageSet.add(Material.PUMPKIN_STEM);
        blockAirFoliageSet.add(Material.MELON_STEM);
        blockAirFoliageSet.add(Material.NETHER_WART);
        blockAirFoliageSet.add(Material.TRIPWIRE_HOOK);
        blockAirFoliageSet.add(Material.TRIPWIRE);
        blockAirFoliageSet.add(Material.CARROTS);
        blockAirFoliageSet.add(Material.POTATOES);
        blockAirFoliageSet.add(Material.WHITE_BANNER);

        // Usable blocks
        blockUseSet.add(Material.DISPENSER);
        blockUseSet.add(Material.WHITE_BED); // Add other bed colors if needed
        blockUseSet.add(Material.PISTON);
        blockUseSet.add(Material.BOOKSHELF);
        blockUseSet.add(Material.CHEST);
        blockUseSet.add(Material.CRAFTING_TABLE);
        blockUseSet.add(Material.FURNACE);
        blockUseSet.add(Material.OAK_DOOR);
        blockUseSet.add(Material.LEVER);
        blockUseSet.add(Material.IRON_DOOR);
        blockUseSet.add(Material.STONE_BUTTON);
        blockUseSet.add(Material.OAK_FENCE);
        blockUseSet.add(Material.REPEATER);
        blockUseSet.add(Material.OAK_TRAPDOOR);
        blockUseSet.add(Material.OAK_FENCE_GATE);
        blockUseSet.add(Material.NETHER_BRICK_FENCE);
        blockUseSet.add(Material.ENCHANTING_TABLE);
        blockUseSet.add(Material.BREWING_STAND);
        blockUseSet.add(Material.ENDER_CHEST);
        blockUseSet.add(Material.ANVIL);
        blockUseSet.add(Material.CAULDRON);
        blockUseSet.add(Material.TRAPPED_CHEST);
        blockUseSet.add(Material.HOPPER);
        blockUseSet.add(Material.DROPPER);

        // Modern fence gates
        blockUseSet.add(Material.BIRCH_FENCE_GATE);
        blockUseSet.add(Material.JUNGLE_FENCE_GATE);
        blockUseSet.add(Material.DARK_OAK_FENCE_GATE);
        blockUseSet.add(Material.ACACIA_FENCE_GATE);
        blockUseSet.add(Material.SPRUCE_FENCE_GATE);

        // Modern doors
        blockUseSet.add(Material.SPRUCE_DOOR);
        blockUseSet.add(Material.BIRCH_DOOR);
        blockUseSet.add(Material.JUNGLE_DOOR);
        blockUseSet.add(Material.ACACIA_DOOR);
        blockUseSet.add(Material.DARK_OAK_DOOR);
    }

    public static boolean isAirOrFoliage(Block block) {
        return blockAirFoliageSet.contains(block.getType());
    }

    public static boolean isUsable(Block block) {
        return blockUseSet.contains(block.getType());
    }

    public static List<Block> getBlocks(Location start, int radius) {
        if (radius <= 0) {
            return new ArrayList<Block>(0);
        }
        int iterations = (radius * 2) + 1;
        List<Block> blocks = new ArrayList<Block>(iterations * iterations * iterations);
        blocks.add((Block) start.getBlock());
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    blocks.add((Block) start.getBlock().getRelative(x, y, z));
                }
            }
        }
        return blocks;
    }

    public static HashMap<Block, Double> getInRadius(Location location, double dR) {
        return getInRadius(location, dR, 9999);
    }

    public static HashMap<Block, Double> getInRadius(Location location, double dR, double maxHeight) {
        HashMap<Block, Double> blockList = new HashMap<Block, Double>();
        int iR = (int) dR + 1;
        for (int x = -iR; x <= iR; x++) {
            for (int z = -iR; z <= iR; z++) {
                for (int y = -iR; y <= iR; y++) {
                    if (Math.abs(y) > maxHeight) {
                        continue;
                    }
                    Block curBlock = location.getWorld().getBlockAt(
                            (int) (location.getX() + x), (int) (location.getY() + y), (int) (location.getZ() + z));
                    double offset = location.distance(curBlock.getLocation().add(0.5, 0.5, 0.5));
                    if (offset <= dR) {
                        blockList.put(curBlock, 1 - (offset / dR));
                    }
                }
            }
        }
        return blockList;
    }

}

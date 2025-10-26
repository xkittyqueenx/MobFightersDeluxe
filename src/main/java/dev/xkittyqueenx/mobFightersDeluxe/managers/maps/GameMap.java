package dev.xkittyqueenx.mobFightersDeluxe.managers.maps;

import dev.xkittyqueenx.mobFightersDeluxe.MobFightersDeluxe;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class GameMap extends SmashMap {

    protected String created_by = "urpcketgf";
    private List<Location> respawn_points = new ArrayList<Location>();
    private List<Location> control_points = new ArrayList<Location>();
    private List<Location> kit_change_points = new ArrayList<>();
    private List<Location> defenderSpawnPoints = new ArrayList<>();
    protected List<Player> voted = new ArrayList<Player>();
    private Vector boundary_min = null;
    private Vector boundary_max = null;

    public GameMap(File file) {
        super(file);
        this.parse_chunk_radius = 10;
        name = file.getName().replace("_", " ");
        name = Character.toUpperCase(name.charAt(0)) + name.substring(1);
        File name_file = new File(map_directory.getPath() + "/map_name.txt");
        File created_by_file = new File(map_directory.getPath() + "/created_by.txt");
        try {
            if (name_file.exists() && name_file.length() != 0) {
                name = Files.readString(name_file.toPath());
            }
            else {
                if(!name_file.exists()) {
                    name_file.createNewFile();
                }
            }
            if(created_by_file.exists() && created_by_file.length() != 0) {
                created_by = Files.readString(created_by_file.toPath());
            }
            else {
                if(!created_by_file.exists()) {
                    created_by_file.createNewFile();
                }
            }
        } catch (Exception e) {
            Bukkit.broadcastMessage(ChatColor.RED + "Failed to read map files");
        }
    }

    @Override
    public void createWorld() {
        respawn_points.clear();
        control_points.clear();
        kit_change_points.clear();
        boundary_min = null;
        boundary_max = null;
        super.createWorld();
    }

    public boolean parseBlock(Block parsed) {
        if(isRespawnPoint(parsed)) {
            respawn_points.add(parsed.getLocation());
            MobFightersDeluxe.getInstance().getComponentLogger().info(MiniMessage.miniMessage().deserialize("Respawn Point added."));
            return true;
        }
        if(isBoundaryPoint(parsed)) {
            addBoundaryPoint(parsed.getLocation());
            MobFightersDeluxe.getInstance().getComponentLogger().info(MiniMessage.miniMessage().deserialize("Boundary Point added."));
            return true;
        }
        if(isCenterPoint(parsed)) {
            world.setSpawnLocation(parsed.getX(), parsed.getY(), parsed.getZ());
            MobFightersDeluxe.getInstance().getComponentLogger().info(MiniMessage.miniMessage().deserialize("Center Point added."));
            return true;
        }
        if (isControlPoint(parsed)) {
            control_points.add(parsed.getLocation());
            return true;
        }
        if(isKitChangePoint(parsed)) {  // Add this check
            kit_change_points.add(parsed.getLocation());
            return true;
        }
        if(isDefenderSpawnPoint(parsed)) {
            defenderSpawnPoints.add(parsed.getLocation());
            return true;
        }
        if(isSmallHealthPack(parsed)) {
            smallHealthPackLocations.add(parsed.getLocation().add(0, 1, 0));
            return true;
        }
        if(isLargeHealthPack(parsed)) {
            largeHealthPackLocations.add(parsed.getLocation().add(0, 1, 0));
            return true;
        }
        return false;
    }

    public List<Location> getControlPoints() {
        return control_points;
    }

    public List<Location> getRespawnPoints() {
        return respawn_points;
    }

    public String getCreatedBy() {
        return created_by;
    }

    public List<Player> getVoted() {
        for (Player player : voted) {
            if (!player.isOnline()) {
                voted.remove(player);
            }
        }
        return voted;
    }

    private List<Location> smallHealthPackLocations = new ArrayList<>();
    private List<Location> largeHealthPackLocations = new ArrayList<>();

    public List<Location> getSmallHealthPackLocations() {
        return smallHealthPackLocations;
    }

    public List<Location> getLargeHealthPackLocations() {
        return largeHealthPackLocations;
    }

    private Location getControlPoint(Block check) {
        if (check.getType() != Material.BLUE_WOOL) {
            return null;
        }
        Block plate = check.getRelative(0, 1, 0);
        return plate.getLocation();
    }

    public List<Location> getKitChangePoints() {
        return kit_change_points;
    }

    public boolean isKitChangePoint(Block check) {
        if (check.getType() != Material.ORANGE_WOOL) {
            return false;
        }
        Block plate = check.getRelative(0, 1, 0);
        return (plate.getType() == Material.LIGHT_WEIGHTED_PRESSURE_PLATE);
    }

    public boolean isDefenderSpawnPoint(Block check) {
        if (check.getType() != Material.PURPLE_WOOL) {
            return false;
        }
        Block plate = check.getRelative(0, 1, 0);
        return (plate.getType() == Material.LIGHT_WEIGHTED_PRESSURE_PLATE);
    }

    public boolean isControlPoint(Block check) {
        if (check.getType() != Material.BLUE_WOOL) {
            return false;
        }
        Block plate = check.getRelative(0, 1, 0);
        return (plate.getType() == Material.HEAVY_WEIGHTED_PRESSURE_PLATE);
    }

    public void clearVoted() {
        voted.clear();
    }

    public void addBoundaryPoint(Location location) {
        if(boundary_min == null) {
            boundary_min = location.toVector();
        }
        if(boundary_max == null) {
            boundary_max = location.toVector();
        }
        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();
        boundary_min.setX(Math.min(x, boundary_min.getX()));
        boundary_min.setY(Math.min(y, boundary_min.getY()));
        boundary_min.setZ(Math.min(z, boundary_min.getZ()));
        boundary_max.setX(Math.max(x, boundary_max.getX()));
        boundary_max.setY(Math.max(y, boundary_max.getY()));
        boundary_max.setZ(Math.max(z, boundary_max.getZ()));
    }

    public boolean isOutOfBounds(Entity entity) {
        if(boundary_min == null || boundary_max == null) {
            return false;
        }
        if(entity == null) {
            return false;
        }
        if(world == null || !entity.getWorld().equals(world)) {
            return false;
        }
        Vector vector = entity.getLocation().toVector();
        if(vector.getX() < boundary_min.getX() || vector.getX() > boundary_max.getX()) {
            return true;
        }
        if(vector.getY() < boundary_min.getY() || vector.getY() > boundary_max.getY()) {
            return true;
        }
        if(vector.getZ() < boundary_min.getZ() || vector.getZ() > boundary_max.getZ()) {
            return true;
        }
        return false;
    }

    public Vector getBoundaryMin() {
        return boundary_min;
    }

    public Vector getBoundaryMax() {
        return boundary_max;
    }

    public boolean isRespawnPoint(Block check) {
        if (check.getType() != Material.GREEN_WOOL) {
            return false;
        }
        Block plate = check.getRelative(0, 1, 0);
        return (plate.getType() == Material.LIGHT_WEIGHTED_PRESSURE_PLATE);
    }

    public List<Location> getDefenderSpawnPoints() {
        return defenderSpawnPoints;
    }

    public boolean isBoundaryPoint(Block check) {
        if (check.getType() != Material.RED_WOOL) {
            return false;
        }
        Block plate = check.getRelative(0, 1, 0);
        return (plate.getType() == Material.LIGHT_WEIGHTED_PRESSURE_PLATE);
    }

    public boolean isCenterPoint(Block check) {
        if (check.getType() != Material.WHITE_WOOL) {
            return false;
        }
        Block plate = check.getRelative(0, 1, 0);
        return (plate.getType() == Material.LIGHT_WEIGHTED_PRESSURE_PLATE);
    }

    public boolean isSmallHealthPack(Block check) {
        if (check.getType() != Material.PINK_WOOL) {
            return false;
        }
        Block plate = check.getRelative(0, 1, 0);
        return (plate.getType() == Material.AIR || plate.getType() == Material.LIGHT_WEIGHTED_PRESSURE_PLATE);
    }

    public boolean isLargeHealthPack(Block check) {
        if (check.getType() != Material.YELLOW_WOOL) {
            return false;
        }
        Block plate = check.getRelative(0, 1, 0);
        return (plate.getType() == Material.AIR || plate.getType() == Material.LIGHT_WEIGHTED_PRESSURE_PLATE);
    }

    public Component toComponent() {
        return MiniMessage.miniMessage().deserialize("<#F4BB44>Map - " + "<#A7C7E7><b>" + getName());
    }

}

package dev.xkittyqueenx.mobFightersDeluxe.utilities;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.List;

public class Hitscan {

    private final Location start;
    private Vector direction;
    private Location lastLocation;

    private double curRange;
    private final double incrementedRange;
    private final double maxRange;

    private boolean ignoreAllBlocks;

    private final Particle particle;
    private final List<Player> toDisplay;

    private Color color;
    private int size;

    public boolean ownerOnly = false;

    public Hitscan(Location start, Vector direction, double incrementedRange, double maxRange, Particle particle, List<Player> toDisplay, Color color, int size) {
        this(start, null, direction, incrementedRange, maxRange, particle, toDisplay, color, size);
    }

    public Hitscan(Location start, Vector direction, double incrementedRange, double maxRange, Particle particle, List<Player> toDisplay, Color color, int size, boolean ownerOnly) {
        this(start, null, direction, incrementedRange, maxRange, particle, toDisplay, color, size, ownerOnly);
    }

    public Hitscan(Location start, Location end, Vector direction, double incrementedRange, double maxRange, Particle particle, List<Player> toDisplay, Color color, int size) {
        this.start = start;
        this.direction = direction;
        this.lastLocation = start;

        this.curRange = 0;
        this.incrementedRange = incrementedRange;
        this.maxRange = maxRange;

        this.particle = particle;
        this.toDisplay = toDisplay;

        this.color = color;
        this.size = size;

        if (this.direction == null) {
            this.direction = end.clone().subtract(start.clone()).toVector().normalize();
        }
    }

    public Hitscan(Location start, Location end, Vector direction, double incrementedRange, double maxRange, Particle particle, List<Player> toDisplay, Color color, int size, boolean ownerOnly) {
        this.start = start;
        this.direction = direction;
        this.lastLocation = start;

        this.curRange = 0;
        this.incrementedRange = incrementedRange;
        this.maxRange = maxRange;

        this.particle = particle;
        this.toDisplay = toDisplay;

        this.color = color;
        this.size = size;

        this.ownerOnly = ownerOnly;

        if (this.direction == null) {
            this.direction = end.clone().subtract(start.clone()).toVector().normalize();
        }
    }

    public boolean update() {
        boolean done = curRange > maxRange;
        Location newTarget = start.clone().add(direction.clone().multiply(curRange));

        if (newTarget.getY() < 0) {
            newTarget.add(0, 0.2, 0);
        }

        lastLocation = newTarget;

        if (!ignoreAllBlocks && newTarget.getBlock().getType() == Material.COBWEB) {
            done = true;
        }

        if (!ignoreAllBlocks && newTarget.getBlock().getType().isSolid()) {
            done = true;
        }

        curRange += incrementedRange;

        if (particle == Particle.DUST) {
            newTarget.getWorld().spawnParticle(particle, newTarget, 0, 0, 0, 0, 10, new Particle.DustOptions(color, size), true);

        } else if (particle == Particle.EFFECT) {
            newTarget.getWorld().spawnParticle(particle, newTarget, 1, 0, 0, 0, 0, color, true);
        } else if (particle == Particle.SCULK_CHARGE) {
            newTarget.getWorld().spawnParticle(particle, newTarget, 1, 0, 0, 0, 0, (float) size, true);
        } else if (particle == Particle.DOLPHIN) {

        } else if (particle == Particle.ENTITY_EFFECT) {
            newTarget.getWorld().spawnParticle(particle, newTarget, 3, 0.4f, 0.4f, 0.4f, 0, color, true);
        } else {
            newTarget.getWorld().spawnParticle(particle, newTarget, 1, 0, 0, 0, 0, null, true);
        }

        return done;
    }

    public void setIgnoreAllBlocks(boolean b) {
        ignoreAllBlocks = b;
    }

    public Location getLastLocation() {
        return lastLocation;
    }

    public Location getDestination() {
        return lastLocation.add(direction);
    }

}

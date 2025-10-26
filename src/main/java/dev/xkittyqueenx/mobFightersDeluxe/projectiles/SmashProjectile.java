package dev.xkittyqueenx.mobFightersDeluxe.projectiles;

import dev.xkittyqueenx.mobFightersDeluxe.MobFightersDeluxe;
import dev.xkittyqueenx.mobFightersDeluxe.fighters.Fighter;
import dev.xkittyqueenx.mobFightersDeluxe.managers.FighterManager;
import dev.xkittyqueenx.mobFightersDeluxe.utilities.BlocksUtil;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.*;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public abstract class SmashProjectile extends BukkitRunnable implements Listener {

    protected static Plugin plugin = MobFightersDeluxe.getInstance();
    protected Player firer;
    protected Player owner;
    protected String name;
    protected Entity projectile;
    protected double damage;
    protected double hitbox_size;
    protected double knockback_mult;
    protected long expiration_ticks = 300;
    protected boolean running = false;
    protected boolean entityDetection = true;
    protected boolean blockDetection = true;
    protected boolean idleDetection = true;

    public SmashProjectile(Player firer, String name) {
        this.firer = firer;
        this.name = name;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void launchProjectile() {
        setProjectileEntity(createProjectileEntity());
        this.doVelocity();
        this.runTaskTimer(plugin, 0L, 0L);
    }

    @Override
    public void run() {
        running = true;
        if (projectile == null || !projectile.isValid() || !projectile.getWorld().equals(firer.getWorld())) {
            destroy();
            return;
        }
        if (projectile.getTicksLived() > getExpirationTicks()) {
            if (onExpire()) {
                destroy();
                return;
            }
        }
        // Check if we hit an entity first
        if(entityDetection) {
            LivingEntity target = checkClosestTarget();
            if (target != null) {
                if (onHitLivingEntity(target)) {
                    playHitSound();
                    destroy();
                    return;
                }
            }
        }
        // Check if we hit a block next
        if(blockDetection) {
            Block block = checkHitBlock();
            if (block != null) {
                if (onHitBlock(block)) {
                    destroy();
                    return;
                }
            }
        }
        // Check if we're idle next
        if(idleDetection) {
            if (checkIdle()) {
                if (onIdle()) {
                    destroy();
                    return;
                }
            }
        }
        doEffect();
    }

    @Override
    public synchronized void cancel() {
        running = false;
        super.cancel();
    }

    public void destroy() {
        if (projectile != null) {
            projectile.remove();
        }
        if(running) {
            this.cancel();
        }
        HandlerList.unregisterAll(this);

    }

    protected boolean canHitEntity(Entity entity) {
        return true;
    }

    protected void playHitSound() {
        firer.playSound(firer.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.25f);
    }

    protected LivingEntity checkClosestTarget() {
        // If the projectile is not valid, return null
        if (projectile == null || !projectile.isValid()) {
            return null;
        }

        Location projectilePos = projectile.getLocation();
        Vector projectileMotion = projectile.getVelocity();

        // Calculate the magnitude of velocity for optimization
        double velocityMagnitude = projectileMotion.length();

        // Adjust iterations based on velocity - faster projectiles need more checks
        int iterations = Math.max(5, Math.min(20, (int)(velocityMagnitude * 5)));

        // Create a slightly larger search box based on velocity and hitbox size
        double searchRadius = hitbox_size + velocityMagnitude + 1.0;

        // Get potential targets more efficiently
        List<LivingEntity> potentialTargets = new ArrayList<>();
        for (Entity entity : projectile.getNearbyEntities(searchRadius, searchRadius, searchRadius)) {
            // Skip non-living entities, the firer, and the projectile itself
            if (!(entity instanceof LivingEntity) || entity.equals(firer) || entity.equals(projectile)) {
                continue;
            }

            // Skip entities we can't hit
            if (!canHitEntity(entity)) {
                continue;
            }

            // Skip intangible players
            if (entity instanceof Player player) {
                Fighter kit = FighterManager.getPlayerFighters().get(player);
                if (kit != null && kit.isIntangible()) {
                    continue;
                }
            }

            potentialTargets.add((LivingEntity) entity);
        }

        // If no potential targets, return early
        if (potentialTargets.isEmpty()) {
            return null;
        }

        // Find the closest collision along the path
        LivingEntity closestTarget = null;
        double closestDistance = Double.MAX_VALUE;

        // Check for collisions along the path
        for (int i = 0; i < iterations; i++) {
            double t = i / (double)(iterations - 1);

            // Calculate projectile position at this step
            Location stepPos = projectilePos.clone().add(
                    projectileMotion.clone().multiply(t)
            );

            // Create projectile hitbox for this step
            BoundingBox stepHitbox = BoundingBox.of(
                    stepPos,
                    hitbox_size * 2, // width
                    hitbox_size * 2, // height
                    hitbox_size * 2  // depth
            );

            for (LivingEntity target : potentialTargets) {
                Vector targetMotion = target.getVelocity();

                if (target instanceof Player player) {
                    Fighter fighter = FighterManager.getPlayerFighters().get(player);
                    BoundingBox targetBB = fighter.getDisguise().getBoundingBox();
                    // Calculate target position at this step
                    BoundingBox targetStepBB = targetBB.clone().shift(
                            targetMotion.getX() * t,
                            targetMotion.getY() * t,
                            targetMotion.getZ() * t
                    );

                    // Check for intersection
                    if (stepHitbox.overlaps(targetStepBB)) {
                        // Calculate distance to determine the closest target
                        double distance = projectilePos.distance(targetStepBB.getCenter().toLocation(player.getWorld()));

                        if (distance < closestDistance) {
                            closestDistance = distance;
                            closestTarget = target;
                        }
                    }
                } else {
                    BoundingBox targetBB = target.getBoundingBox();
                    BoundingBox targetStepBB = targetBB.clone().shift(
                            targetMotion.getX() * t,
                            targetMotion.getY() * t,
                            targetMotion.getZ() * t
                    );

                    // Check for intersection
                    if (stepHitbox.overlaps(targetStepBB)) {
                        // Calculate distance to determine the closest target
                        double distance = projectilePos.distance(targetStepBB.getCenter().toLocation(target.getWorld()));

                        if (distance < closestDistance) {
                            closestDistance = distance;
                            closestTarget = target;
                        }
                    }
                }
            }

            // If we found a target, no need to check further steps
            if (closestTarget != null) {
                break;
            }
        }

        return closestTarget;
    }


    // This modifies projectile motion and location, this can cause
    // Bugs with projectiles that do not delete themselves
    protected Block checkHitBlock() {
        World world = projectile.getWorld();
        Location currentLoc = projectile.getLocation();
        Vector velocity = projectile.getVelocity();

        // Calculate the next position based on velocity
        Location nextLoc = currentLoc.clone().add(velocity);

        // Do ray tracing between current and next position
        RayTraceResult rayTrace = world.rayTraceBlocks(
                currentLoc,
                velocity,
                velocity.length(),
                FluidCollisionMode.NEVER,
                true
        );

        if (rayTrace == null) {
            return null;
        }

        Block hitBlock = rayTrace.getHitBlock();
        if (hitBlock == null || hitBlock.isLiquid() || !hitBlock.getType().isSolid()) {
            return null;
        }

        // Update projectile position to hit location
        Location hitLoc = rayTrace.getHitPosition().toLocation(world);
        projectile.teleport(hitLoc);

        // Move slightly away from the impact point to prevent glitching
        double magnitude = velocity.length();
        Vector normalizedVelocity = velocity.normalize();
        Location adjustedLoc = hitLoc.clone().subtract(
                normalizedVelocity.getX() * 0.0500000007450581D,
                normalizedVelocity.getY() * 0.0500000007450581D,
                normalizedVelocity.getZ() * 0.0500000007450581D
        );

        projectile.teleport(adjustedLoc);

        return hitBlock;
    }

    protected boolean checkIdle() {
        if (projectile.isDead() || !projectile.isValid()) {
            return true;
        }
        Block check_block = projectile.getLocation().getBlock().getRelative(BlockFace.DOWN);
        if (projectile.getVelocity().length() < 0.5 && (projectile.isOnGround() || !BlocksUtil.isAirOrFoliage(check_block))) {
            return true;
        }
        return false;
    }

    public void setProjectileEntity(Entity projectile) {
        if (projectile == null) {
            return;
        }
        this.projectile = projectile;
        if (projectile instanceof Item item) {
            item.setPickupDelay(1000000);
        }
    }

    public Entity getProjectileEntity(){
        return projectile;
    }

    public void setDamage(double damage) {
        this.damage = damage;
    }

    public double getDamage() {
        return damage;
    }

    protected abstract Entity createProjectileEntity();

    // Called once when the projectile is fired
    protected abstract void doVelocity();

    // Visual effects that apply every tick
    protected abstract void doEffect();

    // Returns true to call destroy after
    protected abstract boolean onExpire();

    // Returns true to call destroy after
    protected abstract boolean onHitLivingEntity(LivingEntity hit);

    // Returns true to call destroy after
    protected abstract boolean onHitBlock(Block hit);

    // Returns true to call destroy after
    protected abstract boolean onIdle();

    public long getExpirationTicks() {
        return expiration_ticks;
    }

    public String getName() {
        return name;
    }

}

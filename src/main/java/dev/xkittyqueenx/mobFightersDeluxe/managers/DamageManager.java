package dev.xkittyqueenx.mobFightersDeluxe.managers;

import dev.xkittyqueenx.mobFightersDeluxe.MobFightersDeluxe;
import dev.xkittyqueenx.mobFightersDeluxe.events.SmashDamageEvent;
import dev.xkittyqueenx.mobFightersDeluxe.fighters.Fighter;
import dev.xkittyqueenx.mobFightersDeluxe.fighters.SpectatorFighter;
import dev.xkittyqueenx.mobFightersDeluxe.managers.gamemodes.SmashGamemode;
import dev.xkittyqueenx.mobFightersDeluxe.managers.smashserver.SmashServer;
import dev.xkittyqueenx.mobFightersDeluxe.utilities.DamageUtil;
import dev.xkittyqueenx.mobFightersDeluxe.utilities.Utils;
import dev.xkittyqueenx.mobFightersDeluxe.utilities.VelocityUtil;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.WeakHashMap;

public class DamageManager implements Listener {

    private final Plugin plugin = MobFightersDeluxe.getInstance();
    private static final List<SmashDamageEvent> damage_record = new ArrayList<>();
    private static final WeakHashMap<Player, BukkitRunnable> activeStuns = new WeakHashMap<>();

    public DamageManager() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    // Disable mob combustion in sunlight
    @EventHandler
    public void onCombust(EntityCombustEvent e) {
        if(e instanceof EntityCombustByBlockEvent || e instanceof EntityCombustByEntityEvent) {
            return;
        }
        e.setCancelled(true);
    }

    // Highest priority to get after all changes
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamage(EntityDamageEvent e) {
        if (e.isCancelled()) {
            return;
        }
        if(e.getEntity() instanceof Item) {
            e.setCancelled(true);
        }
        if (!(e.getEntity() instanceof LivingEntity damagee)) {
            return;
        }
        LivingEntity damager = null;
        Projectile projectile = null;
        boolean damage_delay = false;
        ChatColor reason_color = ChatColor.GREEN;
        double knockback = 1;
        if (e instanceof EntityDamageByEntityEvent damageBy) {
            if (damageBy.getDamager() instanceof Projectile) {
                projectile = (Projectile) damageBy.getDamager();
                if (projectile.getShooter() instanceof LivingEntity) {
                    damager = (LivingEntity) projectile.getShooter();
                }
            } else if (damageBy.getDamager() instanceof LivingEntity) {
                damager = (LivingEntity) damageBy.getDamager();
            }
        }
        double damage = e.getDamage();
        // Consistent Arrow Damage
        if (projectile instanceof Arrow) {
            damage = projectile.getVelocity().length() * 3;
            reason_color = ChatColor.YELLOW;
            damage_delay = true;
        }
        // Consistent Melee Damage
        if (damager instanceof Player && e.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
            Fighter fighter = FighterManager.getPlayerFighters().get(damager);
            if (fighter != null) {
                damage = fighter.getDamage();
            }
        }
        boolean display_as_last_damage = true;
        if (e.getCause() == EntityDamageEvent.DamageCause.POISON) {
            if (e.getEntity().hasMetadata("Poison Damager")) {
                List<MetadataValue> values = e.getEntity().getMetadata("Poison Damager");
                if (!values.isEmpty()) {
                    // Set damager so they get put on the damage rate
                    damager = (Player) values.getFirst().value();
                    knockback = 0;
                    display_as_last_damage = false;
                }
            }
        }
        if (e.getEntity() instanceof Mob || e.getEntity() instanceof Player) {
            SmashDamageEvent smashDamageEvent = new SmashDamageEvent(damagee, damager, damage);
            smashDamageEvent.multiplyKnockback(knockback);
            smashDamageEvent.setDamageCause(e.getCause());
            smashDamageEvent.setProjectile(projectile);
            smashDamageEvent.setDisplayAsLastDamage(display_as_last_damage);
            smashDamageEvent.setIgnoreDamageDelay(damage_delay);
            smashDamageEvent.setReasonColor(reason_color);
            smashDamageEvent.callEvent();
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void removeArrows(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Arrow arrow)) {
            return;
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (arrow.isValid()) {
                List<MetadataValue> data = arrow.getMetadata("Sticky Arrow");
                if (data.isEmpty()) {
                    arrow.remove();
                    return;
                }
            }
        }, 1L);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void cancelSmashDamage(SmashDamageEvent e) {
        if (!DamageUtil.canDamage(e.getDamagee(), e.getDamager())) {
            e.setCancelled(true);
            return;
        }
        if (e.getDamagee().getHealth() <= 0) {
            e.setCancelled(true);
            return;
        }
        if (e.getDamagee() != null && e.getDamagee() instanceof Mob damagee) {
            // Check if the damagee can be hit by the damager again
            if (!e.getIgnoreDamageDelay()) {
                if (!DamageUtil.getDamageRateTracker(damagee).canBeHurtBy(e.getDamager())) {
                    e.setCancelled(true);
                    return;
                }
            }
        }

        if (e.getDamager() != null && e.getDamager() instanceof Player damager) {
            if (damager.getGameMode() != GameMode.SURVIVAL && damager.getGameMode() != GameMode.ADVENTURE) {
                e.setCancelled(true);
                return;
            }
            // Check if the damager can hit the damagee again
            if (!e.getIgnoreDamageDelay())
                if (!DamageUtil.getDamageRateTracker(damager).canHurt(e.getDamagee())) {
                    e.setCancelled(true);
                    return;
                }
        }
    }

    @EventHandler
    public void borderKill(SmashDamageEvent e) {
        if (e.getDamageCause() != EntityDamageEvent.DamageCause.VOID) {
            return;
        }
        if (e.getIgnoreDamageDelay()) {
            return;
        }
        if (!(e.getDamagee() instanceof Player)) {
            return;
        }
        DamageUtil.borderKill((Player) e.getDamagee(), true);
        e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onSmashDamage(SmashDamageEvent e) {
        if (e.isCancelled()) {
            return;
        }
        LivingEntity damagee = e.getDamagee();
        LivingEntity damager = e.getDamager();
        double damage = e.getDamage();
        double knockbackMultiplier = e.getKnockbackMultiplier();
        boolean ignoreArmor = e.getIgnoreArmor();
        EntityDamageEvent.DamageCause cause = e.getDamageCause();
        Location origin = e.getKnockbackOrigin();
        String reason = e.getReason();
        Projectile projectile = e.getProjectile();
        if (damagee == null || damage <= 0) {
            e.setCancelled(true);
            return;
        }
        if (!DamageUtil.canDamage(damagee, damager)) {
            e.setCancelled(true);
            return;
        }
        double damageMultiplier = 1;
        double knockbackPercentage = 0.0;
        if (damagee instanceof Mob mob) {
            Player player = Utils.getPlayerFromMob(mob);
            if (player != null) {
                SmashServer smashServer = GameManager.getPlayerServer(player);
                if (smashServer != null) {
                    Fighter fighter = FighterManager.getPlayerFighters().get(player);
                    knockbackPercentage = fighter.getKnockbackPercentage();
                } else {
                    knockbackPercentage = 0.0;
                }
            }
        } else if (damagee instanceof Player player) {
            SmashServer smashServer = GameManager.getPlayerServer(player);
            if (smashServer != null) {
                Fighter fighter = FighterManager.getPlayerFighters().get(player);
                knockbackPercentage = fighter.getKnockbackPercentage();
            } else {
                knockbackPercentage = 0.0;
            }
        } else {
            knockbackPercentage = 0.0;
        }
        double absorption_health = damagee.getAbsorptionAmount();
        if (damagee instanceof Mob mob) {
            Player player = Utils.getPlayerFromMob(mob);
            if (player != null) {
                Fighter fighter = FighterManager.getPlayerFighters().get(player);
                if (fighter != null) {
                    damageMultiplier = Math.max(0, 1 - fighter.getArmor() * 0.07f);
                }
            }
        }
        if (damagee instanceof Player player) {
            Fighter fighter = FighterManager.getPlayerFighters().get(player);
            if (fighter != null) {
                damageMultiplier = Math.max(0, 1 - fighter.getArmor() * 0.07f);
            }
        }
        if (ignoreArmor) {
            damageMultiplier = 1;
        }
        double dealt;
        if ((float) damagee.getNoDamageTicks() > (float) damagee.getMaximumNoDamageTicks() / 2.0F) {
            dealt = Math.max(damage - damagee.getLastDamage(), 0) * damageMultiplier;
        } else {
            dealt = damage * damageMultiplier;
        }
        if (absorption_health > 0) {
            damagee.setAbsorptionAmount(Math.max(0, absorption_health - dealt));
            dealt -= absorption_health;
        }
        damagee.setLastDamage(damage);
        if (damagee instanceof Mob mob) {
            Player player = Utils.getPlayerFromMob(mob);
            if (player != null) {
                SmashServer server = GameManager.getPlayerServer(player);
                if (server != null) {
                    Fighter fighter = FighterManager.getPlayerFighters().get(player);
                    if (fighter != null) {
                        double percentage = fighter.getKnockbackPercentage();
                        fighter.setKnockbackPercentage(Math.min(percentage + Math.max(0.0, dealt), 999.90));
                    }
                }
                player.sendHealthUpdate();
            }
        } else if (damagee instanceof Player player) {
            SmashServer server = GameManager.getPlayerServer(player);
            if (server != null) {
                Fighter fighter = FighterManager.getPlayerFighters().get(player);
                if (fighter != null) {
                    double percentage = fighter.getKnockbackPercentage();
                    fighter.setKnockbackPercentage(Math.min(percentage + Math.max(0.0, dealt), 999.90));
                }
            }
            player.sendHealthUpdate();
        }
        damagee.playHurtAnimation(0);
        storeDamageEvent(e);
        if (damagee instanceof Player player) {
            if (e.getDamageCause() == EntityDamageEvent.DamageCause.VOID) {
                Objects.requireNonNull(GameManager.getPlayerServer(player)).death(player);
            }
        }
        if (damager instanceof Player player && cause == EntityDamageEvent.DamageCause.PROJECTILE && projectile instanceof Arrow) {
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 0.5f);
        }
        if (damager instanceof Player player && cause != EntityDamageEvent.DamageCause.VOID) {
            player.setLevel((int) damage);
        }
        if (knockbackMultiplier > 0 && (origin != null || damager != null || projectile != null)) {
            double knockback = Math.max(damage, 2);
            knockback = Math.log10(knockback * 0.8);
            knockback *= knockbackMultiplier;

            if (damagee instanceof Mob mob) {
                Player player = Utils.getPlayerFromMob(mob);
                Fighter fighter = FighterManager.getPlayerFighters().get(player);
                if (fighter != null) {
                    knockback *= fighter.getKnockback();
                }
                knockback *= (1 + 0.0125 * (knockbackPercentage - 0));
            } else if (damagee instanceof Player player) {
                Fighter fighter = FighterManager.getPlayerFighters().get(player);
                if (fighter != null) {
                    knockback *= fighter.getKnockback();
                }
                knockback *= (1 + 0.0125 * (knockbackPercentage - 0));
            }

            if (origin == null && damager != null) {
                origin = damager.getLocation();
            }
            Vector trajectory = null;
            if (origin != null) {
                trajectory = damagee.getLocation().toVector().subtract(origin.toVector()).setY(0).normalize();
                trajectory.multiply(0.6 * knockback);

                Vector originalDirection = damagee.getLocation().toVector().subtract(origin.toVector()).normalize();
                trajectory.setY(originalDirection.getY() * 0.6 * knockback);
            }

            if (projectile != null) {
                trajectory = projectile.getVelocity();
                trajectory.setY(0);
                trajectory.multiply(0.37 * knockback / trajectory.length());
                trajectory.setY(0.06);
            }
            double vel = 0.2 + trajectory.length() * 0.8;
            if (vel >= 2.5) {
                if (damagee instanceof Mob mob) {
                    Player player = Utils.getPlayerFromMob(mob);
                    if (player != null) {
                        final boolean allowFlight = player.getAllowFlight();
                        player.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, player.getLocation().add(0, 1, 0), (int) vel, 0.6f, 0.4f, 0.6f, 0, null, true);
                        Fighter kit = FighterManager.getPlayerFighters().get(player);
                        LivingEntity living;
                        if (kit.getKnockbackPercentage() >= 300.0) {
                            living = player.getWorld().spawn(player.getEyeLocation().clone().subtract(0, 0.5, 0), Vex.class);
                        } else {
                            living = player.getWorld().spawn(player.getEyeLocation().clone().subtract(0, 0.5, 0), Chicken.class);
                        }
                        for (Player hide : player.getWorld().getPlayers()) {
                            if (hide.equals(player)) {
                                continue;
                            }
                            hide.hideEntity(plugin, living);
                        }
                        living.setInvisible(true);
                        living.setSilent(true);
                        Objects.requireNonNull(living.getAttribute(Attribute.SCALE)).setBaseValue(0.1);
                        living.addPassenger(player);
                        if (living instanceof Chicken chicken) {
                            chicken.setAggressive(false);
                            chicken.setBaby();
                            chicken.setAware(false);
                        }
                        if (living instanceof Vex vex) {
                            vex.setAggressive(false);
                            vex.setCharging(false);
                            vex.setTarget(null);
                            vex.setAware(false);
                        }
                        for (Player show : player.getWorld().getPlayers()) {
                            if (show.equals(player)) {
                                continue;
                            }
                            show.showEntity(plugin, living);
                        }
                        final double movementSpeed = 0.1;
                        VelocityUtil.setVelocity(living, trajectory, vel, false,
                                0, Math.abs(0.2 * knockback), Math.min(0.4 + (0.04 * knockback), 0.6), true, 0.4);
                        kit.setRooted(true);
                        int duration = Math.min((int) (3 * vel), 50);
                        if (vel >= 4.5) {
                            damagee.getWorld().playSound(damagee.getLocation(), Sound.ITEM_MACE_SMASH_GROUND_HEAVY, 1.25f, 1f);
                        } else if (vel >= 3.5) {
                            damagee.getWorld().playSound(damagee.getLocation(), Sound.ITEM_MACE_SMASH_GROUND, 1f, 1f);
                        } else {
                            damagee.getWorld().playSound(damagee.getLocation(), Sound.ITEM_MACE_SMASH_AIR, 0.75f, 1f);
                        }
                        if (activeStuns.containsKey(player)) {
                            BukkitRunnable oldStun = activeStuns.get(player);
                            oldStun.cancel();
                            activeStuns.remove(player);
                            Objects.requireNonNull(player.getAttribute(Attribute.MOVEMENT_SPEED)).setBaseValue(movementSpeed);
                        }
                        Objects.requireNonNull(player.getAttribute(Attribute.MOVEMENT_SPEED)).setBaseValue(0);
                        BukkitRunnable runnable = new BukkitRunnable() {
                            int ticks = 0;
                            @Override
                            public void run() {
                                ticks++;
                                if (living instanceof Vex) {
                                    if (living.getLocation().clone().add(living.getVelocity().clone()).getBlock().isSolid() || living.getLocation().clone().subtract(living.getVelocity().clone()).getBlock().isSolid() || living.getLocation().clone().add(0, 1, 0).getBlock().isSolid() || living.getLocation().clone().subtract(living.getVelocity().clone().multiply(0.5)).getBlock().isSolid() || living.getLocation().clone().add(living.getVelocity().clone().multiply(0.5)).getBlock().isSolid() ||
                                            living.getLocation().clone().add(living.getVelocity().clone().multiply(1.5)).getBlock().isSolid() || living.getLocation().clone().subtract(living.getVelocity().clone().multiply(1.5)).getBlock().isSolid() || living.getLocation().clone().add(living.getLocation().getDirection()).getBlock().isSolid() || living.getLocation().clone().subtract(living.getLocation().getDirection()).getBlock().isSolid()) {
                                        return;
                                    }
                                }
                                Fighter kit = FighterManager.getPlayerFighters().get(player);
                                if (ticks >= duration || kit == null || kit instanceof SpectatorFighter) {
                                    activeStuns.remove(player);
                                    if (kit == null || kit instanceof SpectatorFighter) {
                                        living.removePassenger(player);
                                        living.remove();
                                        Objects.requireNonNull(player.getAttribute(Attribute.MOVEMENT_SPEED)).setBaseValue(movementSpeed);
                                        cancel();
                                        return;
                                    }

                                    Vector silverfishVelocity = living.getVelocity().clone();

                                    // Get the velocity magnitude to scale our compensation
                                    double velocityMagnitude = silverfishVelocity.length();

                                    // Create a normalized direction vector for the offset
                                    Vector directionVector = silverfishVelocity.clone().normalize();

                                    // Calculate an adaptive offset that scales with velocity
                                    // The scaling factor can be adjusted based on testing
                                    double offsetScale = 3 + (3.5 * velocityMagnitude);
                                    Vector offsetVector = directionVector.multiply(-offsetScale);

//                                    player.sendMessage("" + velocityMagnitude);
//                                    player.sendMessage("" + offsetScale);

                                    // Use silverfish location as base position
                                    Location targetLocation = living.getLocation().clone();

                                    // Apply our calculated offset
                                    targetLocation.add(offsetVector);

                                    // Preserve player's view direction
                                    targetLocation.setPitch(player.getLocation().getPitch());
                                    targetLocation.setYaw(player.getLocation().getYaw());
                                    if (offsetScale > 8) {
                                        targetLocation.subtract(new Vector(0, velocityMagnitude, 0));
                                    } else {
                                        targetLocation.subtract(new Vector(0, velocityMagnitude / 2, 0));
                                    }

                                    // Check if target location or block above is solid
                                    if (targetLocation.getBlock().isSolid() || targetLocation.clone().add(0, 1, 0).getBlock().isSolid()) {
                                        // Find a safe location nearby
                                        Location safeLocation = findSafeLocation(targetLocation);
                                        if (safeLocation != null) {
                                            targetLocation = safeLocation;
                                        } else {
                                            // If no safe location found, keep the player at their current location
                                            targetLocation = player.getLocation();
                                        }
                                    }

                                    // Remove the player from the silverfish
                                    living.removePassenger(player);

                                    player.teleport(targetLocation);

                                    // Then remove the silverfish
                                    living.remove();

                                    Fighter kit1 = FighterManager.getPlayerFighters().get(player);
                                    if (kit1 == null) {
                                        return;
                                    }
                                    Objects.requireNonNull(player.getAttribute(Attribute.MOVEMENT_SPEED)).setBaseValue(movementSpeed);
                                    player.setAllowFlight(allowFlight);
                                    Bukkit.getScheduler().runTaskLater(plugin, () -> player.setVelocity(silverfishVelocity), 0L);
                                    Utils.sendActionBarMessage(" ", player);
                                    kit1.setRooted(false);
                                    cancel();
                                    return;
                                }
                                if (living.getVelocity().lengthSquared() < 0.05 && ticks > 5) {
                                    Vector silverfishVelocity = living.getVelocity().clone();

                                    // Use the silverfish's location as the reference point
                                    Location entityLocation = living.getLocation().clone();

                                    // Preserve the player's looking direction
                                    entityLocation.setPitch(player.getLocation().getPitch());
                                    entityLocation.setYaw(player.getLocation().getYaw());

                                    // Check if target location or block above is solid
                                    if (entityLocation.getBlock().isSolid() || entityLocation.clone().add(0, 1, 0).getBlock().isSolid()) {
                                        // Find a safe location nearby
                                        Location safeLocation = findSafeLocation(entityLocation);
                                        if (safeLocation != null) {
                                            entityLocation = safeLocation;
                                        } else {
                                            // If no safe location found, keep the player at their current location
                                            entityLocation = player.getLocation();
                                        }
                                    }

                                    // Remove the player from the silverfish
                                    living.removePassenger(player);

                                    // Teleport to the entity's location
                                    player.teleport(entityLocation);

                                    // Then remove the silverfish
                                    living.remove();

                                    Fighter kit1 = FighterManager.getPlayerFighters().get(player);
                                    if (kit1 == null) {
                                        return;
                                    }
                                    Objects.requireNonNull(player.getAttribute(Attribute.MOVEMENT_SPEED)).setBaseValue(movementSpeed);
                                    player.setAllowFlight(allowFlight);
                                    Bukkit.getScheduler().runTaskLater(plugin, () -> player.setVelocity(silverfishVelocity), 0L);
                                    Utils.sendActionBarMessage(" ", player);
                                    kit1.setRooted(false);
                                    cancel();
                                    return;
                                }
                                Utils.sendActionBarMessage("<red><b>☠ STUNNED ☠", player);
                                living.addPassenger(player);
                            }
                        };
                        activeStuns.put(player, runnable);
                        runnable.runTaskTimer(plugin, 0L, 0L);
                    }
                } else if (damagee instanceof Player player) {
                    final boolean allowFlight = player.getAllowFlight();
                    player.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, player.getLocation().add(0, 1, 0), (int) vel, 0.6f, 0.4f, 0.6f, 0, null, true);
                    Fighter kit = FighterManager.getPlayerFighters().get(player);
                    LivingEntity living;
                    if (kit.getKnockbackPercentage() >= 300.0) {
                        living = player.getWorld().spawn(player.getEyeLocation().clone().subtract(0, 0.5, 0), Vex.class);
                    } else {
                        living = player.getWorld().spawn(player.getEyeLocation().clone().subtract(0, 0.5, 0), Chicken.class);
                    }
                    for (Player hide : player.getWorld().getPlayers()) {
                        if (hide.equals(player)) {
                            continue;
                        }
                        hide.hideEntity(plugin, living);
                    }
                    living.setInvisible(true);
                    living.setSilent(true);
                    Objects.requireNonNull(living.getAttribute(Attribute.SCALE)).setBaseValue(0.1);
                    living.addPassenger(player);
                    if (living instanceof Chicken chicken) {
                        chicken.setAggressive(false);
                        chicken.setBaby();
                        chicken.setAware(false);
                    }
                    if (living instanceof Vex vex) {
                        vex.setAggressive(false);
                        vex.setCharging(false);
                        vex.setTarget(null);
                        vex.setAware(false);
                    }
                    for (Player show : player.getWorld().getPlayers()) {
                        if (show.equals(player)) {
                            continue;
                        }
                        show.showEntity(plugin, living);
                    }
                    final double movementSpeed = 0.1;
                    VelocityUtil.setVelocity(living, trajectory, vel, false,
                            0, Math.abs(0.2 * knockback), Math.min(0.4 + (0.04 * knockback), 0.6), true, 0.4);
                    kit.setRooted(true);
                    int duration = Math.min((int) (3 * vel), 50);
                    if (vel >= 4.5) {
                        damagee.getWorld().playSound(damagee.getLocation(), Sound.ITEM_MACE_SMASH_GROUND_HEAVY, 1.25f, 1f);
                    } else if (vel >= 3.5) {
                        damagee.getWorld().playSound(damagee.getLocation(), Sound.ITEM_MACE_SMASH_GROUND, 1f, 1f);
                    } else {
                        damagee.getWorld().playSound(damagee.getLocation(), Sound.ITEM_MACE_SMASH_AIR, 0.75f, 1f);
                    }
                    if (activeStuns.containsKey(player)) {
                        BukkitRunnable oldStun = activeStuns.get(player);
                        oldStun.cancel();
                        activeStuns.remove(player);
                        Objects.requireNonNull(player.getAttribute(Attribute.MOVEMENT_SPEED)).setBaseValue(movementSpeed);
                    }
                    Objects.requireNonNull(player.getAttribute(Attribute.MOVEMENT_SPEED)).setBaseValue(0);
                    BukkitRunnable runnable = new BukkitRunnable() {
                        int ticks = 0;
                        @Override
                        public void run() {
                            ticks++;
                            if (living instanceof Vex) {
                                if (living.getLocation().clone().add(living.getVelocity().clone()).getBlock().isSolid() || living.getLocation().clone().subtract(living.getVelocity().clone()).getBlock().isSolid() || living.getLocation().clone().add(0, 1, 0).getBlock().isSolid() || living.getLocation().clone().subtract(living.getVelocity().clone().multiply(0.5)).getBlock().isSolid() || living.getLocation().clone().add(living.getVelocity().clone().multiply(0.5)).getBlock().isSolid() ||
                                        living.getLocation().clone().add(living.getVelocity().clone().multiply(1.5)).getBlock().isSolid() || living.getLocation().clone().subtract(living.getVelocity().clone().multiply(1.5)).getBlock().isSolid() || living.getLocation().clone().add(living.getLocation().getDirection()).getBlock().isSolid() || living.getLocation().clone().subtract(living.getLocation().getDirection()).getBlock().isSolid()) {
                                    return;
                                }
                            }
                            Fighter kit = FighterManager.getPlayerFighters().get(player);
                            if (ticks >= duration || kit == null || kit instanceof SpectatorFighter) {
                                activeStuns.remove(player);
                                if (kit == null || kit instanceof SpectatorFighter) {
                                    living.removePassenger(player);
                                    living.remove();
                                    Objects.requireNonNull(player.getAttribute(Attribute.MOVEMENT_SPEED)).setBaseValue(movementSpeed);
                                    cancel();
                                    return;
                                }

                                Vector silverfishVelocity = living.getVelocity().clone();

                                // Get the velocity magnitude to scale our compensation
                                double velocityMagnitude = silverfishVelocity.length();

                                // Create a normalized direction vector for the offset
                                Vector directionVector = silverfishVelocity.clone().normalize();

                                // Calculate an adaptive offset that scales with velocity
                                // The scaling factor can be adjusted based on testing
                                double offsetScale = 3 + (3.5 * velocityMagnitude);
                                Vector offsetVector = directionVector.multiply(-offsetScale);

//                                    player.sendMessage("" + velocityMagnitude);
//                                    player.sendMessage("" + offsetScale);

                                // Use silverfish location as base position
                                Location targetLocation = living.getLocation().clone();

                                // Apply our calculated offset
                                targetLocation.add(offsetVector);

                                // Preserve player's view direction
                                targetLocation.setPitch(player.getLocation().getPitch());
                                targetLocation.setYaw(player.getLocation().getYaw());
                                if (offsetScale > 8) {
                                    targetLocation.subtract(new Vector(0, velocityMagnitude, 0));
                                } else {
                                    targetLocation.subtract(new Vector(0, velocityMagnitude / 2, 0));
                                }

                                // Check if target location or block above is solid
                                if (targetLocation.getBlock().isSolid() || targetLocation.clone().add(0, 1, 0).getBlock().isSolid()) {
                                    // Find a safe location nearby
                                    Location safeLocation = findSafeLocation(targetLocation);
                                    if (safeLocation != null) {
                                        targetLocation = safeLocation;
                                    } else {
                                        // If no safe location found, keep the player at their current location
                                        targetLocation = player.getLocation();
                                    }
                                }

                                // Remove the player from the silverfish
                                living.removePassenger(player);

                                player.teleport(targetLocation);

                                // Then remove the silverfish
                                living.remove();

                                Fighter kit1 = FighterManager.getPlayerFighters().get(player);
                                if (kit1 == null) {
                                    return;
                                }
                                Objects.requireNonNull(player.getAttribute(Attribute.MOVEMENT_SPEED)).setBaseValue(movementSpeed);
                                player.setAllowFlight(allowFlight);
                                Bukkit.getScheduler().runTaskLater(plugin, () -> player.setVelocity(silverfishVelocity), 0L);
                                Utils.sendActionBarMessage(" ", player);
                                kit1.setRooted(false);
                                cancel();
                                return;
                            }
                            if (living.getVelocity().lengthSquared() < 0.05 && ticks > 5) {
                                Vector silverfishVelocity = living.getVelocity().clone();

                                // Use the silverfish's location as the reference point
                                Location entityLocation = living.getLocation().clone();

                                // Preserve the player's looking direction
                                entityLocation.setPitch(player.getLocation().getPitch());
                                entityLocation.setYaw(player.getLocation().getYaw());

                                // Check if target location or block above is solid
                                if (entityLocation.getBlock().isSolid() || entityLocation.clone().add(0, 1, 0).getBlock().isSolid()) {
                                    // Find a safe location nearby
                                    Location safeLocation = findSafeLocation(entityLocation);
                                    if (safeLocation != null) {
                                        entityLocation = safeLocation;
                                    } else {
                                        // If no safe location found, keep the player at their current location
                                        entityLocation = player.getLocation();
                                    }
                                }

                                // Remove the player from the silverfish
                                living.removePassenger(player);

                                // Teleport to the entity's location
                                player.teleport(entityLocation);

                                // Then remove the silverfish
                                living.remove();

                                Fighter kit1 = FighterManager.getPlayerFighters().get(player);
                                if (kit1 == null) {
                                    return;
                                }
                                Objects.requireNonNull(player.getAttribute(Attribute.MOVEMENT_SPEED)).setBaseValue(movementSpeed);
                                player.setAllowFlight(allowFlight);
                                Bukkit.getScheduler().runTaskLater(plugin, () -> player.setVelocity(silverfishVelocity), 0L);
                                Utils.sendActionBarMessage(" ", player);
                                kit1.setRooted(false);
                                cancel();
                                return;
                            }
                            Utils.sendActionBarMessage("<red><b>☠ STUNNED ☠", player);
                            living.addPassenger(player);
                        }
                    };
                    activeStuns.put(player, runnable);
                    runnable.runTaskTimer(plugin, 0L, 0L);
                }
            } else {
                if (damagee instanceof Mob mob) {
                    Player player = Utils.getPlayerFromMob(mob);
                    if (player != null) {
                        VelocityUtil.setVelocity(player, trajectory, vel, false,
                                0, Math.abs(0.2 * knockback), Math.min(0.4 + (0.04 * knockback), 0.6), true, 0.5);
                    }
                } else if (damagee instanceof Player player) {
                    VelocityUtil.setVelocity(player, trajectory, vel, false,
                            0, Math.abs(0.2 * knockback), Math.min(0.4 + (0.04 * knockback), 0.6), true, 0.5);
                }
            }
        }
    }

    private Location findSafeLocation(Location origin) {
        // Check in a small radius around the original location
        for (int x = -2; x <= 2; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -2; z <= 2; z++) {
                    Location check = origin.clone().add(x, y, z);
                    // A location is safe if both the block at that location and the block above are not solid
                    if (!check.getBlock().isSolid() && !check.clone().add(0, 1, 0).getBlock().isSolid()) {
                        return check;
                    }
                }
            }
        }
        return null;
    }

    public static void storeDamageEvent(SmashDamageEvent e) {
        // Add size limit to prevent memory leaks
        if (damage_record.size() > 1000) {
            damage_record.removeFirst();
        }
        damage_record.add(e);
    }

    public static void deathReport(Player player, boolean remove) {
        int count = 1;
        List<SmashDamageEvent> to_remove = new ArrayList<SmashDamageEvent>();
        for (int i = damage_record.size() - 1; i >= 0; i--) {
            SmashDamageEvent e = damage_record.get(i);
            // Expunge old records
            if ((System.currentTimeMillis() - e.getDamageTimeMs()) > 15000) {
                to_remove.add(e);
                continue;
            }
            if (!e.getDamageeName().equals(player.getName())) {
                continue;
            }
            String time_since = String.format("%.1f", (System.currentTimeMillis() - e.getDamageTimeMs()) / 1000.0);
            String damage_amount = "Infinite";
            if (e.getDamage() < 1000) {
                damage_amount = String.format("%.1f", e.getDamage());
            }
            player.sendMessage(MiniMessage.miniMessage().deserialize("<#32CD32>#" + count + ": <#FAA0A0>" + e.getDamagerName() +
                    " <gray>[<yellow>" + damage_amount + "<gray>] [<gold>" + e.getReason() + "<gray>] [<green>" + time_since + " Seconds Prior<gray>]"));
            count++;
            to_remove.add(e);
        }
        if(remove) {
            damage_record.removeAll(to_remove);
        }
    }

    public static SmashDamageEvent getLastDamageEvent(Player player) {
        long last_time = 0;
        SmashDamageEvent record = null;
        // Check for ones with a damaging entity first
        for (SmashDamageEvent check : damage_record) {
            if (check.getDamager() == null) {
                continue;
            }
            if (!(check.getDamager() instanceof Player)) {
                continue;
            }
            if (!check.getDamageeName().equals(player.getName())) {
                continue;
            }
            if (!check.getDisplayAsLastDamage()) {
                continue;
            }
            // Ignore old records
            if ((System.currentTimeMillis() - check.getDamageTimeMs()) > 15000) {
                continue;
            }
            if (check.getDamageTimeMs() <= last_time) {
                continue;
            }
            last_time = check.getDamageTimeMs();
            record = check;
        }
        if(record != null) {
            return record;
        }
        // Now check all records if we didn't find one
        for (SmashDamageEvent check : damage_record) {
            if (!check.getDamageeName().equals(player.getName())) {
                continue;
            }
            if (check.getDamageTimeMs() <= last_time) {
                continue;
            }
            // Ignore old records
            if ((System.currentTimeMillis() - check.getDamageTimeMs()) > 15000) {
                continue;
            }
            last_time = check.getDamageTimeMs();
            record = check;
        }
        return record;
    }

}

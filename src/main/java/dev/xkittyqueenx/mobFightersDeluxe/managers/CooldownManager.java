package dev.xkittyqueenx.mobFightersDeluxe.managers;

import dev.xkittyqueenx.mobFightersDeluxe.MobFightersDeluxe;
import dev.xkittyqueenx.mobFightersDeluxe.abilities.Ability;
import dev.xkittyqueenx.mobFightersDeluxe.attributes.Attribute;
import dev.xkittyqueenx.mobFightersDeluxe.events.RechargeAttributeEvent;
import dev.xkittyqueenx.mobFightersDeluxe.fighters.Fighter;
import dev.xkittyqueenx.mobFightersDeluxe.managers.smashserver.SmashServer;
import dev.xkittyqueenx.mobFightersDeluxe.utilities.Utils;
import net.kyori.adventure.key.Key;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Iterator;

public class CooldownManager extends BukkitRunnable {

    private ArrayList<CooldownData> cooldownData = new ArrayList<>();
    private boolean isRunning = false;
    private Plugin plugin = MobFightersDeluxe.getInstance();

    public CooldownManager() {
        this.runTaskTimer(plugin, 0, 1);
        isRunning = true;
    }

    @Override
    public void run() {
        for (Iterator<CooldownData> cdDataIterator = cooldownData.iterator(); cdDataIterator.hasNext(); ) {
            CooldownData currData = cdDataIterator.next();

            Ability using = FighterManager.getCurrentAbility(currData.getAbilityUser());

            if (using != null && using.equals(currData.getAttribute())) {
                displayCooldownTo(currData.getAbilityUser(), currData);
            }

            // The kit has probably been unequipped if the owner is null
            if(currData.getAttribute() == null || currData.getAttribute().getOwner() == null) {
                cdDataIterator.remove();
                return;
            }

            if (currData.getRemainingTimeMs() <= 0) {
                cdDataIterator.remove();

                RechargeAttributeEvent event = new RechargeAttributeEvent(currData.getAbilityUser(), currData.getAttribute());
                event.callEvent();
                Utils.sendActionBarMessage("<green><b>" + currData.getAttribute().name + " Recharged", currData.getAbilityUser());
                Ability ability = FighterManager.getCurrentAbility(currData.getAbilityUser());
                if (ability != null && ability.equals(currData.getAttribute())) {
                    currData.getAbilityUser().playSound(currData.getAbilityUser().getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 24.0f);
                }
            }
        }
    }

    public long getRemainingTimeFor(Attribute attribute, Player abilityUser) {
        for (CooldownData cd : cooldownData) {
            if (cd.getAttribute().equals(attribute) && cd.abilityUser.equals(abilityUser)) {
                return cd.getRemainingTimeMs();
            }
        }

        return 0;
    }

    public long getTimeElapsedFor(Attribute attribute, Player abilityUser) {
        for (CooldownData cd : cooldownData) {
            if (cd.getAttribute().equals(attribute) && cd.abilityUser.equals(abilityUser)) {
                return cd.getTimeElapsedMs();
            }
        }

        return 0;
    }

    public void addCooldown(Attribute attribute, long duration, Player abilityUser) {
        Fighter kit = FighterManager.getPlayerFighters().get(abilityUser);
        boolean replaced = false;
        for(CooldownData cd : cooldownData) {
            if(cd.getAttribute().equals(attribute)) {
                replaced = true;
                break;
            }
        }
        if(!replaced && duration == 0) {
            return;
        }
        cooldownData.removeIf(cd -> cd.getAttribute().equals(attribute));
        Ability ability = kit.getAbilityInSlot(0);
        if (ability != null && ability.equals(attribute)) {
            ItemStack itemStack = abilityUser.getInventory().getItem(0);
            if (itemStack != null) {
                abilityUser.setCooldown(Key.key("0"), (int) (20 * Utils.msToSeconds(duration)));
            }
        }
        Ability ability2 = kit.getAbilityInSlot(1);
        if (ability2 != null && ability2.equals(attribute)) {
            ItemStack itemStack = abilityUser.getInventory().getItem(1);
            if (itemStack != null) {
                abilityUser.setCooldown(Key.key("1"), (int) (20 * Utils.msToSeconds(duration)));
            }
        }
        Ability ability3 = kit.getAbilityInSlot(2);
        if (ability3 != null && ability3.equals(attribute)) {
            ItemStack itemStack = abilityUser.getInventory().getItem(2);
            if (itemStack != null) {
                abilityUser.setCooldown(Key.key("2"), (int) (20 * Utils.msToSeconds(duration)));
            }
        }
        cooldownData.add(new CooldownData(attribute, duration, abilityUser));
    }

    public void displayCooldownTo(Player player, Attribute attribute) {
        CooldownData found = null;
        for(CooldownData data : cooldownData) {
            if(data.getAttribute().equals(attribute)) {
                found = data;
                break;
            }
        }
        if(found == null) {
            return;
        }
        displayCooldownTo(player, found);
    }

    private void displayCooldownTo(Player player, CooldownData cd) {
        int barLength = 24;
        int startRedBarInterval = barLength - (int) ((cd.getRemainingTimeMs() / (double) cd.duration) * barLength); // Val between 0 - 1
        StringBuilder sb = new StringBuilder("      <white><b>" + cd.getAttribute().name + " ");
        for (int i = 0; i < barLength; i++) {
            if (i < startRedBarInterval) {
                sb.append("<green>▌");
            } else {
                sb.append("<red>▌");
            }
        }

        sb.append(" <white>" + Utils.msToSeconds(cd.getRemainingTimeMs()) + " Seconds");
        Utils.sendActionBarMessage(sb.toString(), player);
    }

    private class CooldownData {
        private Player abilityUser;
        private Attribute attribute;
        private long duration;
        private long startTime;

        CooldownData(Attribute attribute, long duration, Player abilityUser) {
            this.attribute = attribute;
            this.duration = duration;
            startTime = System.currentTimeMillis();
            this.abilityUser = abilityUser;
        }

        long getRemainingTimeMs() {
            return startTime + duration - System.currentTimeMillis();
        }

        long getTimeElapsedMs() {
            return System.currentTimeMillis() - startTime;
        }

        long getStartTime() {
            return startTime;
        }

        long getDuration() { return duration; }

        Player getAbilityUser() {
            return abilityUser;
        }

        Attribute getAttribute() {
            return attribute;
        }
    }

}

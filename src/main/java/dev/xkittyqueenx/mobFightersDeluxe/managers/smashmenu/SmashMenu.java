package dev.xkittyqueenx.mobFightersDeluxe.managers.smashmenu;

import org.bukkit.inventory.Inventory;

import java.util.HashMap;

public class SmashMenu {

    private HashMap<Integer, MenuRunnable> actions = new HashMap<>();
    private Inventory inventory;

    public SmashMenu(Inventory inventory) {
        this.inventory = inventory;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public void setActionFromSlot(int slot, MenuRunnable runnable) {
        actions.put(slot, runnable);
    }

    public MenuRunnable getActionFromSlot(int slot) {
        return actions.get(slot);
    }

}

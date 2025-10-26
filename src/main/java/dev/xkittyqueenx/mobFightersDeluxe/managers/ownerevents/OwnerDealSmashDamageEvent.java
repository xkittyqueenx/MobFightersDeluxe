package dev.xkittyqueenx.mobFightersDeluxe.managers.ownerevents;

import dev.xkittyqueenx.mobFightersDeluxe.events.SmashDamageEvent;

import java.lang.reflect.InvocationTargetException;

public interface OwnerDealSmashDamageEvent {

    public abstract void onOwnerDealSmashDamageEvent(SmashDamageEvent e) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException;

}

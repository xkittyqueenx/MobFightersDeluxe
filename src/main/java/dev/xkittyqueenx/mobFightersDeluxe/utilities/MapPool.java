package dev.xkittyqueenx.mobFightersDeluxe.utilities;

public enum MapPool {

    SSM("Smash"),
    ORIGINAL("Experimental"),
    CAPTURE("Capture"),
    CONTROL("Control"),
    ESCORT("Escort"),
    TDM("TDM"),
    RETRO("Retro");

    private String map_pool;

    MapPool(String map_pool) {
        this.map_pool = map_pool;
    }

    public String getMapPool() {
        return map_pool;
    }

}

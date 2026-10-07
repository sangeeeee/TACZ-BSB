package com.sange.tacz_bsb.ammo;

/** Shared presentation values with no Minecraft or loader dependencies. */
public final class AmmoTier {
    private AmmoTier() {}
    public static int color(int tier) { return tier == 3 ? 0xAA55FF : tier == 2 ? 0xFF8800 : 0xDDDDDD; }
    public static String key(int tier) { return tier == 3 ? "precise" : "improved"; }
}

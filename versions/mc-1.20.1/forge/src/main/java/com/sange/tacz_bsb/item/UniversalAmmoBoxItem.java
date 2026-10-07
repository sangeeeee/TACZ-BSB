package com.sange.tacz_bsb.item;

import com.tacz.guns.item.AmmoBoxItem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Fixed-tier, unlimited ammunition for any caliber, including standard TaCZ gun packs. */
public final class UniversalAmmoBoxItem extends AmmoBoxItem {
    private final int tier;
    public UniversalAmmoBoxItem(int tier) {
        if (tier != 2 && tier != 3) throw new IllegalArgumentException("Invalid universal box tier");
        this.tier = tier;
    }
    @Override public int getAmmoCount(ItemStack stack) { return Integer.MAX_VALUE; }
    @Override public net.minecraft.resources.ResourceLocation getAmmoId(ItemStack stack) {
        return com.tacz.guns.api.DefaultAssets.EMPTY_AMMO_ID;
    }
    public int tier() { return tier; }
    @Override public boolean isAllTypeCreative(ItemStack stack) { return true; }
    @Override public boolean isCreative(ItemStack stack) { return false; }
    @Override public Component getName(ItemStack stack) {
        return Component.translatable(getDescriptionId()).withStyle(style -> style.withColor(TieredAmmoItem.color(tier)));
    }
}

package com.sange.tacz_bsb.item;

import net.minecraft.world.item.Item;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Uses the original model with Minecraft's standard item glint for every display context. */
public final class GlintMaterialItem extends Item {
    public GlintMaterialItem() { super(new Properties()); }
    @Override public boolean isFoil(ItemStack stack) { return true; }
    @Override public Component getName(ItemStack stack) {
        return super.getName(stack).copy().withStyle(style -> style.withColor(TieredAmmoItem.color(3)));
    }
}

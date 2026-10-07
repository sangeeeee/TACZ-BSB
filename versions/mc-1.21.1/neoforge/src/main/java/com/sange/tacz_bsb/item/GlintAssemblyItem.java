package com.sange.tacz_bsb.item;

import com.simibubi.create.content.processing.sequenced.SequencedAssemblyItem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Only work-in-progress items use Create's non-stackable progress item. */
public final class GlintAssemblyItem extends SequencedAssemblyItem {
    public GlintAssemblyItem() { super(new Properties()); }
    @Override public boolean isFoil(ItemStack stack) { return true; }
    @Override public Component getName(ItemStack stack) {
        return super.getName(stack).copy().withStyle(style -> style.withColor(TieredAmmoItem.color(3)));
    }
}

package com.sange.tacz_bsb.client;

import com.sange.tacz_bsb.item.GlintAssemblyItem;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

public final class AssemblyVisibility {
    public static boolean hidden(ItemStack stack) {
        var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return stack.getItem() instanceof GlintAssemblyItem
                || (id.getNamespace().equals("tacz_c") && id.getPath().startsWith("unfinished_"))
                || (stack.hasTag() && stack.getTag().contains("SequencedAssembly"));
    }
}

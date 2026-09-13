package com.sange.tacz_bsb.item;

import com.sange.tacz_bsb.BsbConfig;
import com.tacz.guns.item.AmmoItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import java.util.List;
import java.util.Locale;

public final class PreciseAmmoItem extends AmmoItem {
    @Override public boolean isFoil(ItemStack stack) { return true; }
    @Override @OnlyIn(Dist.CLIENT)
    public Component getName(ItemStack stack) {
        return Component.translatable("item.tacz_bsb.precise_ammo", super.getName(stack));
    }
    @Override @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> text, TooltipFlag flags) {
        String id = getAmmoId(stack).toString();
        text.add(Component.translatable("tooltip.tacz_bsb.damage",
                String.format(Locale.ROOT, "%.2f", BsbConfig.multiplier(id, false)),
                String.format(Locale.ROOT, "%.2f", BsbConfig.multiplier(id, true))).withStyle(ChatFormatting.GOLD));
        text.add(Component.translatable("tooltip.tacz_bsb.production").withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, context, text, flags);
    }
}


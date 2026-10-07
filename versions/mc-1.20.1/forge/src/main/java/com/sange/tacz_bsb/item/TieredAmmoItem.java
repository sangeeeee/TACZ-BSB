package com.sange.tacz_bsb.item;

import com.sange.tacz_bsb.BsbConfig;
import com.tacz.guns.item.AmmoItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import java.util.List;
import java.util.Locale;

public final class TieredAmmoItem extends AmmoItem {
    private final int tier;
    public TieredAmmoItem(int tier) {
        if (tier != 2 && tier != 3) throw new IllegalArgumentException("Invalid enhanced ammo tier");
        this.tier = tier;
    }
    public int tier() { return tier; }
    public static int color(int tier) { return com.sange.tacz_bsb.ammo.AmmoTier.color(tier); }
    public static String tierKey(int tier) { return com.sange.tacz_bsb.ammo.AmmoTier.key(tier); }
    @Override public boolean isFoil(ItemStack stack) { return true; }
    @Override @OnlyIn(Dist.CLIENT)
    public Component getName(ItemStack stack) {
        // Strip embedded TaCZ colors so the name uses the ammunition tier color.
        String baseName = ChatFormatting.stripFormatting(super.getName(stack).getString());
        return Component.translatable("item.tacz_bsb." + tierKey(tier) + "_ammo", baseName)
                .withStyle(style -> style.withColor(color(tier)));
    }
    @Override @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, net.minecraft.world.level.Level context, List<Component> text, TooltipFlag flags) {
        String id = getAmmoId(stack).toString();
        text.add(Component.translatable("tooltip.tacz_bsb." + tierKey(tier)).withStyle(style -> style.withColor(color(tier))));
        text.add(Component.translatable("tooltip.tacz_bsb.damage",
                String.format(Locale.ROOT, "%.2f", BsbConfig.multiplier(id, tier, false)),
                String.format(Locale.ROOT, "%.2f", BsbConfig.multiplier(id, tier, true))).withStyle(style -> style.withColor(color(tier))));
        text.add(Component.translatable("tooltip.tacz_bsb.production").withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, context, text, flags);
    }
}

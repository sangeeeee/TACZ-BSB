package com.sange.tacz_bsb.client;

import com.sange.tacz_bsb.BsbConfig;
import com.sange.tacz_bsb.ammo.AmmoState;
import com.sange.tacz_bsb.ammo.AmmoTransactions;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.init.ModCreativeTabs;
import com.tacz.guns.config.client.RenderConfig;
import com.tacz.guns.resource.pojo.data.gun.Bolt;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import com.sange.tacz_bsb.item.PreciseAmmoItem;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@EventBusSubscriber(modid = "tacz_bsb", value = Dist.CLIENT)
public final class BsbClient {
    @SubscribeEvent
    public static void hud(RenderGuiLayerEvent.Post event) {
        if (!event.getName().toString().equals("tacz:tac_gun_hud_overlay")) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || !RenderConfig.GUN_HUD_ENABLE.get()) return;
        var stack = mc.player.getMainHandItem();
        if (!(stack.getItem() instanceof IGun gun)) return;
        var index = TimelessAPI.getClientGunIndex(gun.getGunId(stack));
        if (index.isEmpty() || TimelessAPI.getGunDisplay(stack).isEmpty()) return;
        var state = AmmoState.display(stack);
        Bolt bolt = index.get().getGunData().getBolt();
        int type = state == null ? (bolt != Bolt.OPEN_BOLT && gun.hasBulletInBarrel(stack) ? 1 : 0)
                : state.nextRound(bolt == Bolt.OPEN_BOLT, bolt == Bolt.MANUAL_ACTION);
        String key;
        if (type == 0 && bolt == Bolt.MANUAL_ACTION
                && gun.getCurrentAmmoCount(stack) > 0) key = "hud.tacz_bsb.unchambered";
        else {
            if (type == 0 && gun.useInventoryAmmo(stack)) {
                for (int slot = 0; slot < mc.player.getInventory().getContainerSize(); slot++) {
                    var ammo = mc.player.getInventory().getItem(slot);
                    if (ammo.getItem() instanceof com.tacz.guns.api.item.IAmmo a && a.isAmmoOfGun(stack, ammo)) {
                        type = AmmoTransactions.precise(ammo) ? 2 : 1; break;
                    }
                    if (ammo.getItem() instanceof com.tacz.guns.api.item.IAmmoBox box
                            && box.isAmmoBoxOfGun(stack, ammo) && box.getAmmoCount(ammo) > 0) {
                        type = AmmoTransactions.preciseBox(ammo) ? 2 : 1; break;
                    }
                }
            } else if (type == 0 && state == null && gun.getCurrentAmmoCount(stack) > 0) type = 1;
            key = type == 2 ? "hud.tacz_bsb.precise" : type == 1 ? "hud.tacz_bsb.normal" : "hud.tacz_bsb.empty";
        }
        var graphics = event.getGuiGraphics();
        Component text = Component.translatable(key);
        graphics.drawString(mc.font, text,
                graphics.guiWidth() - 20 - mc.font.width(text) + BsbConfig.value(BsbConfig.HUD_X),
                graphics.guiHeight() - 58 + BsbConfig.value(BsbConfig.HUD_Y),
                type == 2 ? PreciseAmmoItem.NAME_COLOR : 0xDDDDDD, true);
    }
    @SubscribeEvent
    public static void boxTooltip(ItemTooltipEvent event) {
        if (AmmoTransactions.preciseBox(event.getItemStack())) {
            event.getToolTip().add(Component.translatable("tooltip.tacz_bsb.box").withStyle(style -> style.withColor(PreciseAmmoItem.NAME_COLOR)));
        }
    }
    @EventBusSubscriber(modid = "tacz_bsb", value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static final class ModEvents {
        @SubscribeEvent
        public static void creative(BuildCreativeModeTabContentsEvent event) {
            if (!event.getTabKey().equals(ModCreativeTabs.AMMO_TAB.getKey())) return;
            TimelessAPI.getAllCommonAmmoIndex().stream()
                    .sorted(java.util.Comparator.comparingInt(e -> e.getValue().getSort()))
                    .filter(e -> BsbConfig.enabled(e.getKey().toString()))
                    .forEach(e -> event.accept(AmmoTransactions.stack(e.getKey(), true, 1)));
        }
    }
}


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
import com.sange.tacz_bsb.item.TieredAmmoItem;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

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
                        type = AmmoTransactions.tier(ammo); break;
                    }
                    if (ammo.getItem() instanceof com.tacz.guns.api.item.IAmmoBox box
                            && box.isAmmoBoxOfGun(stack, ammo) && box.getAmmoCount(ammo) > 0) {
                        type = AmmoTransactions.boxTier(ammo); break;
                    }
                }
            } else if (type == 0 && state == null && gun.getCurrentAmmoCount(stack) > 0) type = 1;
            key = type > 1 ? "hud.tacz_bsb." + TieredAmmoItem.tierKey(type) : type == 1 ? "hud.tacz_bsb.normal" : "hud.tacz_bsb.empty";
        }
        var graphics = event.getGuiGraphics();
        Component text = Component.translatable(key);
        graphics.drawString(mc.font, text,
                graphics.guiWidth() - 20 - mc.font.width(text) + BsbConfig.value(BsbConfig.HUD_X),
                graphics.guiHeight() - 58 + BsbConfig.value(BsbConfig.HUD_Y),
                TieredAmmoItem.color(type), true);
    }
    @EventBusSubscriber(modid = "tacz_bsb", value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static final class ModEvents {
        @SubscribeEvent
        public static void colors(net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Item event) {
            event.register(com.tacz.guns.item.AmmoBoxItem::getColor,
                    com.sange.tacz_bsb.BsbContent.IMPROVED_UNIVERSAL_AMMO_BOX.get(),
                    com.sange.tacz_bsb.BsbContent.PRECISE_UNIVERSAL_AMMO_BOX.get());
        }
        @SubscribeEvent
        public static void creative(BuildCreativeModeTabContentsEvent event) {
            if (event.getTabKey().equals(ModCreativeTabs.OTHER_TAB.getKey())) {
                event.accept(com.sange.tacz_bsb.BsbContent.IMPROVED_UNIVERSAL_AMMO_BOX);
                event.accept(com.sange.tacz_bsb.BsbContent.PRECISE_UNIVERSAL_AMMO_BOX);
            }
            if (event.getTabKey().location().toString().equals("tacz_c:timeless_and_classics_zero_creatified")) {
                com.sange.tacz_bsb.BsbMaterials.ITEMS.values().stream()
                        .filter(item -> !AssemblyVisibility.hidden(item.get().getDefaultInstance())).forEach(event::accept);
            }
            java.util.stream.Stream.concat(event.getParentEntries().stream(), event.getSearchEntries().stream())
                    .filter(AssemblyVisibility::hidden).toList()
                    .forEach(stack -> event.remove(stack, net.minecraft.world.item.CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS));
            if (!event.getTabKey().equals(ModCreativeTabs.AMMO_TAB.getKey())) return;
            TimelessAPI.getAllCommonAmmoIndex().stream()
                    .sorted(java.util.Comparator.comparingInt(e -> e.getValue().getSort()))
                    .forEach(e -> {
                        for (int tier = 2; tier <= 3; tier++) {
                            if (BsbConfig.enabled(e.getKey().toString(), tier)) event.accept(AmmoTransactions.stack(e.getKey(), tier, 1));
                        }
                    });
        }
    }
}


package com.sange.tacz_bsb.gametest;

import com.sange.tacz_bsb.ammo.AmmoTransactions;
import com.tacz.guns.api.TimelessAPI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import java.util.List;

/** Development-only renderer smoke test, excluded from the release JAR. */
@EventBusSubscriber(modid = "tacz_bsb", value = Dist.CLIENT)
public final class ClientSmokeTest {
    private static int ticks;
    private static boolean started;
    private static boolean opening;
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("tacz_bsb.clientSmoke")) return;
        var mc = Minecraft.getInstance();
        if (!opening && mc.getOverlay() == null && mc.screen != null) {
            opening = true;
            mc.createWorldOpenFlows().openWorld("BSBTest", () -> { throw new IllegalStateException("Cannot open smoke world"); });
        }
        if (!started && mc.level != null && mc.getOverlay() == null && !TimelessAPI.getAllClientAmmoIndex().isEmpty()) {
            if (TimelessAPI.getAllClientAmmoIndex().size() != 24) throw new IllegalStateException("Expected 24 client ammo indexes");
            net.minecraft.world.item.CreativeModeTabs.tryRebuildTabContents(mc.level.enabledFeatures(), true, mc.level.registryAccess());
            long variants = com.tacz.guns.init.ModCreativeTabs.AMMO_TAB.get().getDisplayItems().stream()
                    .filter(AmmoTransactions::precise).count();
            if (variants != 24) throw new IllegalStateException("Expected 24 precise ammo in TaCZ creative tab, got " + variants);
            for (var entry : TimelessAPI.getAllClientAmmoIndex()) {
                var ordinary = AmmoTransactions.stack(entry.getKey(), false, 1);
                var precise = AmmoTransactions.stack(entry.getKey(), true, 1);
                var baseModel = mc.getItemRenderer().getModel(ordinary, mc.level, mc.player, 0);
                var preciseModel = mc.getItemRenderer().getModel(precise, mc.level, mc.player, 0);
                if (baseModel.usesBlockLight() != preciseModel.usesBlockLight()) throw new IllegalStateException("Ammo GUI lighting differs");
                for (var context : net.minecraft.world.item.ItemDisplayContext.values()) {
                    var a = baseModel.getTransforms().getTransform(context);
                    var b = preciseModel.getTransforms().getTransform(context);
                    if (!a.scale.equals(b.scale) || !a.rotation.equals(b.rotation) || !a.translation.equals(b.translation))
                        throw new IllegalStateException("Ammo model transform differs: " + entry.getKey() + " " + context);
                }
            }
            mc.setScreen(new Preview());
            started = true;
        }
        if (started && ++ticks == 40) Screenshot.grab(mc.gameDirectory, "bsb-client-smoke.png", mc.getMainRenderTarget(), message -> {});
        if (started && ticks == 50) Screenshot.grab(mc.gameDirectory, "bsb-glint-animation.png", mc.getMainRenderTarget(), message -> {});
        if (started && ticks == 60) {
            mc.setScreen(null);
            var uuid = mc.player.getUUID();
            var server = mc.getSingleplayerServer();
            server.execute(() -> {
                var player = server.getPlayerList().getPlayer(uuid);
                player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
                var stack = com.tacz.guns.api.item.builder.GunItemBuilder.create()
                        .setId(net.minecraft.resources.ResourceLocation.parse("tacz:glock_17"))
                        .setFireMode(com.tacz.guns.api.item.gun.FireMode.SEMI).build(server.registryAccess());
                var state = com.sange.tacz_bsb.ammo.AmmoLedger.ordinary(1, false);
                state.chamber = 2;
                com.sange.tacz_bsb.ammo.AmmoState.write(stack, com.tacz.guns.api.item.IGun.getIGunOrNull(stack), state);
                player.getInventory().selected = 0;
                player.getInventory().setItem(0, stack);
                player.inventoryMenu.broadcastChanges();
            });
        }
        if (started && ticks == 120) {
            var state = com.sange.tacz_bsb.ammo.AmmoState.display(mc.player.getMainHandItem());
            if (state == null || state.chamber != 2 || state.magazine.size() != 1) throw new IllegalStateException("Ammo state did not synchronize to client");
            Screenshot.grab(mc.gameDirectory, "bsb-hud-smoke.png", mc.getMainRenderTarget(), message -> {});
        }
        if (started && ticks == 160) {
            var uuid = mc.player.getUUID();
            var server = mc.getSingleplayerServer();
            server.execute(() -> {
                var player = server.getPlayerList().getPlayer(uuid);
                var stack = com.tacz.guns.api.item.builder.GunItemBuilder.create()
                        .setId(net.minecraft.resources.ResourceLocation.parse("tacz:rpg7"))
                        .setFireMode(com.tacz.guns.api.item.gun.FireMode.SEMI).setAmmoCount(1).setAmmoInBarrel(true)
                        .build(server.registryAccess());
                var state = com.sange.tacz_bsb.ammo.AmmoLedger.ordinary(0, false);
                state.magazine.addLast(true, 1);
                com.sange.tacz_bsb.ammo.AmmoState.write(stack, com.tacz.guns.api.item.IGun.getIGunOrNull(stack), state);
                player.getInventory().setItem(0, stack);
                player.inventoryMenu.broadcastChanges();
            });
        }
        if (started && ticks == 220) {
            var state = com.sange.tacz_bsb.ammo.AmmoState.display(mc.player.getMainHandItem());
            if (state == null || state.nextRound(true, false) != 2) throw new IllegalStateException("RPG precision display regression");
            Screenshot.grab(mc.gameDirectory, "bsb-rpg-hud.png", mc.getMainRenderTarget(), message -> {});
        }
        if (started && (ticks == 230 || ticks == 300)) {
            boolean precise = ticks == 300;
            var uuid = mc.player.getUUID();
            var server = mc.getSingleplayerServer();
            server.execute(() -> {
                var player = server.getPlayerList().getPlayer(uuid);
                player.getInventory().setItem(0, AmmoTransactions.stack(net.minecraft.resources.ResourceLocation.parse("tacz:9mm"), precise, 1));
                player.inventoryMenu.broadcastChanges();
            });
        }
        if (started && ticks == 290) Screenshot.grab(mc.gameDirectory, "bsb-hand-normal.png", mc.getMainRenderTarget(), message -> {});
        if (started && ticks == 360) Screenshot.grab(mc.gameDirectory, "bsb-hand-precise.png", mc.getMainRenderTarget(), message -> {});
        if (started && ticks == 400) {
            System.out.println("BSB_CLIENT_SMOKE_PASSED: 24 paired icons rendered, equal model transforms/lighting, creative tab, state sync, RPG HUD and held ammo checked");
            mc.stop();
        }
    }
    private static final class Preview extends Screen {
        private final List<ItemStack> items = TimelessAPI.getAllClientAmmoIndex().stream()
                .sorted(java.util.Comparator.comparing(e -> e.getKey().toString()))
                .map(e -> AmmoTransactions.stack(e.getKey(), true, 1)).toList();
        Preview() { super(Component.literal("BSB rendering smoke test")); }
        @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            graphics.fill(0, 0, width, height, 0xFF20242A);
            graphics.drawString(font, "Ordinary / Precise - 24 ammo pairs", 12, 8, 0xFFFFFF);
            for (int i = 0; i < items.size(); i++) {
                int x = 12 + (i % 3) * (width / 3), y = 30 + (i / 3) * 24;
                var stack = items.get(i);
                var id = ((com.tacz.guns.api.item.IAmmo)stack.getItem()).getAmmoId(stack);
                graphics.renderItem(AmmoTransactions.stack(id, false, 1), x, y);
                graphics.renderItem(stack, x + 20, y);
                graphics.drawString(font, font.plainSubstrByWidth(id.getPath(), width / 3 - 48), x + 40, y + 4, 0xFFD36B);
            }
        }
    }
}

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
            mc.setScreen(new Preview());
            started = true;
        }
        if (started && ++ticks == 40) Screenshot.grab(mc.gameDirectory, "bsb-client-smoke.png", mc.getMainRenderTarget(), message -> {});
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
            System.out.println("BSB_CLIENT_SMOKE_PASSED: 24 variants rendered, creative tab populated, chamber/magazine synchronized, HUD rendered");
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
            graphics.drawString(font, "TACZ: Better Streamline Bullet - 24 ammo variants", 12, 8, 0xFFFFFF);
            for (int i = 0; i < items.size(); i++) {
                int x = 12 + (i % 3) * (width / 3), y = 30 + (i / 3) * 24;
                var stack = items.get(i);
                graphics.renderItem(stack, x, y);
                graphics.drawString(font, font.plainSubstrByWidth(stack.getHoverName().getString(), width / 3 - 24), x + 20, y + 4, 0xFFD36B);
            }
        }
    }
}

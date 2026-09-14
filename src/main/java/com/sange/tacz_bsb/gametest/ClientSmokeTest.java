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
                    .filter(item -> AmmoTransactions.tier(item) > 1).count();
            if (variants != 48) throw new IllegalStateException("Expected 48 enhanced ammo in TaCZ creative tab, got " + variants);
            for (var entry : TimelessAPI.getAllClientAmmoIndex()) for (int tier = 2; tier <= 3; tier++) {
                var ordinary = AmmoTransactions.stack(entry.getKey(), 1, 1);
                var precise = AmmoTransactions.stack(entry.getKey(), tier, 1);
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
            var createTab = net.minecraft.core.registries.BuiltInRegistries.CREATIVE_MODE_TAB.get(
                    net.minecraft.resources.ResourceLocation.parse("tacz_c:timeless_and_classics_zero_creatified"));
            for (var item : com.sange.tacz_bsb.BsbMaterials.ITEMS.values()) {
                boolean hidden = com.sange.tacz_bsb.client.AssemblyVisibility.hidden(item.get().getDefaultInstance());
                if (createTab.getDisplayItems().stream().anyMatch(stack -> stack.is(item.get())) == hidden)
                    throw new IllegalStateException("Incorrect material visibility: " + item.getId());
                if (hidden && net.minecraft.world.item.CreativeModeTabs.searchTab().getDisplayItems().stream().anyMatch(stack -> stack.is(item.get())))
                    throw new IllegalStateException("Unfinished material appears in creative search: " + item.getId());
                if (hidden && !item.get().getDefaultInstance().is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.ResourceLocation.parse("c:hidden_from_recipe_viewers"))))
                    throw new IllegalStateException("Missing recipe viewer hide tag: " + item.getId());
                var stack = item.get().getDefaultInstance();
                if (mc.getItemRenderer().getModel(stack, mc.level, mc.player, 0) == mc.getModelManager().getMissingModel())
                    throw new IllegalStateException("Missing material model: " + item.getId());
                if (!stack.hasFoil()) throw new IllegalStateException("Missing material glint: " + item.getId());
            }
            for (var box : new com.sange.tacz_bsb.item.UniversalAmmoBoxItem[]{
                    com.sange.tacz_bsb.BsbContent.IMPROVED_UNIVERSAL_AMMO_BOX.get(),
                    com.sange.tacz_bsb.BsbContent.PRECISE_UNIVERSAL_AMMO_BOX.get()}) {
                var stack = box.getDefaultInstance();
                if (!com.tacz.guns.init.ModCreativeTabs.OTHER_TAB.get().getDisplayItems().stream().anyMatch(item -> item.is(box)))
                    throw new IllegalStateException("Universal box missing from TaCZ creative tab");
                if (!stack.hasFoil() || stack.getHoverName().getStyle().getColor().getValue() != com.sange.tacz_bsb.item.TieredAmmoItem.color(box.tier()))
                    throw new IllegalStateException("Universal box style mismatch");
                if (mc.getItemRenderer().getModel(stack, mc.level, mc.player, 0) == mc.getModelManager().getMissingModel())
                    throw new IllegalStateException("Missing universal box model");
            }
            mc.setScreen(new Preview());
            started = true;
        }
        if (started && ++ticks == 40) Screenshot.grab(mc.gameDirectory, "bsb-client-smoke.png", mc.getMainRenderTarget(), message -> {});
        if (started && ticks == 50) Screenshot.grab(mc.gameDirectory, "bsb-glint-animation.png", mc.getMainRenderTarget(), message -> {});
        if (started && ticks == 52) mc.setScreen(new MaterialsPreview());
        if (started && ticks == 58) Screenshot.grab(mc.gameDirectory, "bsb-materials.png", mc.getMainRenderTarget(), message -> {});
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
        if (started && ticks == 126) mc.setScreen(new BoxTooltipPreview());
        if (started && ticks == 140) Screenshot.grab(mc.gameDirectory, "bsb-box-tooltips.png", mc.getMainRenderTarget(), message -> {});
        if (started && ticks == 143) mc.setScreen(new UniversalBoxPreview());
        if (started && ticks == 148) Screenshot.grab(mc.gameDirectory, "bsb-universal-boxes.png", mc.getMainRenderTarget(), message -> {});
        if (started && ticks == 150) mc.setScreen(null);
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
                state.magazine.addLast(3, 1);
                com.sange.tacz_bsb.ammo.AmmoState.write(stack, com.tacz.guns.api.item.IGun.getIGunOrNull(stack), state);
                player.getInventory().setItem(0, stack);
                player.inventoryMenu.broadcastChanges();
            });
        }
        if (started && ticks == 220) {
            var state = com.sange.tacz_bsb.ammo.AmmoState.display(mc.player.getMainHandItem());
            if (state == null || state.nextRound(true, false) != 3) throw new IllegalStateException("RPG precision display regression");
            Screenshot.grab(mc.gameDirectory, "bsb-rpg-hud.png", mc.getMainRenderTarget(), message -> {});
        }
        if (started && (ticks == 230 || ticks == 300)) {
            int tier = ticks == 300 ? 3 : 1;
            var uuid = mc.player.getUUID();
            var server = mc.getSingleplayerServer();
            server.execute(() -> {
                var player = server.getPlayerList().getPlayer(uuid);
                player.getInventory().setItem(0, AmmoTransactions.stack(net.minecraft.resources.ResourceLocation.parse("tacz:9mm"), tier, 1));
                player.inventoryMenu.broadcastChanges();
            });
        }
        if (started && ticks == 290) Screenshot.grab(mc.gameDirectory, "bsb-hand-normal.png", mc.getMainRenderTarget(), message -> {});
        if (started && ticks == 360) Screenshot.grab(mc.gameDirectory, "bsb-hand-precise.png", mc.getMainRenderTarget(), message -> {});
        if (started && ticks == 400) {
            if (net.neoforged.fml.ModList.get().isLoaded("jei") && !JeiSmokeTest.passed)
                throw new IllegalStateException("JEI visibility verification did not complete");
            System.out.println("BSB_CLIENT_SMOKE_PASSED: 24 three-tier icon sets rendered, equal model transforms/lighting, creative tabs, 85 material models, state sync, RPG HUD and held ammo checked");
            mc.stop();
        }
    }
    private static final class UniversalBoxPreview extends Screen {
        UniversalBoxPreview() { super(Component.literal("Universal ammo boxes")); }
        @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            graphics.fill(0, 0, width, height, 0xFF20242A);
            ItemStack[] boxes = {com.sange.tacz_bsb.BsbContent.IMPROVED_UNIVERSAL_AMMO_BOX.get().getDefaultInstance(),
                    com.sange.tacz_bsb.BsbContent.PRECISE_UNIVERSAL_AMMO_BOX.get().getDefaultInstance()};
            for (int i = 0; i < boxes.length; i++) {
                graphics.renderItem(boxes[i], 20, 25 + i * 75);
                graphics.renderTooltip(font, boxes[i], 50, 25 + i * 75);
            }
        }
    }
    private static final class BoxTooltipPreview extends Screen {
        private final java.util.ArrayList<ItemStack> boxes = new java.util.ArrayList<>();
        BoxTooltipPreview() {
            super(Component.literal("Ammo box tooltip verification"));
            for (var entry : TimelessAPI.getAllClientAmmoIndex()) for (int tier = 1; tier <= 3; tier++) {
                for (boolean creative : new boolean[]{false, true}) {
                    ItemStack stack = new ItemStack(com.tacz.guns.init.ModItems.AMMO_BOX.get());
                    var box = (com.tacz.guns.api.item.IAmmoBox) stack.getItem();
                    box.setAmmoId(stack, entry.getKey());
                    box.setAmmoCount(stack, 64);
                    if (creative) box.setCreative(stack, false);
                    stack.set(com.sange.tacz_bsb.BsbContent.BOX_TIER.get(), tier);
                    var saved = stack.copy();
                    var tooltip = (com.tacz.guns.inventory.tooltip.AmmoBoxTooltip) stack.getTooltipImage().orElseThrow();
                    var ammo = tooltip.getAmmo();
                    if (AmmoTransactions.tier(ammo) != tier || !((com.tacz.guns.api.item.IAmmo) ammo.getItem()).getAmmoId(ammo).equals(entry.getKey()))
                        throw new IllegalStateException("Wrong inline ammo type: " + entry.getKey() + " tier " + tier);
                    if (tier > 1 && (!ammo.hasFoil() || ammo.getHoverName().getStyle().getColor() == null
                            || ammo.getHoverName().getStyle().getColor().getValue() != com.sange.tacz_bsb.item.TieredAmmoItem.color(tier)))
                        throw new IllegalStateException("Wrong inline ammo name color/glint");
                    var clientTooltip = new com.tacz.guns.client.tooltip.ClientAmmoBoxTooltip(tooltip);
                    if (clientTooltip.getWidth(Minecraft.getInstance().font) < Minecraft.getInstance().font.width(ammo.getHoverName()) + 22)
                        throw new IllegalStateException("Inline ammo name does not fit");
                    if (!ItemStack.matches(stack, saved) || tooltip.getCount() != box.getAmmoCount(stack))
                        throw new IllegalStateException("Tooltip modified ammo-box contents");
                    if (!creative && entry.getKey().toString().equals("tacz:9mm")) boxes.add(stack.copy());
                    box.setAmmoCount(stack, 0);
                    if (stack.getTooltipImage().isPresent() != creative) throw new IllegalStateException("Incorrect empty/infinite box display");
                    box.setAmmoId(stack, com.tacz.guns.api.DefaultAssets.EMPTY_AMMO_ID);
                    if (stack.getTooltipImage().isPresent()) throw new IllegalStateException("Unassigned box shows ammo icon");
                }
            }
            System.out.println("BSB_BOX_TOOLTIP_PASSED: 24 calibers x 3 tiers x normal/creative boxes, names, colors, glint, count, empty boxes");
        }
        @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            graphics.fill(0, 0, width, height, 0xFF20242A);
            for (int i = 0; i < boxes.size(); i++) graphics.renderTooltip(font, boxes.get(i), 20, 12 + i * 75);
        }
    }
    private static final class MaterialsPreview extends Screen {
        MaterialsPreview() { super(Component.literal("BSB materials")); }
        @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            graphics.fill(0, 0, width, height, 0xFF20242A);
            graphics.drawString(font, "85 glint materials / 38 stackable / 47 assembly intermediates", 12, 8, 0xFFFFFF);
            int i = 0;
            for (var item : com.sange.tacz_bsb.BsbMaterials.ITEMS.values()) {
                int x = 12 + (i % 15) * 26, y = 35 + (i / 15) * 26;
                graphics.renderItem(item.get().getDefaultInstance(), x, y);
                i++;
            }
        }
    }
    private static final class Preview extends Screen {
        private final List<ItemStack> items = TimelessAPI.getAllClientAmmoIndex().stream()
                .sorted(java.util.Comparator.comparing(e -> e.getKey().toString()))
                .map(e -> AmmoTransactions.stack(e.getKey(), 2, 1)).toList();
        Preview() { super(Component.literal("BSB rendering smoke test")); }
        @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            graphics.fill(0, 0, width, height, 0xFF20242A);
            graphics.drawString(font, "Ordinary / Improved / Precise - 24 calibers", 12, 8, 0xFFFFFF);
            for (int i = 0; i < items.size(); i++) {
                int x = 12 + (i % 3) * (width / 3), y = 30 + (i / 3) * 24;
                var stack = items.get(i);
                var id = ((com.tacz.guns.api.item.IAmmo)stack.getItem()).getAmmoId(stack);
                graphics.renderItem(AmmoTransactions.stack(id, 1, 1), x, y);
                graphics.renderItem(stack, x + 20, y);
                graphics.renderItem(AmmoTransactions.stack(id, 3, 1), x + 40, y);
                graphics.drawString(font, font.plainSubstrByWidth(id.getPath(), width / 3 - 68), x + 60, y + 4, 0xFFD36B);
            }
        }
    }
}

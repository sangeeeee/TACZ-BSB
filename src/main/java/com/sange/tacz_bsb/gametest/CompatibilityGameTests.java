package com.sange.tacz_bsb.gametest;

import com.sange.tacz_bsb.BsbContent;
import com.sange.tacz_bsb.ammo.*;
import com.sange.tacz_bsb.mixin.BulletAccessor;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.IAmmoBox;
import com.tacz.guns.api.item.builder.GunItemBuilder;
import com.tacz.guns.api.item.gun.FireMode;
import com.tacz.guns.entity.EntityKineticBullet;
import com.tacz.guns.init.ModItems;
import com.tacz.guns.item.ModernKineticGunScriptAPI;
import com.tacz.guns.resource.modifier.AttachmentCacheProperty;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.ArrayList;
import java.util.function.Consumer;

@GameTestHolder("tacz_bsb")
@PrefixGameTestTemplate(false)
public final class CompatibilityGameTests {
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void recipes(GameTestHelper test) {
        int changed = 0;
        for (var holder : test.getLevel().getRecipeManager().getRecipes()) {
            if (holder.id().getNamespace().equals("tacz_c") && holder.value() instanceof SequencedAssemblyRecipe recipe) {
                for (var result : recipe.resultPool) {
                    var stack = result.getStack();
                    test.assertFalse(stack.is(ModItems.AMMO.get()), "An ordinary assembly output remains: " + holder.id());
                    if (stack.is(BsbContent.IMPROVED_AMMO.get())) changed++;
                }
            }
        }
        test.assertValueEqual(changed, 24, "24 default assembly outputs");
        test.assertValueEqual(TimelessAPI.getAllCommonAmmoIndex().size(), 24, "default ammo coverage");
        test.assertTrue(test.getLevel().getRecipeManager().byKey(ResourceLocation.parse("tacz:ammo/9mm")).isPresent(),
                "Original workbench recipe must remain");
        test.succeed();
    }
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void preciseProductionChains(GameTestHelper test) {
        int recipes = 0, ammo = 0, assemblies = 0;
        for (var holder : test.getLevel().getRecipeManager().getRecipes()) {
            if (!holder.id().getNamespace().equals("tacz_bsb")) continue;
            recipes++;
            if (!(holder.value() instanceof SequencedAssemblyRecipe recipe)) continue;
            assemblies++;
            ItemStack input = recipe.getIngredient().getItems()[0].copyWithCount(1);
            for (int i = 0; i < recipe.getLoops() * recipe.getSequence().size(); i++) {
                var step = recipe.getSequence().get(i % recipe.getSequence().size()).getRecipe();
                com.simibubi.create.content.processing.recipe.ProcessingRecipe<?, ?> selected;
                if (step instanceof com.simibubi.create.content.kinetics.deployer.DeployerApplicationRecipe) {
                    var slots = new net.neoforged.neoforge.items.ItemStackHandler(2);
                    slots.setStackInSlot(0, input);
                    slots.setStackInSlot(1, step.getIngredients().get(1).getItems()[0].copyWithCount(1));
                    var chosen = SequencedAssemblyRecipe.getRecipe(test.getLevel(),
                            new net.neoforged.neoforge.items.wrapper.RecipeWrapper(slots),
                            com.simibubi.create.AllRecipeTypes.DEPLOYING.getType(),
                            com.simibubi.create.content.kinetics.deployer.DeployerApplicationRecipe.class).orElseThrow();
                    test.assertValueEqual(chosen.id(), holder.id(), "deployer selects correct tier: " + holder.id() + " step " + i);
                    selected = chosen.value();
                } else {
                    var chosen = SequencedAssemblyRecipe.getRecipe(test.getLevel(), input,
                            com.simibubi.create.AllRecipeTypes.PRESSING.getType(),
                            com.simibubi.create.content.kinetics.press.PressingRecipe.class).orElseThrow();
                    test.assertValueEqual(chosen.id(), holder.id(), "press selects correct tier: " + holder.id());
                    selected = chosen.value();
                }
                var outputs = selected.rollResults(test.getLevel().random);
                test.assertValueEqual(outputs.size(), 1, "assembly step output count");
                input = outputs.getFirst();
            }
            test.assertTrue(ItemStack.matches(input, recipe.getResultItem(test.getLevel().registryAccess())), "completed assembly: " + holder.id());
            if (input.is(BsbContent.PRECISE_AMMO.get())) ammo++;
        }
        test.assertValueEqual(recipes, 64, "all new recipes loaded");
        test.assertValueEqual(assemblies, 47, "all precise assembly chains");
        test.assertValueEqual(ammo, 24, "all precise final ammunition outputs");
        test.assertValueEqual(com.sange.tacz_bsb.BsbMaterials.ITEMS.size(), 85, "registered materials");
        for (var entry : com.sange.tacz_bsb.BsbMaterials.ITEMS.values())
            test.assertTrue(entry.get().getDefaultInstance().hasFoil(), "material glint");
        test.succeed();
    }
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void threeTierReloadSaveUnload(GameTestHelper test) {
        var api = setup(test, "tacz:glock_17");
        var player = (ServerPlayer) api.getShooter();
        var id = ResourceLocation.parse("tacz:9mm");
        for (int tier = 1; tier <= 3; tier++) player.getInventory().setItem(tier, AmmoTransactions.stack(id, tier, 3));
        test.assertValueEqual(api.consumeAmmoFromPlayer(12), 9, "extract all three tiers");
        api.putAmmoInMagazine(9);
        api.removeAmmoFromMagazine(1);
        api.setAmmoInBarrel(true);
        for (int i = 0; i < 4; i++) api.reduceAmmoOnce();
        var state = AmmoState.read(api.getItemStack(), api.getAbstractGunItem());
        test.assertValueEqual(state.chamber, 2, "improved round in chamber");
        test.assertValueEqual(state.countTier(1), 0, "ordinary rounds spent");
        test.assertValueEqual(state.countTier(2), 2, "improved remainder");
        test.assertValueEqual(state.countTier(3), 3, "precise remainder");
        var saved = api.getItemStack().save(test.getLevel().registryAccess());
        var restored = ItemStack.parseOptional(test.getLevel().registryAccess(), (net.minecraft.nbt.CompoundTag) saved);
        var restoredState = AmmoState.read(restored, IGun.getIGunOrNull(restored));
        test.assertValueEqual(restoredState.magazine.runs(), state.magazine.runs(), "three-tier save ordering");
        test.assertValueEqual(restoredState.chamber, state.chamber, "saved chamber");
        AmmoTransactions.unload(player, api.getItemStack(), true);
        int[] returned = new int[4];
        for (var item : player.getInventory().items) if (item.getItem() instanceof com.tacz.guns.api.item.IAmmo)
            returned[AmmoTransactions.tier(item)] += item.getCount();
        test.assertValueEqual(returned[1], 0, "ordinary unload");
        test.assertValueEqual(returned[2], 2, "improved unload");
        test.assertValueEqual(returned[3], 3, "precise unload");
        test.succeed();
    }
    private static ModernKineticGunScriptAPI setup(GameTestHelper test, String gunId) {
        ServerPlayer player = net.neoforged.neoforge.common.util.FakePlayerFactory.get(test.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "BSBTest"));
        player.setPos(test.absoluteVec(new net.minecraft.world.phys.Vec3(2, 2, 2)));
        player.setGameMode(GameType.SURVIVAL);
        var stack = GunItemBuilder.create().setId(ResourceLocation.parse(gunId)).setFireMode(FireMode.SEMI).build(test.getLevel().registryAccess());
        test.assertFalse(stack.isEmpty(), "Gun index must be loaded: " + gunId);
        player.getInventory().selected = 0;
        player.getInventory().setItem(0, stack);
        var operator = IGunOperator.fromLivingEntity(player);
        var cache = new AttachmentCacheProperty();
        cache.eval(stack, TimelessAPI.getCommonGunIndex(ResourceLocation.parse(gunId)).orElseThrow().getGunData());
        operator.updateCacheProperty(cache);
        operator.getDataHolder().currentGunItem = () -> stack;
        var api = new ModernKineticGunScriptAPI();
        api.setShooter(player);
        api.setItemStack(stack);
        api.setDataHolder(operator.getDataHolder());
        return api;
    }
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void mixedReloadUnloadAndSave(GameTestHelper test) {
        var api = setup(test, "tacz:glock_17");
        var player = (ServerPlayer) api.getShooter();
        var id = ResourceLocation.parse("tacz:9mm");
        player.getInventory().setItem(1, AmmoTransactions.stack(id, 1, 2));
        player.getInventory().setItem(2, AmmoTransactions.stack(id, 2, 3));
        test.assertValueEqual(api.consumeAmmoFromPlayer(10), 5, "actual extracted count");
        test.assertValueEqual(api.putAmmoInMagazine(10), 5, "unbacked rounds rejected");
        api.removeAmmoFromMagazine(1);
        api.setAmmoInBarrel(true);
        test.assertTrue(api.reduceAmmoOnce(), "first ordinary round");
        test.assertTrue(api.reduceAmmoOnce(), "second ordinary round");
        var state = AmmoState.read(api.getItemStack(), api.getAbstractGunItem());
        test.assertValueEqual(state.chamber, 2, "precise chamber after two ordinary shots");
        test.assertValueEqual(state.countTier(2), 3, "precise quantity");
        var saved = api.getItemStack().save(test.getLevel().registryAccess());
        var restored = ItemStack.parseOptional(test.getLevel().registryAccess(), (net.minecraft.nbt.CompoundTag)saved);
        test.assertValueEqual(AmmoState.read(restored, IGun.getIGunOrNull(restored)).countTier(2), 3, "save/reload");
        AmmoTransactions.unload(player, api.getItemStack(), true);
        int precise = 0;
        for (var item : player.getInventory().items) if ((AmmoTransactions.tier(item) == 2)) precise += item.getCount();
        test.assertValueEqual(precise, 3, "unload preserves precise rounds");
        test.assertValueEqual(api.getAmmoAmount(), 0, "empty magazine");
        test.assertFalse(api.hasAmmoInBarrel(), "empty chamber");
        test.succeed();
    }
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void boxesKeepQuality(GameTestHelper test) {
        var api = setup(test, "tacz:glock_17");
        var player = (ServerPlayer) api.getShooter();
        var id = ResourceLocation.parse("tacz:9mm");
        ItemStack boxStack = new ItemStack(ModItems.AMMO_BOX.get());
        IAmmoBox box = (IAmmoBox) boxStack.getItem();
        var container = new SimpleContainer(1);
        Slot slot = new Slot(container, 0, 0, 0);
        container.setItem(0, AmmoTransactions.stack(id, 2, 6));
        test.assertTrue(boxStack.getItem().overrideStackedOnOther(boxStack, slot, ClickAction.SECONDARY, player), "box insertion");
        test.assertTrue((AmmoTransactions.boxTier(boxStack) == 2), "box quality");
        container.setItem(0, AmmoTransactions.stack(id, 1, 2));
        test.assertFalse(boxStack.getItem().overrideStackedOnOther(boxStack, slot, ClickAction.SECONDARY, player), "reject mixing in box");
        test.assertValueEqual(box.getAmmoCount(boxStack), 6, "box count unchanged");
        container.setItem(0, ItemStack.EMPTY);
        test.assertTrue(boxStack.getItem().overrideStackedOnOther(boxStack, slot, ClickAction.SECONDARY, player), "box withdrawal");
        test.assertTrue((AmmoTransactions.tier(slot.getItem()) == 2), "withdrawal item quality");
        test.assertValueEqual(slot.getItem().getCount(), 6, "withdrawal count");
        box.setCreative(boxStack, false);
        container.setItem(0, AmmoTransactions.stack(id, 2, 4));
        test.assertTrue(boxStack.getItem().overrideStackedOnOther(boxStack, slot, ClickAction.SECONDARY, player), "configure creative precise box");
        test.assertValueEqual(box.getAmmoCount(boxStack), Integer.MAX_VALUE, "creative box supply");
        test.assertValueEqual(slot.getItem().getCount(), 4, "creative box does not consume selector ammo");
        player.getInventory().setItem(1, boxStack);
        test.assertValueEqual(api.consumeAmmoFromPlayer(3), 3, "extract from creative box");
        api.putAmmoInMagazine(3);
        test.assertValueEqual(AmmoState.read(api.getItemStack(), api.getAbstractGunItem()).countTier(2), 3, "creative box preserves precision");
        test.assertValueEqual(box.getAmmoCount(boxStack), Integer.MAX_VALUE, "creative supply remains infinite");
        test.succeed();
    }
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void automaticChamberAndCancellation(GameTestHelper test) {
        var api = setup(test, "tacz:glock_17");
        var data = IGunOperator.fromLivingEntity(api.getShooter()).getDataHolder();
        data.shootTimestamp = -10000;
        var state = AmmoLedger.ordinary(0, false);
        state.magazine.addLast(2, 1);
        state.magazine.addLast(1, 1);
        AmmoState.write(api.getItemStack(), api.getAbstractGunItem(), state);
        var draw = new com.tacz.guns.entity.shooter.LivingEntityDrawGun(api.getShooter(), data);
        var shoot = new com.tacz.guns.entity.shooter.LivingEntityShoot(api.getShooter(), data, draw);
        Consumer<com.tacz.guns.api.event.common.GunShootEvent> cancel = e -> e.setCanceled(true);
        NeoForge.EVENT_BUS.addListener(cancel);
        try {
            test.assertValueEqual(shoot.shoot(() -> 0f, () -> 0f, System.currentTimeMillis() - data.baseTimestamp),
                    com.tacz.guns.api.entity.ShootResult.FORGE_EVENT_CANCEL, "cancel shoot event");
        } finally { NeoForge.EVENT_BUS.unregister(cancel); }
        state = AmmoState.read(api.getItemStack(), api.getAbstractGunItem());
        test.assertValueEqual(state.chamber, 2, "automatic chamber retains precise type");
        test.assertValueEqual(state.total(), 2, "cancelled shot conserves all rounds");
        test.assertTrue(api.reduceAmmoOnce(), "consume chambered round");
        test.assertValueEqual(AmmoState.read(api.getItemStack(), api.getAbstractGunItem()).chamber, 1, "next ordinary round");
        test.succeed();
    }
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void originalReloadScriptsConserveAmmo(GameTestHelper test) {
        for (String gunId : new String[]{"tacz:glock_17", "tacz:hk_mk23", "tacz:m870", "tacz:m1014", "tacz:kar98", "tacz:spas_12"}) {
            for (int interruptAt : new int[]{-1, 10, 30, 60}) {
                var api = setup(test, gunId);
                var player = (ServerPlayer)api.getShooter();
                var gun = api.getAbstractGunItem();
                var stack = api.getItemStack();
                var data = IGunOperator.fromLivingEntity(player).getDataHolder();
                var id = AmmoTransactions.ammoId(stack);
                player.getInventory().setItem(1, AmmoTransactions.stack(id, 1, 8));
                player.getInventory().setItem(2, AmmoTransactions.stack(id, 2, 8));
                data.reloadStateType = com.tacz.guns.api.entity.ReloadState.StateType.EMPTY_RELOAD_FEEDING;
                data.reloadTimestamp = System.currentTimeMillis();
                test.assertTrue(gun.startReload(data, stack, player), "start reload: " + gunId);
                for (int tick = 0; tick < 600 && data.reloadStateType.isReloading(); tick++) {
                    data.reloadTimestamp -= 50;
                    if (tick == interruptAt) gun.interruptReload(data, stack, player);
                    data.reloadStateType = gun.tickReload(data, stack, player).getStateType();
                    var ledger = AmmoState.read(stack, gun);
                    int count = ledger.total(), precise = ledger.countTier(2);
                    for (var item : player.getInventory().items) {
                        if (item.getItem() instanceof com.tacz.guns.api.item.IAmmo) {
                            count += item.getCount();
                            if ((AmmoTransactions.tier(item) == 2)) precise += item.getCount();
                        }
                    }
                    test.assertValueEqual(count, 16, "reload count: " + gunId + " tick " + tick);
                    test.assertValueEqual(precise, 8, "precise reload count: " + gunId + " tick " + tick);
                }
                test.assertTrue(AmmoState.read(stack, gun).total() > 0 || interruptAt >= 0, "reload should load ammo: " + gunId);
                AmmoTransactions.unload(player, stack, true);
                int total = 0, precise = 0;
                for (var item : player.getInventory().items) if (item.getItem() instanceof com.tacz.guns.api.item.IAmmo) {
                    total += item.getCount();
                    if ((AmmoTransactions.tier(item) == 2)) precise += item.getCount();
                }
                test.assertValueEqual(total, 16, "unload after script: " + gunId);
                test.assertValueEqual(precise, 8, "precise unload after script: " + gunId);
            }
        }
        test.succeed();
    }
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void inventoryFeedAndDummyAmmo(GameTestHelper test) throws Exception {
        var api = setup(test, "tacz:glock_17");
        var player = (ServerPlayer)api.getShooter();
        var gun = api.getAbstractGunItem();
        var stack = api.getItemStack();
        var reload = api.getGunIndex().getGunData().getReloadData();
        var type = reload.getClass().getDeclaredField("type");
        type.setAccessible(true);
        var old = type.get(reload);
        try {
            type.set(reload, com.tacz.guns.resource.pojo.data.gun.FeedType.INVENTORY);
            var id = AmmoTransactions.ammoId(stack);
            player.getInventory().setItem(1, AmmoTransactions.stack(id, 1, 1));
            player.getInventory().setItem(2, AmmoTransactions.stack(id, 2, 2));
            api.consumeAmmoFromPlayer(1);
            api.setAmmoInBarrel(true);
            test.assertTrue(api.reduceAmmoOnce(), "inventory ordinary chamber shot");
            test.assertValueEqual(AmmoState.read(stack, gun).chamber, 2, "inventory precise chamber");
            test.assertTrue(api.reduceAmmoOnce(), "inventory precise shot");
            AmmoTransactions.unload(player, stack, true);
            int remaining = player.getInventory().items.stream().filter(item -> AmmoTransactions.tier(item) == 2).mapToInt(ItemStack::getCount).sum();
            test.assertValueEqual(remaining, 1, "inventory unload returns chamber only");
        } finally { type.set(reload, old); }
        gun.setDummyAmmoAmount(stack, 7);
        test.assertValueEqual(api.consumeAmmoFromPlayer(4), 4, "dummy supply");
        api.putAmmoInMagazine(4);
        api.removeAmmoFromMagazine(1);
        api.setAmmoInBarrel(true);
        api.reduceAmmoOnce();
        AmmoTransactions.unload(player, stack, true);
        test.assertValueEqual(gun.getDummyAmmoAmount(stack), 6, "dummy refund after one shot");
        test.succeed();
    }
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void projectileDamage(GameTestHelper test) {
        checkDamage(test, "tacz:glock_17", 2, false, 1.5f, 1.5f);
        checkDamage(test, "tacz:rpg7", 2, true, 1.5f, 1.5f);
        checkDamage(test, "tacz:glock_17", 3, false, 2f, 2f);
        checkDamage(test, "tacz:rpg7", 3, true, 2f, 2f);
        test.succeed();
    }
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void independentlyConfiguredDamage(GameTestHelper test) {
        double direct = com.sange.tacz_bsb.BsbConfig.DIRECT.get();
        double explosion = com.sange.tacz_bsb.BsbConfig.EXPLOSION.get();
        try {
            com.sange.tacz_bsb.BsbConfig.DIRECT.set(2.0);
            com.sange.tacz_bsb.BsbConfig.EXPLOSION.set(3.0);
            checkDamage(test, "tacz:rpg7", 2, true, 2f, 3f);
        } finally {
            com.sange.tacz_bsb.BsbConfig.DIRECT.set(direct);
            com.sange.tacz_bsb.BsbConfig.EXPLOSION.set(explosion);
        }
        test.succeed();
    }
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void rpgIgnoresNativeCreativeBarrelFlag(GameTestHelper test) {
        var api = setup(test, "tacz:rpg7");
        var player = (ServerPlayer)api.getShooter();
        var gun = api.getAbstractGunItem();
        var stack = api.getItemStack();
        var data = IGunOperator.fromLivingEntity(player).getDataHolder();
        gun.setBulletInBarrel(stack, true); // TaCZ creative-tab guns carry this flag, even for OPEN_BOLT.
        test.assertValueEqual(AmmoState.read(stack, gun).total(), 0, "barrel flag is not a cartridge");
        var bullets = new ArrayList<EntityKineticBullet>();
        Consumer<EntityJoinLevelEvent> listener = event -> {
            if (event.getEntity() instanceof EntityKineticBullet b && b.getOwner() == player) bullets.add(b);
        };
        NeoForge.EVENT_BUS.addListener(listener);
        try {
            for (int tier : new int[]{1, 2, 3}) {
                player.getInventory().setItem(1, AmmoTransactions.stack(AmmoTransactions.ammoId(stack), tier, 1));
                data.reloadStateType = com.tacz.guns.api.entity.ReloadState.StateType.EMPTY_RELOAD_FEEDING;
                data.reloadTimestamp = System.currentTimeMillis();
                gun.startReload(data, stack, player);
                data.reloadTimestamp -= 10000;
                gun.tickReload(data, stack, player);
                // Exercise the current TaCZ flag without fabricating a separate chambered cartridge.
                gun.setBulletInBarrel(stack, true);
                var state = AmmoState.read(stack, gun);
                test.assertValueEqual(state.total(), 1, "RPG reload contains exactly one rocket");
                test.assertValueEqual(AmmoState.display(stack).nextRound(true, false), tier, "HUD reads magazine for RPG");
                test.assertValueEqual(AmmoTransactions.peek(api), tier, "non-consuming shot quality");
                api.shootOnce(false); // Creative/infinite shooting must use the same quality as survival/HUD.
                test.assertValueEqual(AmmoState.read(stack, gun).total(), 1, "non-consuming shot retains rocket");
                if (tier != 3) test.assertTrue(api.reduceAmmoOnce(), "consume ordinary rocket for next reload");
            }
        } finally { NeoForge.EVENT_BUS.unregister(listener); }
        test.assertValueEqual(bullets.size(), 3, "three RPG projectiles");
        float normal = bullets.get(0).getDamage(bullets.get(0).position());
        test.assertTrue(Math.abs(bullets.get(1).getDamage(bullets.get(1).position()) - normal * 1.5f) < 0.001f, "RPG direct damage");
        float explosion = ((BulletAccessor)bullets.get(0)).bsb$getExplosionDamage();
        test.assertTrue(Math.abs(((BulletAccessor)bullets.get(1)).bsb$getExplosionDamage() - explosion * 1.5f) < 0.001f, "RPG explosive damage");
        test.assertTrue(Math.abs(bullets.get(2).getDamage(bullets.get(2).position()) - normal * 2f) < 0.001f, "precise RPG direct damage");
        test.assertTrue(Math.abs(((BulletAccessor)bullets.get(2)).bsb$getExplosionDamage() - explosion * 2f) < 0.001f, "precise RPG explosive damage");
        AmmoTransactions.unload(player, stack, true);
        int returned = 0;
        for (var item : player.getInventory().items) if (item.getItem() instanceof com.tacz.guns.api.item.IAmmo) {
            test.assertTrue((AmmoTransactions.tier(item) == 3), "no phantom ordinary round on RPG unload");
            returned += item.getCount();
        }
        test.assertValueEqual(returned, 1, "return only the real precise rocket");
        for (var bullet : bullets) bullet.discard();
        test.succeed();
    }
    private static void checkDamage(GameTestHelper test, String gunId, int tier, boolean explosion, float directMultiplier, float explosionMultiplier) {
        var api = setup(test, gunId);
        var state = AmmoLedger.ordinary(api.getBolt() == com.tacz.guns.resource.pojo.data.gun.Bolt.OPEN_BOLT ? 1 : 0,
                api.getBolt() != com.tacz.guns.resource.pojo.data.gun.Bolt.OPEN_BOLT);
        state.magazine.addLast(tier, 1);
        AmmoState.write(api.getItemStack(), api.getAbstractGunItem(), state);
        var bullets = new ArrayList<EntityKineticBullet>();
        Consumer<EntityJoinLevelEvent> listener = event -> {
            if (event.getEntity() instanceof EntityKineticBullet bullet && bullet.getOwner() == api.getShooter()) bullets.add(bullet);
        };
        NeoForge.EVENT_BUS.addListener(listener);
        try {
            api.shootOnce(true);
            // RPGs have a one-round capacity but this synthetic setup deliberately tests the common shot path.
            if (api.getBolt() == com.tacz.guns.resource.pojo.data.gun.Bolt.MANUAL_ACTION && !api.hasAmmoInBarrel() && api.getAmmoAmount() > 0) { api.removeAmmoFromMagazine(1); api.setAmmoInBarrel(true); }
            api.shootOnce(true);
        } finally { NeoForge.EVENT_BUS.unregister(listener); }
        test.assertValueEqual(bullets.size(), 2, "two actual projectiles: " + gunId);
        var normal = bullets.get(0);
        var precise = bullets.get(1);
        for (int distance : new int[]{0, 50, 150}) {
            float base = normal.getDamage(normal.position().add(distance, 0, 0));
            float improved = precise.getDamage(precise.position().add(distance, 0, 0));
            test.assertTrue(Math.abs(improved - base * directMultiplier) < 0.001f, "direct multiplier at distance " + distance);
        }
        if (explosion) {
            float base = ((BulletAccessor)normal).bsb$getExplosionDamage();
            test.assertTrue(base > 0, "RPG explosion base");
            test.assertTrue(Math.abs(((BulletAccessor)precise).bsb$getExplosionDamage() - base * explosionMultiplier) < 0.001f, "explosion multiplier");
        }
        for (var bullet : bullets) bullet.discard();
    }
}


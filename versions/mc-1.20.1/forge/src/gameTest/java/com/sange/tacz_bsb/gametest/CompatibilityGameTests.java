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
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import java.util.ArrayList;
import java.util.function.Consumer;

@GameTestHolder("tacz_bsb")
@PrefixGameTestTemplate(false)
public final class CompatibilityGameTests {
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void invalidNbtCannotDiscardAmmunition(GameTestHelper test) {
        var api = setup(test, "tacz:glock_17");
        var state = AmmoLedger.ordinary(0, false);
        state.reserve.addLast(3, 4);
        AmmoState.write(api.getItemStack(), api.getAbstractGunItem(), state);
        var broken = api.getItemStack().copy();
        broken.getTag().getCompound(BsbContent.AMMO_STATE).remove("reserve");
        boolean rejected = false;
        try { AmmoState.read(broken, api.getAbstractGunItem()); }
        catch (IllegalStateException expected) { rejected = true; }
        test.assertTrue(rejected, "missing pending rounds must not be silently discarded");
        equal(test, AmmoState.read(api.getItemStack(), api.getAbstractGunItem()).reserve.countTier(3), 4,
                "original pending ammunition is unchanged");
        var box = new ItemStack(ModItems.AMMO_BOX.get());
        box.getOrCreateTag().putInt(BsbContent.BOX_TIER, 4);
        rejected = false;
        try { AmmoTransactions.boxTier(box); }
        catch (IllegalStateException expected) { rejected = true; }
        test.assertTrue(rejected, "invalid box tiers must be rejected");
        test.succeed();
    }
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void recipes(GameTestHelper test) {
        int changed = 0;
        for (var holder : test.getLevel().getRecipeManager().getRecipes()) {
            if (holder.getId().getNamespace().equals("tacz_c") && holder instanceof SequencedAssemblyRecipe recipe) {
                for (var result : recipe.resultPool) {
                    var stack = result.getStack();
                    test.assertFalse(stack.is(ModItems.AMMO.get()), "An ordinary assembly output remains: " + holder.getId());
                    if (stack.is(BsbContent.IMPROVED_AMMO.get())) changed++;
                }
            }
        }
        equal(test, changed, 21, "21 Forge default assembly outputs");
        equal(test, TimelessAPI.getAllCommonAmmoIndex().size(), 24, "default ammo coverage");
        test.assertTrue(test.getLevel().getRecipeManager().byKey(ResourceLocation.tryParse("tacz:ammo/9mm")).isPresent(),
                "Original workbench recipe must remain");
        test.succeed();
    }
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void preciseProductionChains(GameTestHelper test) {
        int recipes = 0, ammo = 0, assemblies = 0;
        for (var holder : test.getLevel().getRecipeManager().getRecipes()) {
            if (!holder.getId().getNamespace().equals("tacz_bsb")) continue;
            recipes++;
            if (!(holder instanceof SequencedAssemblyRecipe recipe)) continue;
            assemblies++;
            ItemStack input = recipe.getIngredient().getItems()[0].copyWithCount(1);
            for (int i = 0; i < recipe.getLoops() * recipe.getSequence().size(); i++) {
                var step = recipe.getSequence().get(i % recipe.getSequence().size()).getRecipe();
                if (step.getType() == com.simibubi.create.AllRecipeTypes.FILLING.getType()) {
                    var fluid = new net.minecraftforge.fluids.FluidStack(
                            i == 0 ? net.minecraft.world.level.material.Fluids.LAVA : net.minecraft.world.level.material.Fluids.WATER, 100);
                    var wrongFluid = new net.minecraftforge.fluids.FluidStack(
                            i == 0 ? net.minecraft.world.level.material.Fluids.WATER : net.minecraft.world.level.material.Fluids.LAVA, 100);
                    equal(test, com.simibubi.create.content.fluids.spout.FillingBySpout.getRequiredAmountForItem(
                            test.getLevel(), input, wrongFluid), -1, "wrong fluid rejected");
                    int required = com.simibubi.create.content.fluids.spout.FillingBySpout.getRequiredAmountForItem(test.getLevel(), input, fluid);
                    equal(test, required, 100, "hardening fluid amount");
                    input = com.simibubi.create.content.fluids.spout.FillingBySpout.fillItem(test.getLevel(), required, input, fluid);
                    equal(test, fluid.getAmount(), 0, "spout consumes fluid");
                    continue;
                }
                com.simibubi.create.content.processing.recipe.ProcessingRecipe<?> selected;
                if (step instanceof com.simibubi.create.content.kinetics.deployer.DeployerApplicationRecipe) {
                    var slots = new net.minecraftforge.items.ItemStackHandler(2);
                    slots.setStackInSlot(0, input);
                    slots.setStackInSlot(1, step.getIngredients().get(1).getItems()[0].copyWithCount(1));
                    var chosen = deploy(test, new net.minecraftforge.items.wrapper.RecipeWrapper(slots));
                    test.assertTrue(assemblyId(test, chosen).equals(holder.getId()) || holder.getId().equals(com.sange.tacz_bsb.recipe.AssemblyPairs.other(assemblyId(test, chosen))),
                            "deployer stays within the same ammunition family: " + holder.getId());
                    selected = chosen;
                } else {
                    var chosen = SequencedAssemblyRecipe.getRecipe(test.getLevel(), input,
                            com.simibubi.create.AllRecipeTypes.PRESSING.getType(),
                            com.simibubi.create.content.kinetics.press.PressingRecipe.class).orElseThrow();
                    equal(test, assemblyId(test, chosen), holder.getId(), "press selects correct tier: " + holder.getId());
                    selected = chosen;
                }
                var outputs = selected.rollResults();
                equal(test, outputs.size(), 1, "assembly step output count");
                input = outputs.get(0);
            }
            test.assertTrue(ItemStack.matches(input, recipe.getResultItem(test.getLevel().registryAccess())), "completed assembly: " + holder.getId());
            if (input.is(BsbContent.PRECISE_AMMO.get())) ammo++;
        }
        equal(test, recipes, 58, "all new recipes loaded");
        equal(test, assemblies, 44, "all precise assembly chains");
        equal(test, ammo, 21, "all precise final ammunition outputs");
        equal(test, com.sange.tacz_bsb.BsbMaterials.ITEMS.size(), 79, "registered materials");
        for (var entry : com.sange.tacz_bsb.BsbMaterials.ITEMS.values())
            test.assertTrue(entry.get().getDefaultInstance().hasFoil(), "material glint");
        test.succeed();
    }
    private static com.simibubi.create.content.kinetics.deployer.DeployerApplicationRecipe deploy(
            GameTestHelper test, net.minecraftforge.items.wrapper.RecipeWrapper slots) {
        var block = new com.simibubi.create.content.kinetics.deployer.DeployerBlockEntity(
                net.minecraft.core.registries.BuiltInRegistries.BLOCK_ENTITY_TYPE.get(ResourceLocation.tryParse("create:deployer")), net.minecraft.core.BlockPos.ZERO,
                net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(ResourceLocation.tryParse("create:deployer")).defaultBlockState());
        ((net.minecraft.world.level.block.entity.BlockEntity) block).setLevel(test.getLevel());
        var event = new com.simibubi.create.content.kinetics.deployer.DeployerRecipeSearchEvent(block, slots);
        event.addRecipe(() -> SequencedAssemblyRecipe.getRecipe(test.getLevel(), slots,
                com.simibubi.create.AllRecipeTypes.DEPLOYING.getType(),
                com.simibubi.create.content.kinetics.deployer.DeployerApplicationRecipe.class), 100);
        MinecraftForge.EVENT_BUS.post(event);
        var selected = event.getRecipe();
        if (selected == null) throw new IllegalStateException("No deployer recipe for " + slots.getItem(0) + " / " + slots.getItem(1));
        return (com.simibubi.create.content.kinetics.deployer.DeployerApplicationRecipe) selected;
    }
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void assemblyBranchesLockAfterPowder(GameTestHelper test) {
        for (var pair : com.sange.tacz_bsb.recipe.AssemblyPairs.PAIRS.entrySet()) {
            for (boolean high : new boolean[]{false, true}) {
                var targetId = high ? pair.getValue() : pair.getKey();
                var sourceId = high ? pair.getKey() : pair.getValue();
                var source = (SequencedAssemblyRecipe) test.getLevel().getRecipeManager().byKey(sourceId).orElseThrow();
                var target = (SequencedAssemblyRecipe) test.getLevel().getRecipeManager().byKey(targetId).orElseThrow();
                // Force the shared first operation down the opposite branch, independently of recipe iteration order.
                ItemStack input = source.getTransitionalItem().copyWithCount(1);
                progress(input, sourceId, 1, source.getSequence().size());
                for (int step = 1; step < target.getSequence().size(); step++) {
                    var slots = new net.minecraftforge.items.ItemStackHandler(2);
                    slots.setStackInSlot(0, input);
                    slots.setStackInSlot(1, target.getSequence().get(step).getRecipe().getIngredients().get(1).getItems()[0].copyWithCount(1));
                    var inv = new net.minecraftforge.items.wrapper.RecipeWrapper(slots);
                    var selected = deploy(test, inv);
                    input = selected.rollResults().get(0);
                    if (assemblyId(test, selected).equals(targetId) && (input.hasTag() && input.getTag().contains("SequencedAssembly"))) {
                        // Once the first differing powder was consumed, changing the next held powder cannot switch back.
                        slots.setStackInSlot(0, input);
                        int next = step + 1;
                        if (next < source.getSequence().size()) {
                            slots.setStackInSlot(1, source.getSequence().get(next).getRecipe().getIngredients().get(1).getItems()[0].copyWithCount(1));
                            test.assertTrue(com.sange.tacz_bsb.recipe.AssemblyBranching.alternate(test.getLevel(), inv).isEmpty(), "no mixed-powder upgrade: " + targetId);
                        }
                    }
                }
                test.assertTrue(ItemStack.matches(input, target.getResultItem(test.getLevel().registryAccess())), "branch output: " + targetId);
            }
        }
        test.succeed();
    }
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void threeTierReloadSaveUnload(GameTestHelper test) {
        var api = setup(test, "tacz:glock_17");
        var player = (ServerPlayer) api.getShooter();
        var id = ResourceLocation.tryParse("tacz:9mm");
        for (int tier = 1; tier <= 3; tier++) player.getInventory().setItem(tier, AmmoTransactions.stack(id, tier, 3));
        equal(test, api.consumeAmmoFromPlayer(12), 9, "extract all three tiers");
        api.putAmmoInMagazine(9);
        api.removeAmmoFromMagazine(1);
        api.setAmmoInBarrel(true);
        for (int i = 0; i < 4; i++) api.reduceAmmoOnce();
        var state = AmmoState.read(api.getItemStack(), api.getAbstractGunItem());
        equal(test, state.chamber, 2, "improved round in chamber");
        equal(test, state.countTier(1), 0, "ordinary rounds spent");
        equal(test, state.countTier(2), 2, "improved remainder");
        equal(test, state.countTier(3), 3, "precise remainder");
        var saved = api.getItemStack().save(new net.minecraft.nbt.CompoundTag());
        var restored = ItemStack.of(saved);
        var restoredState = AmmoState.read(restored, IGun.getIGunOrNull(restored));
        equal(test, restoredState.magazine.runs(), state.magazine.runs(), "three-tier save ordering");
        equal(test, restoredState.chamber, state.chamber, "saved chamber");
        AmmoTransactions.unload(player, api.getItemStack(), true);
        int[] returned = new int[4];
        for (var item : player.getInventory().items) if (item.getItem() instanceof com.tacz.guns.api.item.IAmmo)
            returned[AmmoTransactions.tier(item)] += item.getCount();
        equal(test, returned[1], 0, "ordinary unload");
        equal(test, returned[2], 2, "improved unload");
        equal(test, returned[3], 3, "precise unload");
        test.succeed();
    }
    private static ModernKineticGunScriptAPI setup(GameTestHelper test, String gunId) {
        ServerPlayer player = net.minecraftforge.common.util.FakePlayerFactory.get(test.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "BSBTest"));
        player.setPos(test.absoluteVec(new net.minecraft.world.phys.Vec3(2, 2, 2)));
        player.setGameMode(GameType.SURVIVAL);
        var stack = GunItemBuilder.create().setId(ResourceLocation.tryParse(gunId)).setFireMode(FireMode.SEMI).build();
        test.assertFalse(stack.isEmpty(), "Gun index must be loaded: " + gunId);
        player.getInventory().selected = 0;
        player.getInventory().setItem(0, stack);
        var operator = IGunOperator.fromLivingEntity(player);
        var cache = new AttachmentCacheProperty();
        cache.eval(stack, TimelessAPI.getCommonGunIndex(ResourceLocation.tryParse(gunId)).orElseThrow().getGunData());
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
        var id = ResourceLocation.tryParse("tacz:9mm");
        player.getInventory().setItem(1, AmmoTransactions.stack(id, 1, 2));
        player.getInventory().setItem(2, AmmoTransactions.stack(id, 2, 3));
        equal(test, api.consumeAmmoFromPlayer(10), 5, "actual extracted count");
        equal(test, api.putAmmoInMagazine(10), 5, "unbacked rounds rejected");
        api.removeAmmoFromMagazine(1);
        api.setAmmoInBarrel(true);
        test.assertTrue(api.reduceAmmoOnce(), "first ordinary round");
        test.assertTrue(api.reduceAmmoOnce(), "second ordinary round");
        var state = AmmoState.read(api.getItemStack(), api.getAbstractGunItem());
        equal(test, state.chamber, 2, "precise chamber after two ordinary shots");
        equal(test, state.countTier(2), 3, "precise quantity");
        var saved = api.getItemStack().save(new net.minecraft.nbt.CompoundTag());
        var restored = ItemStack.of(saved);
        equal(test, AmmoState.read(restored, IGun.getIGunOrNull(restored)).countTier(2), 3, "save/reload");
        AmmoTransactions.unload(player, api.getItemStack(), true);
        int precise = 0;
        for (var item : player.getInventory().items) if ((AmmoTransactions.tier(item) == 2)) precise += item.getCount();
        equal(test, precise, 3, "unload preserves precise rounds");
        equal(test, api.getAmmoAmount(), 0, "empty magazine");
        test.assertFalse(api.hasAmmoInBarrel(), "empty chamber");
        test.succeed();
    }
    @GameTest(template = "empty", timeoutTicks = 400)
    public static void universalBoxesSupplyEveryGun(GameTestHelper test) {
        for (int tier = 1; tier <= 3; tier++) {
            ItemStack supply = tier == 1 ? new ItemStack(ModItems.AMMO_BOX.get())
                    : new ItemStack(tier == 2 ? BsbContent.IMPROVED_UNIVERSAL_AMMO_BOX.get() : BsbContent.PRECISE_UNIVERSAL_AMMO_BOX.get());
            var box = (IAmmoBox) supply.getItem();
            if (tier == 1) box.setCreative(supply, true);
            var before = supply.copy();
            for (var entry : TimelessAPI.getAllCommonGunIndex()) {
                var api = setup(test, entry.getKey().toString());
                var player = (ServerPlayer) api.getShooter();
                player.getInventory().setItem(1, supply);
                test.assertTrue(box.isAmmoBoxOfGun(api.getItemStack(), supply), "universal caliber match: " + entry.getKey());
                if (api.useInventoryAmmo()) {
                    if (api.getBolt() == com.tacz.guns.resource.pojo.data.gun.Bolt.MANUAL_ACTION) {
                        equal(test, api.consumeAmmoFromPlayer(1), 1, "inventory chamber extraction");
                        api.setAmmoInBarrel(true);
                    }
                    equal(test, AmmoTransactions.fire(api), tier, "inventory-fed shot quality: " + entry.getKey());
                    test.assertTrue(ItemStack.matches(supply, before), "inventory-fed box unchanged");
                    equal(test, box.getAmmoCount(supply), Integer.MAX_VALUE, "inventory-fed unlimited supply");
                    continue;
                }
                equal(test, api.consumeAmmoFromPlayer(2), 2, "universal extraction");
                api.putAmmoInMagazine(2);
                var state = AmmoState.read(api.getItemStack(), api.getAbstractGunItem());
                test.assertTrue(state.total() > 0, "gun received rounds");
                equal(test, state.countTier(tier), state.total(), "correct universal quality: " + entry.getKey());
                if (api.getBolt() != com.tacz.guns.resource.pojo.data.gun.Bolt.OPEN_BOLT) api.setAmmoInBarrel(true);
                equal(test, AmmoTransactions.peek(api), tier, "next shot quality: " + entry.getKey());
                test.assertTrue(ItemStack.matches(supply, before), "unlimited box contents unchanged");
                equal(test, box.getAmmoCount(supply), Integer.MAX_VALUE, "unlimited supply");
            }
        }
        for (int tier = 2; tier <= 3; tier++) for (String id : new String[]{"tacz:glock_17", "tacz:m870", "tacz:rpg7"}) {
            var api = setup(test, id);
            var player = (ServerPlayer) api.getShooter();
            var stack = api.getItemStack();
            var gun = api.getAbstractGunItem();
            player.getInventory().setItem(1, new ItemStack(tier == 2 ? BsbContent.IMPROVED_UNIVERSAL_AMMO_BOX.get() : BsbContent.PRECISE_UNIVERSAL_AMMO_BOX.get()));
            var data = IGunOperator.fromLivingEntity(player).getDataHolder();
            data.reloadStateType = com.tacz.guns.api.entity.ReloadState.StateType.EMPTY_RELOAD_FEEDING;
            data.reloadTimestamp = System.currentTimeMillis();
            test.assertTrue(gun.startReload(data, stack, player), "start universal reload: " + id);
            for (int tick = 0; tick < 600 && data.reloadStateType.isReloading(); tick++) {
                data.reloadTimestamp -= 50;
                data.reloadStateType = gun.tickReload(data, stack, player).getStateType();
            }
            var state = AmmoState.read(stack, gun);
            test.assertTrue(state.total() > 0, "universal reload completed");
            equal(test, state.countTier(tier), state.total(), "actual reload preserves tier: " + id);
        }
        test.succeed();
    }
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void boxesKeepQuality(GameTestHelper test) {
        var api = setup(test, "tacz:glock_17");
        var player = (ServerPlayer) api.getShooter();
        var id = ResourceLocation.tryParse("tacz:9mm");
        ItemStack boxStack = new ItemStack(ModItems.AMMO_BOX.get());
        IAmmoBox box = (IAmmoBox) boxStack.getItem();
        var container = new SimpleContainer(1);
        Slot slot = new Slot(container, 0, 0, 0);
        container.setItem(0, AmmoTransactions.stack(id, 2, 6));
        test.assertTrue(boxStack.getItem().overrideStackedOnOther(boxStack, slot, ClickAction.SECONDARY, player), "box insertion");
        test.assertTrue((AmmoTransactions.boxTier(boxStack) == 2), "box quality");
        container.setItem(0, AmmoTransactions.stack(id, 1, 2));
        test.assertFalse(boxStack.getItem().overrideStackedOnOther(boxStack, slot, ClickAction.SECONDARY, player), "reject mixing in box");
        equal(test, box.getAmmoCount(boxStack), 6, "box count unchanged");
        container.setItem(0, ItemStack.EMPTY);
        test.assertTrue(boxStack.getItem().overrideStackedOnOther(boxStack, slot, ClickAction.SECONDARY, player), "box withdrawal");
        test.assertTrue((AmmoTransactions.tier(slot.getItem()) == 2), "withdrawal item quality");
        equal(test, slot.getItem().getCount(), 6, "withdrawal count");
        box.setCreative(boxStack, false);
        container.setItem(0, AmmoTransactions.stack(id, 2, 4));
        test.assertTrue(boxStack.getItem().overrideStackedOnOther(boxStack, slot, ClickAction.SECONDARY, player), "configure creative precise box");
        equal(test, box.getAmmoCount(boxStack), Integer.MAX_VALUE, "creative box supply");
        equal(test, slot.getItem().getCount(), 4, "creative box does not consume selector ammo");
        player.getInventory().setItem(1, boxStack);
        equal(test, api.consumeAmmoFromPlayer(3), 3, "extract from creative box");
        api.putAmmoInMagazine(3);
        equal(test, AmmoState.read(api.getItemStack(), api.getAbstractGunItem()).countTier(2), 3, "creative box preserves precision");
        equal(test, box.getAmmoCount(boxStack), Integer.MAX_VALUE, "creative supply remains infinite");
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
        MinecraftForge.EVENT_BUS.addListener(cancel);
        try {
            equal(test, shoot.shoot(() -> 0f, () -> 0f, System.currentTimeMillis() - data.baseTimestamp),
                    com.tacz.guns.api.entity.ShootResult.FORGE_EVENT_CANCEL, "cancel shoot event");
        } finally { MinecraftForge.EVENT_BUS.unregister(cancel); }
        state = AmmoState.read(api.getItemStack(), api.getAbstractGunItem());
        equal(test, state.chamber, 2, "automatic chamber retains precise type");
        equal(test, state.total(), 2, "cancelled shot conserves all rounds");
        test.assertTrue(api.reduceAmmoOnce(), "consume chambered round");
        equal(test, AmmoState.read(api.getItemStack(), api.getAbstractGunItem()).chamber, 1, "next ordinary round");
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
                    equal(test, count, 16, "reload count: " + gunId + " tick " + tick);
                    equal(test, precise, 8, "precise reload count: " + gunId + " tick " + tick);
                }
                test.assertTrue(AmmoState.read(stack, gun).total() > 0 || interruptAt >= 0, "reload should load ammo: " + gunId);
                AmmoTransactions.unload(player, stack, true);
                int total = 0, precise = 0;
                for (var item : player.getInventory().items) if (item.getItem() instanceof com.tacz.guns.api.item.IAmmo) {
                    total += item.getCount();
                    if ((AmmoTransactions.tier(item) == 2)) precise += item.getCount();
                }
                equal(test, total, 16, "unload after script: " + gunId);
                equal(test, precise, 8, "precise unload after script: " + gunId);
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
            equal(test, AmmoState.read(stack, gun).chamber, 2, "inventory precise chamber");
            test.assertTrue(api.reduceAmmoOnce(), "inventory precise shot");
            AmmoTransactions.unload(player, stack, true);
            int remaining = player.getInventory().items.stream().filter(item -> AmmoTransactions.tier(item) == 2).mapToInt(ItemStack::getCount).sum();
            equal(test, remaining, 1, "inventory unload returns chamber only");
        } finally { type.set(reload, old); }
        gun.setDummyAmmoAmount(stack, 7);
        equal(test, api.consumeAmmoFromPlayer(4), 4, "dummy supply");
        api.putAmmoInMagazine(4);
        api.removeAmmoFromMagazine(1);
        api.setAmmoInBarrel(true);
        api.reduceAmmoOnce();
        AmmoTransactions.unload(player, stack, true);
        equal(test, gun.getDummyAmmoAmount(stack), 6, "dummy refund after one shot");
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
        equal(test, AmmoState.read(stack, gun).total(), 0, "barrel flag is not a cartridge");
        var bullets = new ArrayList<EntityKineticBullet>();
        Consumer<EntityJoinLevelEvent> listener = event -> {
            if (event.getEntity() instanceof EntityKineticBullet b && b.getOwner() == player) bullets.add(b);
        };
        MinecraftForge.EVENT_BUS.addListener(listener);
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
                equal(test, state.total(), 1, "RPG reload contains exactly one rocket");
                equal(test, AmmoState.display(stack).nextRound(true, false), tier, "HUD reads magazine for RPG");
                equal(test, AmmoTransactions.peek(api), tier, "non-consuming shot quality");
                api.shootOnce(false); // Creative/infinite shooting must use the same quality as survival/HUD.
                equal(test, AmmoState.read(stack, gun).total(), 1, "non-consuming shot retains rocket");
                if (tier != 3) test.assertTrue(api.reduceAmmoOnce(), "consume ordinary rocket for next reload");
            }
        } finally { MinecraftForge.EVENT_BUS.unregister(listener); }
        equal(test, bullets.size(), 3, "three RPG projectiles");
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
        equal(test, returned, 1, "return only the real precise rocket");
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
        MinecraftForge.EVENT_BUS.addListener(listener);
        try {
            api.shootOnce(true);
            // RPGs have a one-round capacity but this synthetic setup deliberately tests the common shot path.
            if (api.getBolt() == com.tacz.guns.resource.pojo.data.gun.Bolt.MANUAL_ACTION && !api.hasAmmoInBarrel() && api.getAmmoAmount() > 0) { api.removeAmmoFromMagazine(1); api.setAmmoInBarrel(true); }
            api.shootOnce(true);
        } finally { MinecraftForge.EVENT_BUS.unregister(listener); }
        equal(test, bullets.size(), 2, "two actual projectiles: " + gunId);
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
    private static void equal(GameTestHelper test, Object actual, Object expected, String description) {
        test.assertTrue(java.util.Objects.deepEquals(actual, expected), description + ": expected " + expected + ", got " + actual);
    }
    private static void progress(ItemStack stack, ResourceLocation id, int step, int total) {
        var tag = new net.minecraft.nbt.CompoundTag();
        tag.putString("id", id.toString());
        tag.putInt("Step", step);
        tag.putFloat("Progress", (float) step / total);
        stack.getOrCreateTag().put("SequencedAssembly", tag);
    }
    private static ResourceLocation assemblyId(GameTestHelper test, com.simibubi.create.content.processing.recipe.ProcessingRecipe<?> step) {
        return test.getLevel().getRecipeManager().getRecipes().stream()
                .filter(recipe -> recipe instanceof SequencedAssemblyRecipe)
                .map(recipe -> (SequencedAssemblyRecipe) recipe)
                .filter(recipe -> recipe.getSequence().stream().anyMatch(operation -> operation.getRecipe().getId().equals(step.getId())))
                .map(SequencedAssemblyRecipe::getId).findFirst().orElseThrow();
    }
}

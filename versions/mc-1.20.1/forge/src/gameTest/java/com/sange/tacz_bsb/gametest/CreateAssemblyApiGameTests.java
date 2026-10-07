package com.sange.tacz_bsb.gametest;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.sange.tacz_bsb.recipe.AssemblyPairs;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.fluids.spout.FillingBySpout;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.content.kinetics.deployer.BeltDeployerCallbacks;
import com.simibubi.create.content.kinetics.deployer.DeployerBlockEntity;
import com.simibubi.create.content.kinetics.press.MechanicalPressBlockEntity;
import com.simibubi.create.content.kinetics.saw.SawBlock;
import com.simibubi.create.content.kinetics.saw.SawBlockEntity;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipeSerializer;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/** Audits the pinned 1.20.1 machine APIs, including material consumption and recipe networking. */
@GameTestHolder("tacz_bsb")
@PrefixGameTestTemplate(false)
public final class CreateAssemblyApiGameTests {
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void allAssembliesThroughMachineApis(GameTestHelper test) {
        Machines machines = machines(test);
        test.runAfterDelay(2, () -> checkAllAssemblies(test, machines));
    }
    private static void checkAllAssemblies(GameTestHelper test, Machines machines) {
        int chains = 0;
        for (var recipe : assemblies(test)) {
            ItemStack input = recipe.getIngredient().getItems()[0].copyWithCount(1);
            int total = recipe.getLoops() * recipe.getSequence().size();
            for (int step = 0; step < total; step++) {
                input = process(test, machines, input,
                        recipe.getSequence().get(step % recipe.getSequence().size()).getRecipe());
                if (step + 1 < total) {
                    var progress = input.getTag().getCompound("SequencedAssembly");
                    var actualId = ResourceLocation.tryParse(progress.getString("id"));
                    // Identical primer/shell operations may initially select either production family.
                    test.assertTrue(recipe.getId().equals(actualId) || recipe.getId().equals(AssemblyPairs.other(actualId)),
                            "assembly family identity: " + recipe.getId());
                    test.assertTrue(progress.getInt("Step") == step + 1, "cumulative Step, including loops");
                    test.assertTrue(Math.abs(progress.getFloat("Progress") - (step + 1f) / total) < 0.00001f,
                            "fractional assembly progress");
                }
            }
            test.assertTrue(ItemStack.matches(input, recipe.getResultItem(test.getLevel().registryAccess())),
                    "machine output, count and NBT: " + recipe.getId());
            test.assertFalse(input.hasTag() && input.getTag().contains("SequencedAssembly"), "final output retains no assembly state");
            chains++;
        }
        test.assertTrue(chains == 89, "44 BSB assemblies and 45 original Create TaCZ assemblies");
        test.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void bothBranchesThroughRealDeployer(GameTestHelper test) {
        Machines machines = machines(test);
        test.runAfterDelay(2, () -> checkBothBranches(test, machines));
    }
    private static void checkBothBranches(GameTestHelper test, Machines machines) {
        int routes = 0;
        for (var pair : AssemblyPairs.PAIRS.entrySet()) for (boolean high : new boolean[]{false, true}) {
            var sourceId = high ? pair.getKey() : pair.getValue();
            var targetId = high ? pair.getValue() : pair.getKey();
            var source = (SequencedAssemblyRecipe) test.getLevel().getRecipeManager().byKey(sourceId).orElseThrow();
            var target = (SequencedAssemblyRecipe) test.getLevel().getRecipeManager().byKey(targetId).orElseThrow();
            ItemStack input = source.getTransitionalItem().copyWithCount(1);
            var progress = new net.minecraft.nbt.CompoundTag();
            progress.putString("id", sourceId.toString());
            progress.putInt("Step", 1);
            progress.putFloat("Progress", 1f / source.getSequence().size());
            input.getOrCreateTag().put("SequencedAssembly", progress);
            for (int step = 1; step < target.getSequence().size(); step++) {
                input = process(test, machines, input, target.getSequence().get(step).getRecipe());
            }
            test.assertTrue(ItemStack.matches(input, target.getResultItem(test.getLevel().registryAccess())),
                    "real deployer branch result: " + sourceId + " -> " + targetId);
            routes++;
        }
        test.assertTrue(routes == 40, "both directions of all 20 production families");
        test.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void assemblyNetworkRoundTrip(GameTestHelper test) {
        for (var recipe : assemblies(test)) {
            var serializer = (SequencedAssemblyRecipeSerializer) recipe.getSerializer();
            var buffer = new FriendlyByteBuf(Unpooled.buffer());
            try {
                serializer.toNetwork(buffer, recipe);
                var received = serializer.fromNetwork(recipe.getId(), buffer);
                JsonObject before = canonical(recipe), after = canonical(received);
                test.assertTrue(before.equals(after), "recipe networking preserves steps, fluids, loops and AmmoId: " + recipe.getId());
                test.assertTrue(buffer.readableBytes() == 0, "entire recipe packet consumed");
            } finally { buffer.release(); }
        }
        test.succeed();
    }

    private static JsonObject canonical(SequencedAssemblyRecipe recipe) {
        JsonObject json = new JsonObject();
        ((SequencedAssemblyRecipeSerializer) recipe.getSerializer()).write(json, recipe);
        // Vanilla Ingredient packets expand tags and merge duplicate values. Compare
        // their accepted item sets rather than expecting the original JSON syntax.
        json.add("ingredient", acceptedItems(recipe.getIngredient()));
        for (int i = 0; i < recipe.getSequence().size(); i++) {
            var operation = recipe.getSequence().get(i).getRecipe();
            JsonArray ingredients = new JsonArray();
            operation.getIngredients().forEach(ingredient -> ingredients.add(acceptedItems(ingredient)));
            operation.getFluidIngredients().forEach(ingredient -> ingredients.add(ingredient.serialize()));
            json.getAsJsonArray("sequence").get(i).getAsJsonObject().add("ingredients", ingredients);
        }
        return json;
    }

    private static JsonElement acceptedItems(Ingredient ingredient) {
        JsonArray result = new JsonArray();
        java.util.Arrays.stream(ingredient.getItems()).map(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()).toString())
                .distinct().sorted().forEach(result::add);
        return result;
    }

    private static List<SequencedAssemblyRecipe> assemblies(GameTestHelper test) {
        return test.getLevel().getRecipeManager().getRecipes().stream()
                .filter(recipe -> List.of("tacz_bsb", "tacz_c").contains(recipe.getId().getNamespace()))
                .filter(SequencedAssemblyRecipe.class::isInstance).map(SequencedAssemblyRecipe.class::cast).toList();
    }

    private record Machines(DeployerBlockEntity deployer, MechanicalPressBlockEntity press, SawBlockEntity saw) {}
    private static Machines machines(GameTestHelper test) {
        BlockPos deployerPos = new BlockPos(1, 1, 1), pressPos = new BlockPos(2, 1, 1), sawPos = new BlockPos(0, 1, 1);
        test.setBlock(deployerPos, BuiltInRegistries.BLOCK.get(ResourceLocation.tryParse("create:deployer")).defaultBlockState());
        test.setBlock(pressPos, BuiltInRegistries.BLOCK.get(ResourceLocation.tryParse("create:mechanical_press")).defaultBlockState());
        test.setBlock(sawPos, BuiltInRegistries.BLOCK.get(ResourceLocation.tryParse("create:mechanical_saw")).defaultBlockState()
                .setValue(SawBlock.FACING, Direction.UP));
        var deployer = (DeployerBlockEntity) test.getBlockEntity(deployerPos);
        return new Machines(deployer, (MechanicalPressBlockEntity) test.getBlockEntity(pressPos),
                (SawBlockEntity) test.getBlockEntity(sawPos));
    }

    private static ItemStack process(GameTestHelper test, Machines machines, ItemStack input, ProcessingRecipe<?> step) {
        if (step.getType() == AllRecipeTypes.DEPLOYING.getType()) {
            var deployer = machines.deployer();
            deployer.getPlayer().setItemInHand(InteractionHand.MAIN_HAND, step.getIngredients().get(1).getItems()[0].copyWithCount(2));
            var recipe = deployer.getRecipe(input);
            test.assertTrue(recipe != null, "real deployer finds next operation: " + step.getId());
            var transported = new TransportedItemStack(input.copyWithCount(2));
            var captured = new AtomicReference<TransportedItemStackHandlerBehaviour.TransportedResult>();
            var handler = new TransportedItemStackHandlerBehaviour(deployer,
                    (distance, callback) -> captured.set(callback.apply(transported)));
            BeltDeployerCallbacks.activate(transported, handler, deployer, recipe);
            var result = captured.get();
            test.assertTrue(result != null && result.getOutputs().size() == 1, "one processed stack returned to the belt");
            test.assertTrue(result.hasHeldOutput() && ItemStack.matches(result.getHeldOutput().stack, input),
                    "exactly one input processed; second input and its NBT retained");
            test.assertTrue(deployer.getPlayer().getMainHandItem().getCount() == 1, "exactly one held material consumed");
            return result.getOutputs().get(0).stack;
        }
        if (step.getType() == AllRecipeTypes.PRESSING.getType()) {
            var outputs = new ArrayList<ItemStack>();
            var transported = new TransportedItemStack(input.copy());
            test.assertTrue(machines.press().tryProcessOnBelt(transported, outputs, true), "press simulation matches");
            test.assertTrue(outputs.isEmpty(), "press simulation creates no items");
            test.assertTrue(machines.press().tryProcessOnBelt(transported, outputs, false) && outputs.size() == 1,
                    "press executes current assembly step");
            return outputs.get(0);
        }
        if (step.getType() == AllRecipeTypes.FILLING.getType()) {
            FluidStack fluid = step.getFluidIngredients().get(0).getMatchingFluidStacks().get(0).copy();
            fluid.setAmount(200);
            ItemStack batch = input.copyWithCount(2);
            int amount = FillingBySpout.getRequiredAmountForItem(test.getLevel(), batch, fluid);
            test.assertTrue(amount == 100, "spout requires 100 mB");
            var output = FillingBySpout.fillItem(test.getLevel(), amount, batch, fluid);
            test.assertTrue(fluid.getAmount() == 100 && batch.getCount() == 1, "spout consumes one input and 100 mB");
            return output;
        }
        if (step.getType() == AllRecipeTypes.CUTTING.getType()) {
            var saw = machines.saw();
            saw.inventory.clear();
            saw.inventory.setStackInSlot(0, input.copy());
            saw.setSpeed(256);
            saw.start(input);
            test.assertTrue(saw.inventory.recipeDuration == step.getProcessingDuration(), "saw uses assembly duration");
            for (int tick = 0; tick < 100 && !saw.inventory.appliedRecipe; tick++) saw.tick();
            test.assertTrue(saw.inventory.appliedRecipe && saw.inventory.getStackInSlot(0).isEmpty(), "saw consumes input");
            var outputs = new ArrayList<ItemStack>();
            for (int slot = 1; slot < saw.inventory.getSlots(); slot++) {
                if (!saw.inventory.getStackInSlot(slot).isEmpty()) outputs.add(saw.inventory.getStackInSlot(slot).copy());
            }
            test.assertTrue(outputs.size() == 1, "saw returns exactly one processed stack");
            saw.inventory.clear();
            saw.setSpeed(0);
            return outputs.get(0);
        }
        throw new IllegalStateException("Unexpected assembly machine: " + step.getType());
    }
}

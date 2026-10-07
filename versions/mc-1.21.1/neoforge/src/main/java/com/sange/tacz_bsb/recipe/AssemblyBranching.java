package com.sange.tacz_bsb.recipe;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.kinetics.deployer.DeployerApplicationRecipe;
import com.simibubi.create.content.kinetics.deployer.DeployerRecipeSearchEvent;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.wrapper.RecipeWrapper;
import java.util.Optional;

/** Branch only before the first differing ingredient, without reordering or consuming any extra inputs. */
@EventBusSubscriber(modid = "tacz_bsb")
public final class AssemblyBranching {
    @SubscribeEvent
    public static void search(DeployerRecipeSearchEvent event) {
        // Create's matching assembly (100) wins; this only supplies a missing alternative before ordinary recipes (50).
        event.addRecipe(() -> alternate(((net.minecraft.world.level.block.entity.BlockEntity) event.getBlockEntity()).getLevel(), event.getInventory()), 75);
    }
    public static Optional<RecipeHolder<DeployerApplicationRecipe>> alternate(Level level, RecipeWrapper inventory) {
        ItemStack input = inventory.getItem(0);
        var progress = input.get(AllDataComponents.SEQUENCED_ASSEMBLY);
        if (level == null || progress == null) return Optional.empty();
        var targetId = AssemblyPairs.other(progress.id());
        if (targetId == null) return Optional.empty();
        var from = level.getRecipeManager().byKey(progress.id());
        var to = level.getRecipeManager().byKey(targetId);
        if (from.isEmpty() || to.isEmpty()
                || !(from.get().value() instanceof SequencedAssemblyRecipe source)
                || !(to.get().value() instanceof SequencedAssemblyRecipe target)) return Optional.empty();
        int step = progress.step();
        if (!input.is(source.getTransitionalItem().getItem()) || step <= 0
                || step >= source.getSequence().size() || step >= target.getSequence().size()
                || source.getLoops() != target.getLoops()
                || source.getSequence().size() != target.getSequence().size()) return Optional.empty();
        var ops = level.registryAccess().createSerializationContext(JsonOps.INSTANCE);
        if (!net.minecraft.world.item.crafting.Ingredient.CODEC.encodeStart(ops, source.getIngredient()).result()
                .equals(net.minecraft.world.item.crafting.Ingredient.CODEC.encodeStart(ops, target.getIngredient()).result())) return Optional.empty();
        // Compare the current datapack recipes, not an assumed number of primer steps.
        for (int i = 0; i < step; i++) {
            var left = operation(level, source.getSequence().get(i).getRecipe());
            var right = operation(level, target.getSequence().get(i).getRecipe());
            if (left.isEmpty() || !left.equals(right)) return Optional.empty();
        }
        ItemStack virtual = target.getTransitionalItem().copyWithCount(1);
        virtual.set(AllDataComponents.SEQUENCED_ASSEMBLY, new SequencedAssemblyRecipe.SequencedAssembly(
                targetId, step, (float) step / (target.getLoops() * target.getSequence().size())));
        var slots = new ItemStackHandler(2);
        slots.setStackInSlot(0, virtual);
        slots.setStackInSlot(1, inventory.getItem(1));
        return SequencedAssemblyRecipe.getRecipe(level, new RecipeWrapper(slots),
                AllRecipeTypes.DEPLOYING.getType(), DeployerApplicationRecipe.class)
                .filter(holder -> holder.id().equals(targetId));
    }
    private static Optional<JsonElement> operation(Level level, Recipe<?> recipe) {
        return Recipe.CODEC.encodeStart(level.registryAccess().createSerializationContext(JsonOps.INSTANCE), recipe)
                .result().map(encoded -> {
                    var json = encoded.getAsJsonObject().deepCopy();
                    // Transition identity/result differs by tier; actual operations and consumed ingredients must match.
                    json.getAsJsonArray("ingredients").remove(0);
                    json.remove("results");
                    return json;
                });
    }
}

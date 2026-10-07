package com.sange.tacz_bsb.recipe;

import com.google.gson.JsonObject;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.kinetics.deployer.DeployerApplicationRecipe;
import com.simibubi.create.content.kinetics.deployer.DeployerRecipeSearchEvent;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeSerializer;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.wrapper.RecipeWrapper;
import java.util.Optional;

/** Branch only while the consumed prefix is identical; never reorder operations. */
@Mod.EventBusSubscriber(modid = "tacz_bsb")
public final class AssemblyBranching {
    @SubscribeEvent
    public static void search(DeployerRecipeSearchEvent event) {
        event.addRecipe(() -> alternate(((net.minecraft.world.level.block.entity.BlockEntity) event.getBlockEntity()).getLevel(), event.getInventory()), 75);
    }
    public static Optional<DeployerApplicationRecipe> alternate(Level level, RecipeWrapper inventory) {
        ItemStack input = inventory.getItem(0);
        if (level == null || !input.hasTag() || !input.getTag().contains("SequencedAssembly", Tag.TAG_COMPOUND)) return Optional.empty();
        CompoundTag progress = input.getTag().getCompound("SequencedAssembly");
        ResourceLocation fromId = ResourceLocation.tryParse(progress.getString("id"));
        ResourceLocation targetId = AssemblyPairs.other(fromId);
        if (targetId == null) return Optional.empty();
        var from = level.getRecipeManager().byKey(fromId);
        var to = level.getRecipeManager().byKey(targetId);
        if (from.isEmpty() || to.isEmpty()
                || !(from.get() instanceof SequencedAssemblyRecipe source)
                || !(to.get() instanceof SequencedAssemblyRecipe target)) return Optional.empty();
        int step = progress.getInt("Step");
        if (!input.is(source.getTransitionalItem().getItem()) || step <= 0
                || step >= source.getSequence().size() || step >= target.getSequence().size()
                || source.getLoops() != target.getLoops()
                || source.getSequence().size() != target.getSequence().size()) return Optional.empty();
        if (!source.getIngredient().toJson().equals(target.getIngredient().toJson())) return Optional.empty();
        for (int i = 0; i < step; i++) {
            var left = operation(source.getSequence().get(i).getRecipe());
            var right = operation(target.getSequence().get(i).getRecipe());
            if (left.isEmpty() || !left.equals(right)) return Optional.empty();
        }
        ItemStack virtual = target.getTransitionalItem().copyWithCount(1);
        CompoundTag next = progress.copy();
        next.putString("id", targetId.toString());
        virtual.getOrCreateTag().put("SequencedAssembly", next);
        var slots = new ItemStackHandler(2);
        slots.setStackInSlot(0, virtual);
        slots.setStackInSlot(1, inventory.getItem(1));
        return SequencedAssemblyRecipe.getRecipe(level, new RecipeWrapper(slots),
                AllRecipeTypes.DEPLOYING.getType(), DeployerApplicationRecipe.class)
                .filter(recipe -> recipe.getId().equals(target.getSequence().get(step).getRecipe().getId()));
    }
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Optional<JsonObject> operation(ProcessingRecipe<?> recipe) {
        if (!(recipe.getSerializer() instanceof ProcessingRecipeSerializer serializer)) return Optional.empty();
        JsonObject json = new JsonObject();
        serializer.write(json, recipe);
        json.addProperty("type", net.minecraft.core.registries.BuiltInRegistries.RECIPE_SERIALIZER.getKey(serializer).toString());
        json.getAsJsonArray("ingredients").remove(0);
        json.remove("results");
        return Optional.of(json);
    }
}

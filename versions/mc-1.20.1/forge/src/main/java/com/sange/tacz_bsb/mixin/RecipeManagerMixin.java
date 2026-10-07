package com.sange.tacz_bsb.mixin;

import com.google.gson.JsonElement;
import com.sange.tacz_bsb.BsbConfig;
import com.sange.tacz_bsb.recipe.RecipeRewriter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.crafting.RecipeManager;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import org.spongepowered.asm.mixin.Mixin;
import java.util.HashMap;
import java.util.Map;

@Mixin(RecipeManager.class)
public abstract class RecipeManagerMixin {
    @WrapMethod(method = "apply")
    private void bsb$recipes(Map<ResourceLocation, JsonElement> recipes, ResourceManager resources,
                             ProfilerFiller profiler, Operation<Void> original) {
        Map<ResourceLocation, JsonElement> result = new HashMap<>(recipes);
        result.replaceAll((id, json) -> BsbConfig.value(BsbConfig.RECIPES).contains(id.getNamespace())
                ? RecipeRewriter.rewrite(json, ammoId -> BsbConfig.enabled(ammoId, 2)) : json);
        original.call(result, resources, profiler);
    }
}

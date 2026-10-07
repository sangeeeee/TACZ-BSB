package com.sange.tacz_bsb.gametest;

import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import com.tacz.guns.api.TimelessAPI;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/** Checks the real prerequisite mods, registries and production recipes at launch. */
@GameTestHolder("tacz_bsb")
@PrefixGameTestTemplate(false)
public final class DependencyGameTests {
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void prerequisiteModsAndRecipes(GameTestHelper test) {
        for (String id : List.of("tacz_bsb", "tacz", "tacz_c", "create", "ponder")) {
            test.assertTrue(ModList.get().isLoaded(id), "Required mod loaded: " + id);
        }
        for (String item : List.of("tacz:ammo", "tacz_c:gunpowder_cake", "create:rose_quartz")) {
            test.assertTrue(BuiltInRegistries.ITEM.containsKey(ResourceLocation.tryParse(item)), "Required item registered: " + item);
        }
        test.assertTrue(!TimelessAPI.getAllCommonAmmoIndex().isEmpty(), "TaCZ ammunition definitions loaded");
        test.assertTrue(!TimelessAPI.getAllCommonGunIndex().isEmpty(), "TaCZ gun definitions loaded");
        long assemblyRecipes = test.getLevel().getRecipeManager().getRecipes().stream()
                .filter(recipe -> recipe.getId().getNamespace().equals("tacz_c"))
                .filter(recipe -> recipe instanceof SequencedAssemblyRecipe)
                .count();
        test.assertTrue(assemblyRecipes > 0, "Create: TaCZ sequenced assembly recipes parsed");
        test.succeed();
    }
}

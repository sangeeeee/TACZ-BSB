package com.sange.tacz_bsb.gametest;

import com.sange.tacz_bsb.client.AssemblyVisibility;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

/** Development-only test of the actual JEI search/visibility APIs. */
@JeiPlugin
public final class JeiSmokeTest implements IModPlugin {
    public static boolean passed;
    @Override public ResourceLocation getPluginUid() { return ResourceLocation.parse("tacz_bsb:jei_smoke"); }
    @Override public void onRuntimeAvailable(IJeiRuntime runtime) {
        if (!Boolean.getBoolean("tacz_bsb.clientSmoke")) return;
        var visibility = runtime.getJeiHelpers().getIngredientVisibility();
        int hidden = 0;
        for (var item : BuiltInRegistries.ITEM) {
            var stack = item.getDefaultInstance();
            if (!AssemblyVisibility.hidden(stack)) continue;
            hidden++;
            if (visibility.isIngredientVisible(VanillaTypes.ITEM_STACK, stack))
                throw new IllegalStateException("JEI exposes an unfinished item: " + BuiltInRegistries.ITEM.getKey(item));
        }
        var filter = runtime.getIngredientFilter();
        var previous = filter.getFilterText();
        try {
            for (String query : new String[]{"", "unfinished", "加工中", "@tacz_bsb"}) {
                filter.setFilterText(query);
                if (filter.getFilteredIngredients(VanillaTypes.ITEM_STACK).stream().anyMatch(AssemblyVisibility::hidden))
                    throw new IllegalStateException("Unfinished item found by JEI query: " + query);
            }
        } finally { filter.setFilterText(previous); }
        passed = true;
        System.out.println("BSB_JEI_VISIBILITY_PASSED: " + hidden + " unfinished items excluded from search");
    }
}

package com.sange.tacz_bsb.recipe;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RecipeRewriterTest {
    private static final String RECIPE = """
        {"type":"create:sequenced_assembly","loops":3,"ingredient":{"item":"tacz_c:casefull_9mm"},
         "transitionalItem":{"item":"tacz_c:unfinished_9mm"},
         "sequence":[{"type":"create:pressing","results":[{"item":"tacz_c:unfinished_9mm"}]}],
         "results":[{"item":"tacz:ammo","count":12,"chance":0.7,
         "nbt":{"AmmoId":"pack:new_round"}},{"item":"minecraft:iron_nugget","chance":0.3}]}
        """;
    @Test void onlyFinalAmmoItemChangesAndReloadIsIdempotent() {
        var source = JsonParser.parseString(RECIPE);
        var transformed = RecipeRewriter.rewrite(source, id -> id.equals("pack:new_round"));
        assertNotSame(source, transformed);
        assertEquals(source, JsonParser.parseString(RECIPE));
        var restored = transformed.deepCopy();
        restored.getAsJsonObject().getAsJsonArray("results").get(0).getAsJsonObject().addProperty("item", "tacz:ammo");
        assertEquals(source, restored);
        assertSame(transformed, RecipeRewriter.rewrite(transformed, id -> true));
    }
    @Test void disabledAmmoAndWorkbenchRecipesRemainUnchanged() {
        var source = JsonParser.parseString(RECIPE);
        assertSame(source, RecipeRewriter.rewrite(source, id -> false));
        source.getAsJsonObject().addProperty("type", "tacz:gun_smith_table_crafting");
        assertSame(source, RecipeRewriter.rewrite(source, id -> true));
    }
}

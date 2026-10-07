package com.sange.tacz_bsb.recipe;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.function.Predicate;

public final class RecipeRewriter {
    private RecipeRewriter() {}
    /** Returns an independent recipe tree. All processing steps and probabilities are preserved. */
    public static JsonElement rewrite(JsonElement input, Predicate<String> enabled) {
        if (!input.isJsonObject()) return input;
        JsonObject recipe = input.getAsJsonObject();
        if (!recipe.has("type") || !"create:sequenced_assembly".equals(recipe.get("type").getAsString())
                || !recipe.has("results") || !recipe.get("results").isJsonArray()) return input;
        JsonObject copy = recipe.deepCopy();
        boolean changed = false;
        for (JsonElement result : copy.getAsJsonArray("results")) {
            if (!result.isJsonObject()) continue;
            JsonObject output = result.getAsJsonObject();
            if (!output.has("item") || !"tacz:ammo".equals(output.get("item").getAsString())) continue;
            JsonElement nbt = output.get("nbt");
            if (nbt == null || !nbt.isJsonObject() || !nbt.getAsJsonObject().has("AmmoId")) continue;
            String ammo = nbt.getAsJsonObject().get("AmmoId").getAsString();
            if (!enabled.test(ammo)) continue;
            output.addProperty("item", "tacz_bsb:improved_ammo");
            changed = true;
        }
        return changed ? copy : input;
    }
}

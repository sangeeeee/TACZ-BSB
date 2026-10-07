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
            if (!output.has("id") || !"tacz:ammo".equals(output.get("id").getAsString())) continue;
            JsonObject components = output.getAsJsonObject("components");
            if (components == null || !components.has("minecraft:custom_data")) continue;
            JsonElement custom = components.get("minecraft:custom_data");
            // The official port uses an object. Unknown component formats remain untouched.
            if (!custom.isJsonObject() || !custom.getAsJsonObject().has("AmmoId")) continue;
            String ammo = custom.getAsJsonObject().get("AmmoId").getAsString();
            if (!enabled.test(ammo)) continue;
            output.addProperty("id", "tacz_bsb:improved_ammo");
            changed = true;
        }
        return changed ? copy : input;
    }
}


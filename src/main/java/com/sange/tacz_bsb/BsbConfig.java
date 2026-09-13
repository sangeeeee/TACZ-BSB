package com.sange.tacz_bsb;

import net.neoforged.neoforge.common.ModConfigSpec;
import java.util.List;

public final class BsbConfig {
    public static final ModConfigSpec SPEC, COMMON_SPEC;
    public static final ModConfigSpec.DoubleValue DIRECT, EXPLOSION;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> AMMO, RECIPES, FIFO, OVERRIDES;
    public static final ModConfigSpec.IntValue HUD_X, HUD_Y;
    static {
        var b = new ModConfigSpec.Builder();
        DIRECT = b.comment("Precise direct-hit multiplier, composed with the gun's own multiplier.")
                .defineInRange("directDamageMultiplier", 1.5, 0, 100);
        EXPLOSION = b.comment("Precise explosion damage multiplier. Radius and knockback are unchanged.")
                .defineInRange("explosionDamageMultiplier", 1.5, 0, 100);
        var recipes = new ModConfigSpec.Builder();
        AMMO = recipes.comment("Enabled AmmoIds. '*' enables all valid TaCZ ammo, including gun packs. namespace:* is supported.")
                .defineListAllowEmpty("enabledAmmo", List.of("*"), () -> "*", x -> x instanceof String);
        RECIPES = recipes.comment("Namespaces whose Create sequenced assembly ammo outputs are replaced. Run /reload after edits.")
                .defineListAllowEmpty("recipeNamespaces", List.of("tacz_c"), () -> "tacz_c", x -> x instanceof String);
        COMMON_SPEC = recipes.build();
        FIFO = b.comment("AmmoIds loaded behind existing magazine rounds (tube-style). Other ammo loads at the front.")
                .defineListAllowEmpty("fifoAmmo", List.of("tacz:12g"), () -> "tacz:12g", x -> x instanceof String);
        OVERRIDES = b.comment("Per-ammo overrides: namespace:ammo=directMultiplier,explosionMultiplier")
                .defineListAllowEmpty("damageOverrides", List.of(), () -> "pack:ammo=1.5,1.5", BsbConfig::validOverride);
        HUD_X = b.defineInRange("hudOffsetX", 0, -2000, 2000);
        HUD_Y = b.defineInRange("hudOffsetY", 0, -2000, 2000);
        SPEC = b.build();
    }
    private static boolean validOverride(Object value) {
        if (!(value instanceof String s)) return false;
        try {
            String[] kv = s.split("=", 2), numbers = kv[1].split(",");
            return kv[0].contains(":") && numbers.length == 2 && validMultiplier(numbers[0]) && validMultiplier(numbers[1]);
        } catch (RuntimeException e) { return false; }
    }
    private static boolean validMultiplier(String value) {
        double n = Double.parseDouble(value);
        return Double.isFinite(n) && n >= 0 && n <= 100;
    }
    public static <T> T value(ModConfigSpec.ConfigValue<T> value) {
        boolean loaded = (value == AMMO || value == RECIPES) ? COMMON_SPEC.isLoaded() : SPEC.isLoaded();
        return loaded ? value.get() : value.getDefault();
    }
    public static boolean matches(List<? extends String> entries, String id) {
        return entries.stream().anyMatch(s -> s.equals("*") || s.equals(id)
                || (s.endsWith(":*") && id.startsWith(s.substring(0, s.length() - 1))));
    }
    public static boolean enabled(String id) { return matches(value(AMMO), id); }
    public static boolean fifo(String id) { return matches(value(FIFO), id); }
    public static float multiplier(String id, boolean explosion) {
        for (String entry : value(OVERRIDES)) {
            String[] kv = entry.split("=", 2);
            if (kv[0].equals(id)) return Float.parseFloat(kv[1].split(",")[explosion ? 1 : 0]);
        }
        return value(explosion ? EXPLOSION : DIRECT).floatValue();
    }
}


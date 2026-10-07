package com.sange.tacz_bsb;

import net.neoforged.neoforge.common.ModConfigSpec;
import java.util.List;

public final class BsbConfig {
    public static final ModConfigSpec SPEC, COMMON_SPEC;
    public static final ModConfigSpec.DoubleValue DIRECT, EXPLOSION, PRECISE_DIRECT, PRECISE_EXPLOSION;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> AMMO, PRECISE_AMMO, RECIPES, FIFO, OVERRIDES, PRECISE_OVERRIDES;
    public static final ModConfigSpec.IntValue HUD_X, HUD_Y;
    static {
        var b = new ModConfigSpec.Builder();
        DIRECT = b.comment("Improved direct-hit multiplier, composed with the gun's own multiplier.")
                .defineInRange("improvedDirectDamageMultiplier", 1.5, 0, 100);
        EXPLOSION = b.comment("Improved explosion damage multiplier. Radius and knockback are unchanged.")
                .defineInRange("improvedExplosionDamageMultiplier", 1.5, 0, 100);
        PRECISE_DIRECT = b.defineInRange("preciseDirectDamageMultiplier", 2.0, 0, 100);
        PRECISE_EXPLOSION = b.defineInRange("preciseExplosionDamageMultiplier", 2.0, 0, 100);
        var recipes = new ModConfigSpec.Builder();
        AMMO = recipes.comment("Enabled AmmoIds. '*' enables all valid TaCZ ammo, including gun packs. namespace:* is supported.")
                .defineListAllowEmpty("enabledImprovedAmmo", List.of("*"), () -> "*", x -> x instanceof String);
        PRECISE_AMMO = recipes.comment("AmmoIds with precise variants. Wildcards work as for improved ammunition.")
                .defineListAllowEmpty("enabledPreciseAmmo", List.of("*"), () -> "*", x -> x instanceof String);
        RECIPES = recipes.comment("Namespaces whose Create sequenced assembly ammo outputs are replaced. Run /reload after edits.")
                .defineListAllowEmpty("recipeNamespaces", List.of("tacz_c"), () -> "tacz_c", x -> x instanceof String);
        COMMON_SPEC = recipes.build();
        FIFO = b.comment("AmmoIds loaded behind existing magazine rounds (tube-style). Other ammo loads at the front.")
                .defineListAllowEmpty("fifoAmmo", List.of("tacz:12g"), () -> "tacz:12g", x -> x instanceof String);
        OVERRIDES = b.comment("Per-ammo overrides: namespace:ammo=directMultiplier,explosionMultiplier")
                .defineListAllowEmpty("improvedDamageOverrides", List.of(), () -> "pack:ammo=1.5,1.5", BsbConfig::validOverride);
        PRECISE_OVERRIDES = b.comment("Precise per-ammo overrides: namespace:ammo=directMultiplier,explosionMultiplier")
                .defineListAllowEmpty("preciseDamageOverrides", List.of(), () -> "pack:ammo=2.0,2.0", BsbConfig::validOverride);
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
        boolean loaded = (value == AMMO || value == PRECISE_AMMO || value == RECIPES) ? COMMON_SPEC.isLoaded() : SPEC.isLoaded();
        return loaded ? value.get() : value.getDefault();
    }
    public static boolean matches(List<? extends String> entries, String id) {
        return entries.stream().anyMatch(s -> s.equals("*") || s.equals(id)
                || (s.endsWith(":*") && id.startsWith(s.substring(0, s.length() - 1))));
    }
    public static boolean enabled(String id, int tier) { return matches(value(tier == 3 ? PRECISE_AMMO : AMMO), id); }
    public static boolean fifo(String id) { return matches(value(FIFO), id); }
    public static float multiplier(String id, int tier, boolean explosion) {
        if (tier == 1) return 1;
        for (String entry : value(tier == 3 ? PRECISE_OVERRIDES : OVERRIDES)) {
            String[] kv = entry.split("=", 2);
            if (kv[0].equals(id)) return Float.parseFloat(kv[1].split(",")[explosion ? 1 : 0]);
        }
        return value(tier == 3 ? (explosion ? PRECISE_EXPLOSION : PRECISE_DIRECT) : (explosion ? EXPLOSION : DIRECT)).floatValue();
    }
}


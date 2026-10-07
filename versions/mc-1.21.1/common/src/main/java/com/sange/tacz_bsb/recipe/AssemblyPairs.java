package com.sange.tacz_bsb.recipe;

import net.minecraft.resources.ResourceLocation;
import java.util.Map;

/** Generated default production families; unrelated datapack recipes are never interchangeable. */
public final class AssemblyPairs {
    public static final Map<ResourceLocation, ResourceLocation> PAIRS = Map.ofEntries(
            Map.entry(ResourceLocation.parse("tacz_c:bullet_12g"), ResourceLocation.parse("tacz_bsb:high_bullet_12g")),
            Map.entry(ResourceLocation.parse("tacz_c:bullet_22wmr"), ResourceLocation.parse("tacz_bsb:high_bullet_22wmr")),
            Map.entry(ResourceLocation.parse("tacz_c:bullet_3006"), ResourceLocation.parse("tacz_bsb:high_bullet_3006")),
            Map.entry(ResourceLocation.parse("tacz_c:bullet_308"), ResourceLocation.parse("tacz_bsb:high_bullet_308")),
            Map.entry(ResourceLocation.parse("tacz_c:bullet_338"), ResourceLocation.parse("tacz_bsb:high_bullet_338")),
            Map.entry(ResourceLocation.parse("tacz_c:bullet_357mag"), ResourceLocation.parse("tacz_bsb:high_bullet_357mag")),
            Map.entry(ResourceLocation.parse("tacz_c:bullet_4570"), ResourceLocation.parse("tacz_bsb:high_bullet_4570")),
            Map.entry(ResourceLocation.parse("tacz_c:bullet_45acp"), ResourceLocation.parse("tacz_bsb:high_bullet_45acp")),
            Map.entry(ResourceLocation.parse("tacz_c:bullet_4630"), ResourceLocation.parse("tacz_bsb:high_bullet_4630")),
            Map.entry(ResourceLocation.parse("tacz_c:bullet_500mag"), ResourceLocation.parse("tacz_bsb:high_bullet_500mag")),
            Map.entry(ResourceLocation.parse("tacz_c:bullet_50ae"), ResourceLocation.parse("tacz_bsb:high_bullet_50ae")),
            Map.entry(ResourceLocation.parse("tacz_c:bullet_50bmg"), ResourceLocation.parse("tacz_bsb:high_bullet_50bmg")),
            Map.entry(ResourceLocation.parse("tacz_c:bullet_54539"), ResourceLocation.parse("tacz_bsb:high_bullet_54539")),
            Map.entry(ResourceLocation.parse("tacz_c:bullet_55645"), ResourceLocation.parse("tacz_bsb:high_bullet_55645")),
            Map.entry(ResourceLocation.parse("tacz_c:bullet_5728"), ResourceLocation.parse("tacz_bsb:high_bullet_5728")),
            Map.entry(ResourceLocation.parse("tacz_c:bullet_5842"), ResourceLocation.parse("tacz_bsb:high_bullet_5842")),
            Map.entry(ResourceLocation.parse("tacz_c:bullet_6851fury"), ResourceLocation.parse("tacz_bsb:high_bullet_6851fury")),
            Map.entry(ResourceLocation.parse("tacz_c:bullet_76225"), ResourceLocation.parse("tacz_bsb:high_bullet_76225")),
            Map.entry(ResourceLocation.parse("tacz_c:bullet_76239"), ResourceLocation.parse("tacz_bsb:high_bullet_76239")),
            Map.entry(ResourceLocation.parse("tacz_c:bullet_76254"), ResourceLocation.parse("tacz_bsb:high_bullet_76254")),
            Map.entry(ResourceLocation.parse("tacz_c:bullet_792x57"), ResourceLocation.parse("tacz_bsb:high_bullet_792x57")),
            Map.entry(ResourceLocation.parse("tacz_c:bullet_9mm"), ResourceLocation.parse("tacz_bsb:high_bullet_9mm")),
            Map.entry(ResourceLocation.parse("tacz_c:grenade_booster_charge_40mm"), ResourceLocation.parse("tacz_bsb:precise_grenade_booster_charge_40mm")));
    public static ResourceLocation other(ResourceLocation id) {
        ResourceLocation direct = PAIRS.get(id);
        if (direct != null) return direct;
        for (var entry : PAIRS.entrySet()) if (entry.getValue().equals(id)) return entry.getKey();
        return null;
    }
}

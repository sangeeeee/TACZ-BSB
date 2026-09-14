package com.sange.tacz_bsb.compat;

import com.sange.tacz_bsb.BsbContent;
import com.tacz.guns.compat.jei.GunModSubtype;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.resources.ResourceLocation;

/** Optional JEI integration: each AmmoId is a distinct searchable recipe ingredient. */
@JeiPlugin
public final class BsbJeiPlugin implements IModPlugin {
    @Override public ResourceLocation getPluginUid() { return ResourceLocation.parse("tacz_bsb:jei"); }
    @Override public void registerItemSubtypes(ISubtypeRegistration registration) {
        registration.registerSubtypeInterpreter(BsbContent.IMPROVED_AMMO.get(), GunModSubtype.getAmmoSubtype());
        registration.registerSubtypeInterpreter(BsbContent.PRECISE_AMMO.get(), GunModSubtype.getAmmoSubtype());
    }
}

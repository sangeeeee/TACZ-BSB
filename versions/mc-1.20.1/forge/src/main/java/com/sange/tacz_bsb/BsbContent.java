package com.sange.tacz_bsb;

import com.sange.tacz_bsb.item.TieredAmmoItem;
import com.sange.tacz_bsb.item.UniversalAmmoBoxItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class BsbContent {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "tacz_bsb");
    public static final RegistryObject<TieredAmmoItem> PRECISE_AMMO = ITEMS.register("precise_ammo", () -> new TieredAmmoItem(3));
    public static final RegistryObject<TieredAmmoItem> IMPROVED_AMMO = ITEMS.register("improved_ammo", () -> new TieredAmmoItem(2));
    public static final RegistryObject<UniversalAmmoBoxItem> IMPROVED_UNIVERSAL_AMMO_BOX =
            ITEMS.register("improved_universal_ammo_box", () -> new UniversalAmmoBoxItem(2));
    public static final RegistryObject<UniversalAmmoBoxItem> PRECISE_UNIVERSAL_AMMO_BOX =
            ITEMS.register("precise_universal_ammo_box", () -> new UniversalAmmoBoxItem(3));
    public static final String AMMO_STATE = "tacz_bsb:ammo_state";
    public static final String BOX_TIER = "tacz_bsb:box_tier";
    public static void register(IEventBus bus) { BsbMaterials.initialize(); ITEMS.register(bus); }
}

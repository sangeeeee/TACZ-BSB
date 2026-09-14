package com.sange.tacz_bsb;

import com.sange.tacz_bsb.item.TieredAmmoItem;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

public final class BsbContent {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems("tacz_bsb");
    public static final DeferredItem<TieredAmmoItem> PRECISE_AMMO = ITEMS.register("precise_ammo", () -> new TieredAmmoItem(3));
    public static final DeferredItem<TieredAmmoItem> IMPROVED_AMMO = ITEMS.register("improved_ammo", () -> new TieredAmmoItem(2));
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, "tacz_bsb");
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CustomData>> AMMO_STATE =
            COMPONENTS.register("ammo_state", () -> DataComponentType.<CustomData>builder()
                    .persistent(CustomData.CODEC).networkSynchronized(ByteBufCodecs.fromCodec(CustomData.CODEC)).build());
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> BOX_TIER =
            COMPONENTS.register("box_tier", () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.intRange(1, 3)).networkSynchronized(ByteBufCodecs.VAR_INT).build());
    public static void register(IEventBus bus) { BsbMaterials.initialize(); ITEMS.register(bus); COMPONENTS.register(bus); }
}


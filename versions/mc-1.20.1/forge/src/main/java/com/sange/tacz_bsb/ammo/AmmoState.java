package com.sange.tacz_bsb.ammo;

import com.sange.tacz_bsb.BsbContent;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.resource.pojo.data.gun.Bolt;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.Tag;
import java.util.Arrays;

public final class AmmoState {
    private AmmoState() {}
    private static boolean openBolt(ItemStack stack, IGun gun) {
        return TimelessAPI.getCommonGunIndex(gun.getGunId(stack))
                .map(index -> index.getGunData().getBolt() == Bolt.OPEN_BOLT).orElse(false);
    }
    public static AmmoLedger read(ItemStack stack, IGun gun) {
        CompoundTag data = data(stack);
        int actual = gun.useInventoryAmmo(stack) ? 0 : gun.getCurrentAmmoCount(stack);
        boolean open = openBolt(stack, gun);
        // TaCZ creative-tab guns set this flag even on open-bolt weapons. It is not an extra cartridge.
        boolean chamber = !open && gun.hasBulletInBarrel(stack);
        if (data == null) return AmmoLedger.ordinary(actual, chamber);
        CompoundTag tag = data.copy();
        String recordedAmmo = tag.getString("ammo_id");
        var currentAmmo = AmmoTransactions.ammoId(stack);
        AmmoLedger state = new AmmoLedger(queue(tag, "magazine"), queue(tag, "reserve"),
                queue(tag, "transfer"), tag.getInt("chamber"));
        if (state.total() > 0 && currentAmmo != null && !recordedAmmo.equals(currentAmmo.toString())) {
            throw new IllegalStateException("The gun's caliber changed while ammunition was loaded: " + recordedAmmo);
        }
        if (state.magazine.size() != actual || (state.chamber != 0) != chamber) {
            throw new IllegalStateException("tacz_bsb ammunition state was modified outside the supported TaCZ API: "
                    + gun.getGunId(stack) + ". Refusing to invent or discard rounds.");
        }
        return state;
    }
    public static AmmoLedger display(ItemStack stack) {
        CompoundTag data = data(stack);
        if (data == null) return null;
        CompoundTag tag = data.copy();
        return new AmmoLedger(queue(tag, "magazine"), queue(tag, "reserve"), queue(tag, "transfer"), tag.getInt("chamber"));
    }
    private static RoundQueue queue(CompoundTag tag, String key) {
        return RoundQueue.fromRuns(Arrays.stream(tag.getIntArray(key)).boxed().toList());
    }
    private static CompoundTag data(ItemStack stack) {
        if (!stack.hasTag() || !stack.getTag().contains(BsbContent.AMMO_STATE)) return null;
        if (!stack.getTag().contains(BsbContent.AMMO_STATE, Tag.TAG_COMPOUND)) {
            throw new IllegalStateException("Invalid ammunition state tag");
        }
        CompoundTag data = stack.getTag().getCompound(BsbContent.AMMO_STATE);
        if (!data.contains("ammo_id", Tag.TAG_STRING) || !data.contains("chamber", Tag.TAG_INT)
                || !data.contains("magazine", Tag.TAG_INT_ARRAY) || !data.contains("reserve", Tag.TAG_INT_ARRAY)
                || !data.contains("transfer", Tag.TAG_INT_ARRAY)) {
            throw new IllegalStateException("Incomplete ammunition state; refusing to discard recorded rounds");
        }
        return data;
    }
    public static void write(ItemStack stack, IGun gun, AmmoLedger state) {
        if (openBolt(stack, gun) && state.chamber != 0) {
            throw new IllegalStateException("An open-bolt gun cannot store a separate chambered round");
        }
        CompoundTag tag = new CompoundTag();
        var ammo = AmmoTransactions.ammoId(stack);
        if (ammo != null) tag.putString("ammo_id", ammo.toString());
        tag.putIntArray("magazine", state.magazine.runs());
        tag.putIntArray("reserve", state.reserve.runs());
        tag.putIntArray("transfer", state.transfer.runs());
        tag.putInt("chamber", state.chamber);
        if (!gun.useInventoryAmmo(stack)) gun.setCurrentAmmoCount(stack, state.magazine.size());
        gun.setBulletInBarrel(stack, state.chamber != 0);
        stack.getOrCreateTag().put(BsbContent.AMMO_STATE, tag);
    }
}

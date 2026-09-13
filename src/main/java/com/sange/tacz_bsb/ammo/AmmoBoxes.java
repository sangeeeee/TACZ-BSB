package com.sange.tacz_bsb.ammo;

import com.sange.tacz_bsb.BsbContent;
import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.api.item.IAmmoBox;
import com.tacz.guns.config.sync.SyncConfig;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.item.ItemStack;

public final class AmmoBoxes {
    public static boolean click(ItemStack boxStack, Slot slot, ClickAction action, Player player) {
        if (action != ClickAction.SECONDARY) return false;
        IAmmoBox box = (IAmmoBox) boxStack.getItem();
        ItemStack candidate = slot.getItem();
        if (candidate.isEmpty()) {
            if (box.isCreative(boxStack) || box.isAllTypeCreative(boxStack) || box.getAmmoCount(boxStack) <= 0) return false;
            var index = TimelessAPI.getCommonAmmoIndex(box.getAmmoId(boxStack));
            if (index.isEmpty()) return false;
            int requested = Math.min(index.get().getStackSize(), box.getAmmoCount(boxStack));
            ItemStack result = AmmoTransactions.stack(box.getAmmoId(boxStack), AmmoTransactions.preciseBox(boxStack), requested);
            int accepted = requested - slot.safeInsert(result).getCount();
            if (accepted <= 0) return false;
            box.setAmmoCount(boxStack, box.getAmmoCount(boxStack) - accepted);
            if (box.getAmmoCount(boxStack) == 0) {
                box.setAmmoId(boxStack, DefaultAssets.EMPTY_AMMO_ID);
                boxStack.remove(BsbContent.PRECISE_BOX.get());
            }
            player.playSound(SoundEvents.BUNDLE_REMOVE_ONE, 0.8f, 1);
            return true;
        }
        if (!(candidate.getItem() instanceof IAmmo ammo) || box.isAllTypeCreative(boxStack)) return false;
        var id = ammo.getAmmoId(candidate);
        var index = TimelessAPI.getCommonAmmoIndex(id);
        if (index.isEmpty()) return false;
        boolean precise = AmmoTransactions.precise(candidate);
        boolean empty = box.getAmmoId(boxStack).equals(DefaultAssets.EMPTY_AMMO_ID) || box.getAmmoCount(boxStack) <= 0;
        if (!empty && (!box.getAmmoId(boxStack).equals(id) || AmmoTransactions.preciseBox(boxStack) != precise)) return false;
        if (box.isCreative(boxStack)) {
            box.setAmmoId(boxStack, id);
            box.setAmmoCount(boxStack, Integer.MAX_VALUE);
            boxStack.set(BsbContent.PRECISE_BOX.get(), precise);
            return true;
        }
        long capacity = (long) index.get().getStackSize() * SyncConfig.AMMO_BOX_STACK_SIZE.get() * (box.getAmmoLevel(boxStack) + 1);
        int available = (int) Math.max(0, Math.min(Integer.MAX_VALUE, capacity - Math.max(0, box.getAmmoCount(boxStack))));
        ItemStack taken = slot.safeTake(candidate.getCount(), available, player);
        if (taken.isEmpty()) return false;
        int old = empty ? 0 : box.getAmmoCount(boxStack);
        box.setAmmoId(boxStack, id);
        boxStack.set(BsbContent.PRECISE_BOX.get(), precise);
        box.setAmmoCount(boxStack, old + taken.getCount());
        player.playSound(SoundEvents.BUNDLE_INSERT, 0.8f, 1);
        return true;
    }
}



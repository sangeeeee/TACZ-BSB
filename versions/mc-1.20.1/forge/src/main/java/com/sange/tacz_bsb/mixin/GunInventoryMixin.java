package com.sange.tacz_bsb.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.sange.tacz_bsb.ammo.AmmoTransactions;
import com.tacz.guns.api.item.gun.AbstractGunItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = AbstractGunItem.class, remap = false)
public abstract class GunInventoryMixin {
    @WrapMethod(method = "findAndExtractInventoryAmmo")
    private int bsb$extract(IItemHandler inventory, ItemStack stack, int requested, Operation<Integer> original) {
        if (AmmoTransactions.ammoId(stack) == null) return original.call(inventory, stack, requested);
        return AmmoTransactions.extract(inventory, stack, requested);
    }
    @WrapMethod(method = "dropAllAmmo")
    private void bsb$unload(Player player, ItemStack stack, Operation<Void> original) {
        if (AmmoTransactions.ammoId(stack) == null) original.call(player, stack);
        else AmmoTransactions.unload(player, stack, false);
    }
}

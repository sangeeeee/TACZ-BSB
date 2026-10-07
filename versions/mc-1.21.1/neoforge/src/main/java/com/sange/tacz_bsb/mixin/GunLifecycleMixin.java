package com.sange.tacz_bsb.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.sange.tacz_bsb.ammo.AmmoTransactions;
import com.tacz.guns.api.entity.ReloadState;
import com.tacz.guns.entity.shooter.ShooterDataHolder;
import com.tacz.guns.item.ModernKineticGunItem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;

/** Settle the inventory-to-magazine transaction after each synchronous script callback. */
@Mixin(value = ModernKineticGunItem.class, remap = false)
public abstract class GunLifecycleMixin {
    @WrapMethod(method = {"startReload", "startBolt", "tickBolt"})
    private boolean bsb$booleanCallback(ShooterDataHolder data, ItemStack gun, LivingEntity owner, Operation<Boolean> original) {
        try { return original.call(data, gun, owner); }
        finally { AmmoTransactions.returnPending(owner, gun); }
    }
    @WrapMethod(method = "tickReload")
    private ReloadState bsb$reloadCallback(ShooterDataHolder data, ItemStack gun, LivingEntity owner, Operation<ReloadState> original) {
        try { return original.call(data, gun, owner); }
        finally { AmmoTransactions.returnPending(owner, gun); }
    }
    @WrapMethod(method = "interruptReload")
    private void bsb$interrupt(ShooterDataHolder data, ItemStack gun, LivingEntity owner, Operation<Void> original) {
        try { original.call(data, gun, owner); }
        finally { AmmoTransactions.returnPending(owner, gun); }
    }
}


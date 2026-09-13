package com.sange.tacz_bsb.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.sange.tacz_bsb.ammo.AmmoState;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.gun.AbstractGunItem;
import com.tacz.guns.entity.shooter.LivingEntityShoot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;

/** TaCZ's automatic chambering happens before the script's shoot callback. */
@Mixin(value = LivingEntityShoot.class, remap = false)
public abstract class ShootChamberMixin {
    @Shadow @Final private LivingEntity shooter;

    @WrapOperation(method = "shoot(Ljava/util/function/Supplier;Ljava/util/function/Supplier;JFZ)Lcom/tacz/guns/api/entity/ShootResult;", at = @At(value = "INVOKE",
            target = "Lcom/tacz/guns/api/item/IGun;reduceCurrentAmmoCount(Lnet/minecraft/world/item/ItemStack;)V"))
    private void bsb$transfer(IGun gun, ItemStack stack, Operation<Void> original) {
        var state = AmmoState.read(stack, gun);
        state.removeForTransfer(1);
        AmmoState.write(stack, gun, state);
    }
    @WrapOperation(method = "shoot(Ljava/util/function/Supplier;Ljava/util/function/Supplier;JFZ)Lcom/tacz/guns/api/entity/ShootResult;", at = @At(value = "INVOKE",
            target = "Lcom/tacz/guns/api/item/IGun;setBulletInBarrel(Lnet/minecraft/world/item/ItemStack;Z)V"))
    private void bsb$chamber(IGun gun, ItemStack stack, boolean present, Operation<Void> original) {
        var state = AmmoState.read(stack, gun);
        state.chamber(present, !IGunOperator.fromLivingEntity(shooter).needCheckAmmo());
        AmmoState.write(stack, gun, state);
    }
    @WrapMethod(method = "consumeAmmoFromPlayer")
    private void bsb$inventory(int count, ItemStack stack, boolean check, Operation<Void> original) {
        if (!(stack.getItem() instanceof AbstractGunItem gun)) { original.call(count, stack, check); return; }
        if (check && !gun.useDummyAmmo(stack)) { original.call(count, stack, check); return; }
        int obtained = check ? gun.findAndExtractDummyAmmo(stack, count) : count;
        var state = AmmoState.read(stack, gun);
        state.reserve.addLast(false, obtained);
        AmmoState.write(stack, gun, state);
    }
}

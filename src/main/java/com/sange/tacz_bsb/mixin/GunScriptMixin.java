package com.sange.tacz_bsb.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import com.sange.tacz_bsb.BsbConfig;
import com.sange.tacz_bsb.ammo.AmmoTransactions;
import com.tacz.guns.entity.EntityKineticBullet;
import com.tacz.guns.item.ModernKineticGunScriptAPI;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = ModernKineticGunScriptAPI.class, remap = false)
public abstract class GunScriptMixin {
    @Unique private int bsb$fired;
    @Unique private ModernKineticGunScriptAPI bsb$api() { return (ModernKineticGunScriptAPI)(Object)this; }

    @WrapMethod(method = "consumeAmmoFromPlayer")
    private int bsb$consume(int count, Operation<Integer> original) {
        return AmmoTransactions.active(bsb$api()) ? AmmoTransactions.consume(bsb$api(), count) : original.call(count);
    }
    @WrapMethod(method = "putAmmoInMagazine")
    private int bsb$put(int count, Operation<Integer> original) {
        return AmmoTransactions.active(bsb$api()) ? AmmoTransactions.put(bsb$api(), count) : original.call(count);
    }
    @WrapMethod(method = "removeAmmoFromMagazine")
    private int bsb$remove(int count, Operation<Integer> original) {
        return AmmoTransactions.active(bsb$api()) ? AmmoTransactions.remove(bsb$api(), count) : original.call(count);
    }
    @WrapMethod(method = "setAmmoInBarrel")
    private void bsb$chamber(boolean present, Operation<Void> original) {
        if (AmmoTransactions.active(bsb$api())) AmmoTransactions.chamber(bsb$api(), present);
        else original.call(present);
    }
    @WrapMethod(method = "reduceAmmoOnce")
    private boolean bsb$reduce(Operation<Boolean> original) {
        bsb$fired = 0;
        if (!AmmoTransactions.active(bsb$api())) return original.call();
        bsb$fired = AmmoTransactions.fire(bsb$api());
        return bsb$fired != 0;
    }
    @WrapOperation(method = "lambda$shootOnce$2", at = @At(value = "INVOKE",
            target = "Lcom/tacz/guns/item/ModernKineticGunScriptAPI;reduceAmmoOnce()Z"))
    private boolean bsb$capture(ModernKineticGunScriptAPI api, Operation<Boolean> original,
                                @Share("bsb_round") LocalIntRef round) {
        boolean success = original.call(api);
        round.set(success ? bsb$fired : 0);
        return success;
    }
    @WrapOperation(method = "lambda$shootOnce$2", at = @At(value = "INVOKE",
            target = "Lcom/tacz/guns/entity/EntityKineticBullet;setShotDamageMultiplier(F)V"))
    private void bsb$initializeBullet(EntityKineticBullet bullet, float multiplier, Operation<Void> original,
                                      @Share("bsb_round") LocalIntRef round) {
        var api = bsb$api();
        boolean precise = AmmoTransactions.active(api)
                && (round.get() != 0 ? round.get() : AmmoTransactions.peek(api)) == 2;
        String id = api.getGunIndex().getGunData().getAmmoId().toString();
        original.call(bullet, precise ? multiplier * BsbConfig.multiplier(id, false) : multiplier);
        if (precise) {
            BulletAccessor access = (BulletAccessor) bullet;
            access.bsb$setExplosionDamage(access.bsb$getExplosionDamage() * BsbConfig.multiplier(id, true));
        }
    }
}


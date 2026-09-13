package com.sange.tacz_bsb.mixin;

import com.tacz.guns.entity.EntityKineticBullet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Field access only: does not change ticking, hit handling, or the entity type. */
@Mixin(value = EntityKineticBullet.class, remap = false)
public interface BulletAccessor {
    @Accessor("explosionDamage") float bsb$getExplosionDamage();
    @Accessor("explosionDamage") void bsb$setExplosionDamage(float damage);
}


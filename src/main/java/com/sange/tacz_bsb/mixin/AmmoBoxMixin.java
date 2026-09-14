package com.sange.tacz_bsb.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.sange.tacz_bsb.ammo.AmmoBoxes;
import com.sange.tacz_bsb.ammo.AmmoTransactions;
import com.tacz.guns.item.AmmoBoxItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(AmmoBoxItem.class)
public abstract class AmmoBoxMixin {
    @WrapMethod(method = "overrideStackedOnOther")
    private boolean bsb$click(ItemStack box, Slot slot, ClickAction click, Player player, Operation<Boolean> original) {
        return AmmoBoxes.click(box, slot, click, player);
    }
    @WrapMethod(method = "isFoil")
    private boolean bsb$foil(ItemStack box, Operation<Boolean> original) {
        return AmmoTransactions.boxTier(box) > 1 || original.call(box);
    }
}



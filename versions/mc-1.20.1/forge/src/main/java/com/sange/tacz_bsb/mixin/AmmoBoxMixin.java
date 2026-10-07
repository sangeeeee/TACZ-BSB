package com.sange.tacz_bsb.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.injection.At;
import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.inventory.tooltip.AmmoBoxTooltip;
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
    @WrapOperation(method = "getTooltipImage", at = @At(value = "NEW",
            target = "(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;I)Lcom/tacz/guns/inventory/tooltip/AmmoBoxTooltip;"))
    private AmmoBoxTooltip bsb$tooltip(ItemStack box, ItemStack ammo, int count, Operation<AmmoBoxTooltip> original) {
        int tier = AmmoTransactions.boxTier(box);
        if (tier > 1 && ammo.getItem() instanceof IAmmo item) {
            ammo = AmmoTransactions.stack(item.getAmmoId(ammo), tier, ammo.getCount());
        }
        // TaCZ uses this same stack for the inline name, width calculation and rendered icon.
        return original.call(box, ammo, count);
    }
    @WrapMethod(method = "isFoil")
    private boolean bsb$foil(ItemStack box, Operation<Boolean> original) {
        return AmmoTransactions.boxTier(box) > 1 || original.call(box);
    }
}

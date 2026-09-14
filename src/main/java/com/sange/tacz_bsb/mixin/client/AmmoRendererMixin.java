package com.sange.tacz_bsb.mixin.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sange.tacz_bsb.ammo.AmmoTransactions;
import com.sange.tacz_bsb.client.AmmoGlint;
import com.tacz.guns.client.renderer.item.AmmoItemRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AmmoItemRenderer.class)
public abstract class AmmoRendererMixin {
    @WrapMethod(method = "renderByItem")
    private void bsb$scope(ItemStack stack, ItemDisplayContext context, PoseStack poses,
            MultiBufferSource buffers, int light, int overlay, Operation<Void> original) {
        boolean previous = AmmoGlint.ACTIVE.get();
        AmmoGlint.ACTIVE.set(AmmoTransactions.precise(stack));
        try { original.call(stack, context, poses, buffers, light, overlay); }
        finally { AmmoGlint.ACTIVE.set(previous); }
    }
    @WrapOperation(method = "lambda$renderByItem$0", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/MultiBufferSource;getBuffer(Lnet/minecraft/client/renderer/RenderType;)Lcom/mojang/blaze3d/vertex/VertexConsumer;"))
    private VertexConsumer bsb$glint(MultiBufferSource source, RenderType type, Operation<VertexConsumer> original) {
        return AmmoGlint.wrap(source, original.call(source, type), true);
    }
}


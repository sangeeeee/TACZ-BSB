package com.sange.tacz_bsb.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sange.tacz_bsb.client.AmmoGlint;
import com.tacz.guns.client.model.bedrock.BedrockModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = BedrockModel.class, remap = false)
public abstract class AmmoModelMixin {
    @WrapOperation(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/item/ItemDisplayContext;Lnet/minecraft/client/renderer/RenderType;IIFFFF)V", at = @At(value = "INVOKE", remap = true,
            target = "Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;getBuffer(Lnet/minecraft/client/renderer/RenderType;)Lcom/mojang/blaze3d/vertex/VertexConsumer;"))
    private VertexConsumer bsb$glint(MultiBufferSource.BufferSource source, RenderType type, Operation<VertexConsumer> original) {
        return AmmoGlint.wrap(source, original.call(source, type), false);
    }
}


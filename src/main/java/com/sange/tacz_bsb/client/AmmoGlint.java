package com.sange.tacz_bsb.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

public final class AmmoGlint {
    public static final ThreadLocal<Boolean> ACTIVE = ThreadLocal.withInitial(() -> false);
    public static VertexConsumer wrap(MultiBufferSource source, VertexConsumer original) {
        return ACTIVE.get() ? VertexMultiConsumer.create(source.getBuffer(RenderType.entityGlintDirect()), original) : original;
    }
}


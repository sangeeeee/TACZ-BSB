package com.sange.tacz_bsb.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import java.util.function.Supplier;

public final class AmmoGlint {
    public static final ThreadLocal<Boolean> ACTIVE = ThreadLocal.withInitial(() -> false);
    public static VertexConsumer wrap(MultiBufferSource source, Supplier<VertexConsumer> original, boolean itemIcon) {
        if (!ACTIVE.get()) return original.get();
        // Acquire the fixed glint buffer first. Switching away from TaCZ's shared
        // texture buffer flushes it in 1.20.1, invalidating a previously acquired consumer.
        VertexConsumer glint = source.getBuffer(itemIcon ? RenderType.glint() : RenderType.entityGlintDirect());
        return VertexMultiConsumer.create(glint, original.get());
    }
}

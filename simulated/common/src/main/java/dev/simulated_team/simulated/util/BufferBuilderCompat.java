package dev.simulated_team.simulated.util;

import com.mojang.blaze3d.vertex.BufferBuilder;

public final class BufferBuilderCompat {
    private BufferBuilderCompat() {
    }

    public static BufferBuilder.RenderedBuffer buildOrThrow(final BufferBuilder builder) {
        final BufferBuilder.RenderedBuffer buffer = builder.endOrDiscardIfEmpty();
        if (buffer == null) {
            throw new IllegalStateException("BufferBuilder was empty");
        }
        return buffer;
    }
}

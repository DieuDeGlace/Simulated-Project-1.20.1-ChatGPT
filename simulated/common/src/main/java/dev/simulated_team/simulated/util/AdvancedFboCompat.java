package dev.simulated_team.simulated.util;

import foundry.veil.api.client.render.framebuffer.AdvancedFbo;
import foundry.veil.api.client.render.framebuffer.FramebufferAttachmentDefinition;

/**
 * Compatibility helpers for the Veil 1.20.1 framebuffer builder.
 */
public final class AdvancedFboCompat {
    private AdvancedFboCompat() {
    }

    /**
     * Creates an RGBA color texture with a sampleable depth texture.
     *
     * <p>Veil 1.20.1 keeps the color attachment format selected by
     * {@link AdvancedFbo.Builder#addColorTextureBuffer()} when
     * {@link AdvancedFbo.Builder#setDepthTextureBuffer()} is called. Select the depth format
     * explicitly after the color attachment has been captured so the resulting framebuffer is
     * complete. Newer Veil versions reset and select this format internally.</p>
     */
    public static AdvancedFbo createColorDepthTexture(final int width, final int height) {
        return AdvancedFbo.withSize(width, height)
                .addColorTextureBuffer()
                .setFormat(FramebufferAttachmentDefinition.Format.DEPTH_COMPONENT)
                .setDepthTextureBuffer()
                .build(true);
    }
}

package dev.eriksonn.aeronautics.mixin.render.sodium;

import dev.eriksonn.aeronautics.content.blocks.levitite.LevititeShaderManager;
import dev.eriksonn.aeronautics.index.client.AeroRenderTypes;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.shader.program.ShaderProgram;
import me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Priority <1000 to apply before sable
 */
@Mixin(value = SodiumWorldRenderer.class, priority = 990, remap = false)
public class SodiumWorldRendererMixin {

    @Inject(method = "drawChunkLayer", at = @At("HEAD"), remap = false)
    public void aeronautics$setupLevititeShaders(final RenderType renderType, final PoseStack poseStack, final double x, final double y, final double z, final CallbackInfo ci) {
        if (renderType == AeroRenderTypes.levitite()) {
            final ShaderProgram shader = VeilRenderSystem.setShader(AeroRenderTypes.LEVITITE_SHADER);
            if (shader == null) return;

            long ticks = Minecraft.getInstance().level.getGameTime();
            final float pt = Minecraft.getInstance().getPartialTick();
            ticks = ticks % 100000;
            shader.setFloat("time", ticks + pt);
            LevititeShaderManager.prepareShaderForWorld(shader.toShaderInstance(), x, y, z);
        }
    }

    @Inject(method = "drawChunkLayer", at = @At("TAIL"), remap = false)
    public void aeronautics$cleanupLevititeShaders(final RenderType renderLayer, final PoseStack poseStack, final double x, final double y, final double z, final CallbackInfo ci) {
        if (renderLayer == AeroRenderTypes.levitite()) {
            final ShaderProgram shader = VeilRenderSystem.setShader(AeroRenderTypes.LEVITITE_SHADER);
            if (shader == null) return;

            // reset back to world once rendering is done, for safety
            LevititeShaderManager.prepareShaderForWorld(shader.toShaderInstance(), x, y, z);
        }
    }
}

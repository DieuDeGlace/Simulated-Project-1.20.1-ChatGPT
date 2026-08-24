package dev.eriksonn.aeronautics.mixin.render.vanilla;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.shaders.Uniform;
import dev.eriksonn.aeronautics.content.blocks.levitite.LevititeShaderManager;
import dev.eriksonn.aeronautics.index.client.AeroRenderTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

@Mixin(value = LevelRenderer.class, priority = 990)
public class LevelRendererMixin {

    @Shadow
    @Nullable
    private ClientLevel level;

    @Inject(method = "renderChunkLayer", at = @At("HEAD"), cancellable = true)
    private void aeronautics$skipGhostLayer(final RenderType renderType, final PoseStack poseStack,
                                             final double x, final double y, final double z,
                                             final Matrix4f projectionMatrix, final CallbackInfo ci) {
        if (renderType == AeroRenderTypes.levititeGhosts()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderChunkLayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ShaderInstance;apply()V", shift = At.Shift.AFTER))
    public void aeronautics$setupLevititeShaders(final RenderType renderType, final PoseStack poseStack,
                                                  final double x, final double y, final double z,
                                                  final Matrix4f projectionMatrix, final CallbackInfo ci,
                                                  @Local final ShaderInstance shaderinstance) {
        if (renderType == AeroRenderTypes.levitite()) {

            Uniform time = shaderinstance.getUniform("time");
            if (time != null) {
                long ticks = this.level.getGameTime();
                final float pt = Minecraft.getInstance().getPartialTick();
                ticks = ticks % 100000;
                time.set(ticks + pt);
            }

            LevititeShaderManager.prepareShaderForWorld(shaderinstance, x, y, z);
        }
    }

    @Inject(method = "renderChunkLayer", at = @At("TAIL"))
    public void aeronautics$cleanupLevititeShaders(final RenderType renderType, final PoseStack poseStack,
                                                    final double x, final double y, final double z,
                                                    final Matrix4f projectionMatrix, final CallbackInfo ci,
                                                    @Local final ShaderInstance shaderinstance) {
        if (renderType == AeroRenderTypes.levitite()) {
            // reset back to world once rendering is done, for safety
            LevititeShaderManager.prepareShaderForWorld(shaderinstance, x, y, z);
        }
    }

}

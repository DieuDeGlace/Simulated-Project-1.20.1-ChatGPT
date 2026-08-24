package dev.eriksonn.aeronautics.mixin.levitite;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.eriksonn.aeronautics.content.blocks.levitite.LevititeShaderManager;
import dev.eriksonn.aeronautics.index.client.AeroRenderTypes;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.render.vanilla.VanillaChunkedSubLevelRenderData;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(VanillaChunkedSubLevelRenderData.class)
public class VanillaChunkedSubLevelRenderDataMixin {

    @Final
    @Shadow(remap = false)
    private ClientSubLevel subLevel;

    @WrapMethod(method = "renderChunkedSubLevel", remap = false)
    public int renderChunkedSubLevel(final RenderType layer,
                                      final ShaderInstance shader,
                                      final Matrix4f modelView,
                                      final double camX,
                                      final double camY,
                                      final double camZ,
                                      final Operation<Integer> original) {
        if (layer == AeroRenderTypes.levitite()) {
            final LevititeShaderManager manager = LevititeShaderManager.getInstance(this.subLevel);
            manager.prepareShaderForSublevel(this.subLevel, shader, camX, camY, camZ);
            return original.call(layer, shader, modelView, camX, camY, camZ);

        } else if (layer == AeroRenderTypes.levititeGhosts()) {
            final LevititeShaderManager manager = LevititeShaderManager.getInstance(this.subLevel);
            if (manager.needsLayers()) {
                manager.prepareShaderForSublevel(this.subLevel, shader, camX, camY, camZ);
                shader.safeGetUniform("layerIndex").set(1);
                RenderSystem.disableDepthTest();
                int renderedSections = original.call(layer, shader, modelView, camX, camY, camZ);
                shader.safeGetUniform("layerIndex").set(-1);
                renderedSections += original.call(layer, shader, modelView, camX, camY, camZ);
                shader.safeGetUniform("layerIndex").set(0);
                RenderSystem.enableDepthTest();
                return renderedSections;
            }
            return 0;
        }
        return original.call(layer, shader, modelView, camX, camY, camZ);
    }
}

package dev.simulated_team.simulated.validation.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ryanhcode.offroad.content.blocks.wheel_mount.WheelMountBlockEntity;
import dev.ryanhcode.offroad.content.blocks.wheel_mount.WheelMountRenderer;
import dev.simulated_team.simulated.validation.SubLevelRenderClientValidation;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = WheelMountRenderer.class, remap = false)
public abstract class WheelMountRendererMixin {
    @Inject(method = "renderSafe", at = @At("HEAD"), remap = false)
    private void simulated$recordRender(final WheelMountBlockEntity blockEntity,
                                        final float partialTick,
                                        final PoseStack poseStack,
                                        final MultiBufferSource buffers,
                                        final int light,
                                        final int overlay,
                                        final CallbackInfo ci) {
        SubLevelRenderClientValidation.recordWheelRendererEntry(blockEntity);
    }
}

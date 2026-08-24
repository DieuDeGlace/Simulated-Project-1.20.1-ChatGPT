package dev.simulated_team.simulated.validation.mixin;

import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import dev.eriksonn.aeronautics.content.blocks.propeller.small.BasePropellerBlockEntity;
import dev.eriksonn.aeronautics.content.blocks.propeller.small.SimplePropellerVisual;
import dev.eriksonn.aeronautics.content.blocks.propeller.small.andesite.AndesitePropellerBlockEntity;
import dev.simulated_team.simulated.validation.SubLevelRenderClientValidation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SimplePropellerVisual.class, remap = false)
public abstract class AndesitePropellerVisualMixin {
    @Unique
    private AndesitePropellerBlockEntity simulated$blockEntity;

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void simulated$recordCreation(final VisualizationContext context,
                                          final BasePropellerBlockEntity blockEntity,
                                          final float partialTick,
                                          final CallbackInfo ci) {
        if (blockEntity instanceof final AndesitePropellerBlockEntity andesite) {
            this.simulated$blockEntity = andesite;
            SubLevelRenderClientValidation.recordPropellerVisualCreated(andesite);
        }
    }

    @Inject(method = "beginFrame", at = @At("HEAD"), remap = false)
    private void simulated$recordFrame(final SimpleDynamicVisual.Context context, final CallbackInfo ci) {
        SubLevelRenderClientValidation.recordPropellerVisualFrame(this.simulated$blockEntity);
    }
}

package dev.eriksonn.aeronautics.neoforge.mixin;

import com.google.common.collect.ImmutableList;
import dev.eriksonn.aeronautics.neoforge.compatibility.AeroChunkRenderTypeCompatibility;
import foundry.veil.forge.ForgeRenderTypeStageHandler;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ForgeRenderTypeStageHandler.class, remap = false)
public abstract class ForgeRenderTypeStageHandlerMixin {
    @Inject(method = "setBlockLayers", at = @At("RETURN"), remap = false)
    private static void aeronautics$finalizeChunkLayerAbi(final ImmutableList.Builder<RenderType> builder,
                                                          final CallbackInfo ci) {
        AeroChunkRenderTypeCompatibility.onVeilBlockLayersFinalized();
    }
}

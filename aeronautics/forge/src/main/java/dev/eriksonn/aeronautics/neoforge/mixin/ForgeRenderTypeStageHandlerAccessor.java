package dev.eriksonn.aeronautics.neoforge.mixin;

import foundry.veil.forge.ForgeRenderTypeStageHandler;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;
import java.util.Set;

@Mixin(value = ForgeRenderTypeStageHandler.class, remap = false)
public interface ForgeRenderTypeStageHandlerAccessor {

    @Accessor(value = "CUSTOM_BLOCK_LAYERS", remap = false)
    static Set<RenderType> getCustomBlockLayers() {
        throw new AssertionError("Mixin accessor was not applied");
    }

    @Mutable
    @Accessor(value = "BLOCK_LAYERS", remap = false)
    static void setBlockLayers(final List<RenderType> blockLayers) {
        throw new AssertionError("Mixin accessor was not applied");
    }
}

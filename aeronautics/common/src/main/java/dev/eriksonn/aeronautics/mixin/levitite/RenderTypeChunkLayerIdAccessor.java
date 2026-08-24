package dev.eriksonn.aeronautics.mixin.levitite;

import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RenderType.class)
public interface RenderTypeChunkLayerIdAccessor {

    @Accessor(value = "chunkLayerId", remap = false)
    void setChunkLayerId(int chunkLayerId);
}

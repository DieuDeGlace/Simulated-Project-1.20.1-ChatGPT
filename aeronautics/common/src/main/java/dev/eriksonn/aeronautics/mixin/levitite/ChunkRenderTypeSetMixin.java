package dev.eriksonn.aeronautics.mixin.levitite;

import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.client.ChunkRenderTypeSet;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.BitSet;
import java.util.Iterator;

/** Keeps cached Forge render-layer bitsets aligned with Veil's live layer table. */
@Mixin(value = ChunkRenderTypeSet.class, remap = false)
public abstract class ChunkRenderTypeSetMixin {
    @Shadow
    @Final
    private BitSet bits;

    /**
     * @author Sable/Simulated port
     * @reason Forge's iterator indexes a static RenderType array captured before Veil adds
     * custom layers. Aeronautics must reassign chunk-layer ids, so cached bitsets have to be
     * resolved against the live ids instead of that stale positional array.
     */
    @Overwrite
    public Iterator<RenderType> iterator() {
        return RenderType.chunkBufferLayers().stream()
                .filter(renderType -> {
                    final int layerId = renderType.getChunkLayerId();
                    return layerId >= 0 && this.bits.get(layerId);
                })
                .iterator();
    }
}

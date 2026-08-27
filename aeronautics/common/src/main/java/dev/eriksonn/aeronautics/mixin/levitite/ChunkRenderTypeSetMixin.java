package dev.eriksonn.aeronautics.mixin.levitite;

import dev.eriksonn.aeronautics.compat.ChunkRenderLayerCompatibility;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.client.ChunkRenderTypeSet;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;

/** Preserves creation-time layer meanings while retaining the legacy live-id bridge for diagnosis. */
@Mixin(value = ChunkRenderTypeSet.class, remap = false)
public abstract class ChunkRenderTypeSetMixin {
    @Shadow
    @Final
    private static RenderType[] CHUNK_RENDER_TYPES;

    @Shadow
    @Final
    private BitSet bits;

    @Unique
    private List<RenderType> aeronautics$creationLayers;

    /**
     * Preserve the meaning of this bitset at its creation boundary. This uses
     * Forge's authoritative table at that exact time and does not depend on
     * Embeddium private fields or Mixin application order.
     */
    @Inject(method = "<init>", at = @At("RETURN"))
    private void aeronautics$captureCreationLayers(final BitSet bits, final CallbackInfo ci) {
        final List<RenderType> layers = new ArrayList<>();
        for (int index = bits.nextSetBit(0); index >= 0; index = bits.nextSetBit(index + 1)) {
            if (index < CHUNK_RENDER_TYPES.length) {
                layers.add(CHUNK_RENDER_TYPES[index]);
            }
        }
        this.aeronautics$creationLayers = List.copyOf(layers);
    }

    /**
     * @author Sable/Simulated port
     * @reason Forge's iterator indexes a static RenderType array captured before Veil adds
     * custom layers. The former port behavior reinterpreted every cached bitset against live
     * ids; that remains available behind the runtime command, while the default preserves the
     * meaning captured when each set was constructed.
     */
    @Overwrite
    public Iterator<RenderType> iterator() {
        if (!ChunkRenderLayerCompatibility.isLiveLayerIdInterpretationEnabled()) {
            return this.aeronautics$creationLayers.iterator();
        }

        return RenderType.chunkBufferLayers().stream()
                .filter(renderType -> {
                    final int layerId = renderType.getChunkLayerId();
                    return layerId >= 0 && this.bits.get(layerId);
                })
                .iterator();
    }
}

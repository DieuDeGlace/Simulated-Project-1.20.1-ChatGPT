package dev.eriksonn.aeronautics.neoforge.compatibility;

import dev.eriksonn.aeronautics.mixin.levitite.ChunkRenderTypeSetAccessor;
import dev.eriksonn.aeronautics.mixin.levitite.RenderTypeChunkLayerIdAccessor;
import dev.eriksonn.aeronautics.neoforge.mixin.ForgeRenderTypeStageHandlerAccessor;
import foundry.veil.forge.ForgeRenderTypeStageHandler;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.client.ChunkRenderTypeSet;

import java.util.LinkedHashSet;
import java.util.List;

/** Keeps Forge's chunk-layer bit ABI stable when Veil finalizes custom layers. */
public final class AeroChunkRenderTypeCompatibility {
    private AeroChunkRenderTypeCompatibility() {
    }

    /**
     * Runs synchronously at the return of Veil's block-layer finalization.
     * Vanilla layers retain their Forge-assigned ids 0-4, while every custom
     * layer is appended in Veil registration order. Updating Forge's cached
     * table at this boundary also repairs the supported early-initialization
     * case without changing ChunkRenderTypeSet's behavior for other mods.
     */
    public static void onVeilBlockLayersFinalized() {
        final List<RenderType> currentLayers = ForgeRenderTypeStageHandler.getBlockLayers();
        if (currentLayers == null) {
            return;
        }

        final LinkedHashSet<RenderType> orderedLayers = new LinkedHashSet<>();
        orderedLayers.add(RenderType.solid());
        orderedLayers.add(RenderType.cutoutMipped());
        orderedLayers.add(RenderType.cutout());
        orderedLayers.add(RenderType.translucent());
        orderedLayers.add(RenderType.tripwire());
        orderedLayers.addAll(currentLayers);

        final List<RenderType> finalizedLayers = List.copyOf(orderedLayers);
        ForgeRenderTypeStageHandlerAccessor.setBlockLayers(finalizedLayers);
        for (int index = 0; index < finalizedLayers.size(); index++) {
            ((RenderTypeChunkLayerIdAccessor) (Object) finalizedLayers.get(index)).setChunkLayerId(index);
        }

        // ChunkRenderTypeSet may have been initialized before Veil by another
        // mod. Its pre-existing vanilla bitsets remain valid because ids 0-4
        // never move; only its cached lookup table and ALL singleton need the
        // finalized suffix.
        ChunkRenderTypeSetAccessor.setChunkRenderTypesList(finalizedLayers);
        ChunkRenderTypeSetAccessor.setChunkRenderTypes(finalizedLayers.toArray(new RenderType[0]));
        final var allBits = ((ChunkRenderTypeSetAccessor) (Object) ChunkRenderTypeSet.all()).getBits();
        allBits.clear();
        allBits.set(0, finalizedLayers.size());

        final Object allSet = ChunkRenderTypeSet.all();
        if (allSet instanceof final EmbeddiumChunkRenderTypeSetExtension extension) {
            extension.aeronautics$rebuildUniverse();
        }
    }
}

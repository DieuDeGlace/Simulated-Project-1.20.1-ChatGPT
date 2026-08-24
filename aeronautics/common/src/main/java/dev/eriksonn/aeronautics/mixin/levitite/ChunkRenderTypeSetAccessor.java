package dev.eriksonn.aeronautics.mixin.levitite;

import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.client.ChunkRenderTypeSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.BitSet;
import java.util.List;

@Mixin(value = ChunkRenderTypeSet.class, remap = false)
public interface ChunkRenderTypeSetAccessor {

    @Mutable
    @Accessor(value = "CHUNK_RENDER_TYPES_LIST", remap = false)
    static void setChunkRenderTypesList(final List<RenderType> data) { throw new AssertionError("Something has gone terribly wrong."); }// = RenderType.chunkBufferLayers();

    @Accessor(value = "CHUNK_RENDER_TYPES", remap = false)
    @Mutable
    static void setChunkRenderTypes(final RenderType[] data) { throw new AssertionError("Something has gone terribly wrong."); }// = CHUNK_RENDER_TYPES_LIST.toArray(new RenderType[0]);

    @Accessor(value = "bits", remap = false)
    BitSet getBits();

}

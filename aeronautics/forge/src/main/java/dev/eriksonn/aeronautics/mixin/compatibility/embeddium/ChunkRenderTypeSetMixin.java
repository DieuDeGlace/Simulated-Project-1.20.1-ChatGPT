package dev.eriksonn.aeronautics.mixin.compatibility.embeddium;

import com.google.common.collect.ImmutableList;
import dev.eriksonn.aeronautics.neoforge.compatibility.EmbeddiumChunkRenderTypeSetExtension;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.client.ChunkRenderTypeSet;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.BitSet;

/**
 * Rebuilds Embeddium's derived set universe after Veil finalizes late chunk
 * layers. Lower priority makes these shadows resolve after Embeddium adds the
 * cache fields to Forge's ChunkRenderTypeSet.
 */
@Mixin(value = ChunkRenderTypeSet.class, priority = 900, remap = false)
public abstract class ChunkRenderTypeSetMixin implements EmbeddiumChunkRenderTypeSetExtension {
    @Shadow
    @Final
    @Dynamic("Forge field shadowed by Embeddium and consumed by this lower-priority compatibility mixin")
    private static RenderType[] CHUNK_RENDER_TYPES;

    @Shadow
    @Final
    @Mutable
    @Dynamic("Added to ChunkRenderTypeSet by Embeddium before this lower-priority compatibility mixin")
    private static int POSSIBLE_RENDER_TYPE_COMBINATIONS;

    @Shadow
    @Final
    @Mutable
    @Dynamic("Added to ChunkRenderTypeSet by Embeddium before this lower-priority compatibility mixin")
    private static int MASK_ALL;

    @Shadow
    @Final
    @Mutable
    @Dynamic("Added to ChunkRenderTypeSet by Embeddium before this lower-priority compatibility mixin")
    private static ChunkRenderTypeSet[] UNIVERSE;

    @Shadow
    @Dynamic("Added to ChunkRenderTypeSet by Embeddium before this lower-priority compatibility mixin")
    private int mask;

    @Shadow
    @Dynamic("Added to ChunkRenderTypeSet by Embeddium before this lower-priority compatibility mixin")
    private ImmutableList<RenderType> embeddium$containedTypes;

    @Invoker("<init>")
    private static ChunkRenderTypeSet aeronautics$construct(final BitSet bits) {
        throw new AssertionError("Mixin invoker was not applied");
    }

    @Unique
    @Override
    public void aeronautics$rebuildUniverse() {
        final int layerCount = CHUNK_RENDER_TYPES.length;
        if (layerCount > Byte.SIZE) {
            throw new IllegalStateException("Embeddium supports at most 8 chunk render layers, found " + layerCount);
        }

        final int combinations = 1 << layerCount;
        final int allMask = combinations - 1;
        final ChunkRenderTypeSet[] rebuilt = new ChunkRenderTypeSet[combinations];
        rebuilt[0] = ChunkRenderTypeSet.none();
        for (int bits = 1; bits < allMask; bits++) {
            rebuilt[bits] = aeronautics$construct(BitSet.valueOf(new long[]{bits}));
        }

        final ChunkRenderTypeSet all = ChunkRenderTypeSet.all();
        this.mask = allMask;
        this.embeddium$containedTypes = ImmutableList.copyOf(CHUNK_RENDER_TYPES);
        rebuilt[allMask] = all;

        POSSIBLE_RENDER_TYPE_COMBINATIONS = combinations;
        MASK_ALL = allMask;
        UNIVERSE = rebuilt;
    }
}

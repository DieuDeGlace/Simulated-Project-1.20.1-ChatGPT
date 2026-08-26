package dev.eriksonn.aeronautics.mixin.levitite;

import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Holder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.RenderTypeHelper;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Iterator;
import java.util.Map;

/**
 * Keeps the legacy single-layer API aligned with the live Forge layer set.
 *
 * <p>Connector's Fabric block-render-layer bridge obtains the first layer by
 * indexing Forge's startup-time render-type array directly. Aeronautics must
 * reindex the live layer table when its Veil layers are installed late, so
 * that frozen lookup can disagree with the live {@link ChunkRenderTypeSet}.
 * The set iterator is already the authoritative compatibility boundary; use
 * the same result for the two legacy single-layer methods.</p>
 */
@Mixin(ItemBlockRenderTypes.class)
public abstract class ItemBlockRenderTypesMixin {
    @Shadow
    @Final
    private static Map<Block, RenderType> TYPE_BY_BLOCK;

    @Shadow(remap = false)
    @Final
    private static Map<Holder.Reference<Block>, ChunkRenderTypeSet> BLOCK_RENDER_TYPES;

    @Inject(method = "getChunkRenderType", at = @At("RETURN"), cancellable = true, require = 1)
    private static void aeronautics$useLiveFirstChunkLayer(
            final BlockState state,
            final CallbackInfoReturnable<RenderType> cir) {
        final RenderType first = aeronautics$getLiveFirstLayer(state);
        if (first != null) {
            cir.setReturnValue(first);
        }
    }

    @Inject(method = "getMovingBlockRenderType", at = @At("RETURN"), cancellable = true, require = 1)
    private static void aeronautics$useLiveFirstMovingLayer(
            final BlockState state,
            final CallbackInfoReturnable<RenderType> cir) {
        final RenderType first = aeronautics$getLiveFirstLayer(state);
        if (first != null) {
            cir.setReturnValue(RenderTypeHelper.getMovingBlockRenderType(first));
        }
    }

    private static RenderType aeronautics$getLiveFirstLayer(final BlockState state) {
        // Vanilla handles leaves through its fancy/fast graphics branch before
        // consulting the render-layer map; preserve that special behavior.
        if (state.getBlock() instanceof LeavesBlock) {
            return null;
        }

        final Block block = state.getBlock();
        final RenderType vanillaLayer = TYPE_BY_BLOCK.get(block);
        if (vanillaLayer != null) {
            return vanillaLayer;
        }

        final Holder.Reference<Block> holder = ForgeRegistries.BLOCKS.getDelegateOrThrow(block);
        // Forge's map has SOLID as its default return value. That shared set may
        // have been created before Aeronautics reindexed Veil's layer table, so
        // iterating it is not valid evidence of an explicit registration.
        if (!BLOCK_RENDER_TYPES.containsKey(holder)) {
            return RenderType.solid();
        }

        final ChunkRenderTypeSet layers = BLOCK_RENDER_TYPES.get(holder);
        if (layers == null) {
            return null;
        }

        final Iterator<RenderType> iterator = layers.iterator();
        return iterator.hasNext() ? iterator.next() : null;
    }
}

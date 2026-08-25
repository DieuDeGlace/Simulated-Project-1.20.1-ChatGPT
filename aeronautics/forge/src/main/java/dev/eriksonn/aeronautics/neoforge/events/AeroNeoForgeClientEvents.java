package dev.eriksonn.aeronautics.neoforge.events;

import dev.eriksonn.aeronautics.Aeronautics;
import dev.eriksonn.aeronautics.events.AeronauticsClientEvents;
import dev.eriksonn.aeronautics.index.AeroBlocks;
import dev.eriksonn.aeronautics.index.client.AeroRenderTypes;
import dev.eriksonn.aeronautics.mixin.levitite.ChunkRenderTypeSetAccessor;
import dev.eriksonn.aeronautics.mixin.levitite.RenderTypeChunkLayerIdAccessor;
import dev.eriksonn.aeronautics.neoforge.mixin.ForgeRenderTypeStageHandlerAccessor;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import java.util.List;
import java.util.LinkedHashSet;

@EventBusSubscriber(modid = Aeronautics.MOD_ID, value = Dist.CLIENT)
public class AeroNeoForgeClientEvents {

    @SubscribeEvent
    public static void clientTick(final TickEvent.ClientTickEvent event) {
        AeronauticsClientEvents.clientLevelTick(event.phase == TickEvent.Phase.END);
    }


    @EventBusSubscriber(modid = Aeronautics.MOD_ID, value = Dist.CLIENT)
    public static class ModBusEvents {

        @SubscribeEvent
        public static void clientSetup(final FMLClientSetupEvent event) {
            event.enqueueWork(() -> {
                fixChunkRenderTypeSet();

                final ChunkRenderTypeSet set = ChunkRenderTypeSet.of(RenderType.solid(), AeroRenderTypes.levitite(), AeroRenderTypes.levititeGhosts());
                ItemBlockRenderTypes.setRenderLayer(AeroBlocks.LEVITITE.get(), set);
                ItemBlockRenderTypes.setRenderLayer(AeroBlocks.PEARLESCENT_LEVITITE.get(), set);
            });
        }

        /**
         * Certain mods (like Bookshelf) cause the ChunkRenderTypeSet class in NeoForge to get initialized early,
         * cementing the chunk render layers inside it. We do this as an unfortunate safety measure to "fix" the
         * static collections in ChunkRenderTypeSet to include the Levitite layers, if the class is loaded before
         * us.
         */
        private static void fixChunkRenderTypeSet() {
            // Veil exposes custom block layers before the vanilla list. ChunkRenderTypeSet
            // instances created before Veil initializes already encode solid/cutout/etc. as
            // bits 0-4, so changing those indices makes every cached solid model resolve to
            // the first custom layer instead. Keep the vanilla ABI stable and append every
            // custom layer afterward, preserving its relative order.
            final LinkedHashSet<RenderType> orderedLayers = new LinkedHashSet<>();
            orderedLayers.add(RenderType.solid());
            orderedLayers.add(RenderType.cutoutMipped());
            orderedLayers.add(RenderType.cutout());
            orderedLayers.add(RenderType.translucent());
            orderedLayers.add(RenderType.tripwire());
            orderedLayers.addAll(RenderType.chunkBufferLayers());

            final List<RenderType> list = List.copyOf(orderedLayers);
            ForgeRenderTypeStageHandlerAccessor.setBlockLayers(list);

            for (int index = 0; index < list.size(); index++) {
                ((RenderTypeChunkLayerIdAccessor) (Object) list.get(index)).setChunkLayerId(index);
            }

            ChunkRenderTypeSetAccessor.setChunkRenderTypesList(list);
            ChunkRenderTypeSetAccessor.setChunkRenderTypes(list.toArray(new RenderType[0]));
            ((ChunkRenderTypeSetAccessor) (Object) ChunkRenderTypeSet.all()).getBits().set(0, list.size());
        }
    }
}

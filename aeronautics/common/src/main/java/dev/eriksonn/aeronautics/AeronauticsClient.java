package dev.eriksonn.aeronautics;

import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.effect.ClientBalloonEffectRenderer;
import dev.eriksonn.aeronautics.content.ponder.AeroPonderPlugin;
import dev.eriksonn.aeronautics.index.AeroClickInteractions;
import dev.eriksonn.aeronautics.index.AeroPartialModels;
import dev.eriksonn.aeronautics.index.client.AeroClientRegistries;
import dev.eriksonn.aeronautics.index.client.AeroRenderTypes;
import dev.eriksonn.aeronautics.index.client.AeroSituationalMusic;
import foundry.veil.api.event.VeilRenderLevelStageEvent;
import foundry.veil.platform.VeilEventPlatform;
import net.createmod.ponder.foundation.PonderIndex;

public class AeronauticsClient {
    public static void init() {
        PonderIndex.addPlugin(new AeroPonderPlugin());

        AeroClientRegistries.init();
        AeroPartialModels.init();
        AeroSituationalMusic.init();
        AeroClickInteractions.init();

        registerEvents();
    }

    private static void registerEvents() {
        VeilEventPlatform.INSTANCE.onVeilRenderTypeStageRender((stage,
                                                                levelRenderer,
                                                                bufferSource,
                                                                matrixStack,
                                                                projectionMatrix,
                                                                renderTick,
                                                                partialTick,
                                                                camera,
                                                                frustum) -> {
            ClientBalloonEffectRenderer.onRenderLevelStage(stage, matrixStack.last().pose(),
                    projectionMatrix, renderTick, partialTick);
        });

        VeilEventPlatform.INSTANCE.onVeilRegisterFixedBuffers(registry -> {
            registry.registerFixedBuffer(VeilRenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES, AeroRenderTypes.levitite());
            registry.registerFixedBuffer(VeilRenderLevelStageEvent.Stage.AFTER_WEATHER, AeroRenderTypes.levititeGhosts());
        });
    }
}

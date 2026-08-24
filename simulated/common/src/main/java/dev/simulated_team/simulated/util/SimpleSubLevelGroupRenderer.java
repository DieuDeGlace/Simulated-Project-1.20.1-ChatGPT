package dev.simulated_team.simulated.util;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.entity.EntitySubLevelUtil;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.mixinhelpers.sublevel_render.vanilla.VanillaSubLevelBlockEntityRenderer;
import dev.ryanhcode.sable.mixinterface.BlockEntityRenderDispatcherExtension;
import dev.ryanhcode.sable.neoforge.mixinhelper.compatibility.flywheel.SubLevelEmbedding;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.render.SubLevelRenderData;
import dev.ryanhcode.sable.sublevel.render.dispatcher.SubLevelRenderDispatcher;
import dev.ryanhcode.sable.sublevel.render.dispatcher.VanillaSubLevelRenderDispatcher;
import dev.ryanhcode.sable.sublevel.render.vanilla.VanillaSingleSubLevelRenderData;
import dev.simulated_team.simulated.mixin_interface.diagram.LightTextureExtension;
import dev.simulated_team.simulated.mixin_interface.diagram.VisualManagerExtension;
import dev.simulated_team.simulated.mixin_interface.diagram.VisualizationManagerExtension;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.framebuffer.AdvancedFbo;
import foundry.veil.impl.client.render.LevelPerspectiveCamera;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.*;

import java.util.Collection;
import java.util.List;

public class SimpleSubLevelGroupRenderer {
    private static final LevelPerspectiveCamera CAMERA = new LevelPerspectiveCamera();
    private static final Matrix4f TRANSFORM = new Matrix4f();
    private static final Matrix4f BACKUP_PROJECTION = new Matrix4f();
    private static final Matrix4f BACKUP_CAMERA_PROJECTION = new Matrix4f();
    private static final Matrix4f BACKUP_CAMERA_VIEW = new Matrix4f();
    private static final Vector3f BACKUP_CAMERA_POSITION = new Vector3f();
    public static boolean RENDERING_SIMPLE = false;

    /**
     * @return the chain of sub-levels that should render with a given sub-level into a diagram
     */
    public static Collection<ClientSubLevel> getRenderedChain(final ClientSubLevel subLevel) {
        final ObjectOpenHashSet<ClientSubLevel> visited = new ObjectOpenHashSet<>();
        final ObjectOpenHashSet<ClientSubLevel> frontier = new ObjectOpenHashSet<>();

        frontier.add(subLevel);

        while (!frontier.isEmpty()) {
            final ClientSubLevel current = frontier.iterator().next();

            frontier.remove(current);
            visited.add(current);

            final Iterable<SubLevel> intersecting = Sable.HELPER.getAllIntersecting(current.getLevel(), new BoundingBox3d(current.boundingBox()));

            // Intersecting dependencies
            for (final SubLevel neighbor : intersecting) {
                final ClientSubLevel serverNeighbor = (ClientSubLevel) neighbor;

                if (!visited.contains(serverNeighbor)) {
                    frontier.add(serverNeighbor);
                }
            }
        }

        return visited;
    }

    public static void renderChain(final SubLevel subLevel, final AdvancedFbo fbo, final Matrix4f modelView, final Matrix4f projectionMat, final Vector3d cameraPosition, final Quaternionf orientation, final float partialTicks) {
        final ClientSubLevel clientSubLevel = (ClientSubLevel) subLevel;
        final ClientLevel level = clientSubLevel.getLevel();
        final Collection<ClientSubLevel> subLevels = SimpleSubLevelGroupRenderer.getRenderedChain(clientSubLevel);

        renderGroup(level, subLevels, fbo, modelView, projectionMat, cameraPosition, orientation, partialTicks, true);
    }

    public static void renderGroup(final ClientLevel level, final Collection<ClientSubLevel> subLevels, final AdvancedFbo fbo, final Matrix4f modelView, final Matrix4f projectionMat, final Vector3d cameraPosition, final Quaternionf orientation, final float partialTicks, final boolean renderPlayers) {
        // Finish anything previously being rendered for safety
        final MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        bufferSource.endBatch();

        if (subLevels.isEmpty()) {
            AdvancedFbo.unbind();
            return;
        }

        final Minecraft minecraft = Minecraft.getInstance();
        final GameRenderer gameRenderer = minecraft.gameRenderer;
        final LightTexture lightTexture = gameRenderer.lightTexture();
        final VanillaSubLevelBlockEntityRenderer beRenderer = new VanillaSubLevelBlockEntityRenderer(minecraft.getBlockEntityRenderDispatcher(), minecraft.renderBuffers(), new Long2ObjectOpenHashMap<>());

        CAMERA.setup(cameraPosition, null, minecraft.level, orientation);

        final PoseStack poseStack = new PoseStack();
        poseStack.mulPoseMatrix(TRANSFORM.set(modelView));
        poseStack.mulPose(CAMERA.rotation());

        BACKUP_PROJECTION.set(RenderSystem.getProjectionMatrix());
        gameRenderer.resetProjectionMatrix(TRANSFORM.set(projectionMat));

        final var matrices = VeilRenderSystem.renderer().getCameraMatrices();
        BACKUP_CAMERA_PROJECTION.set(matrices.getProjectionMatrix());
        BACKUP_CAMERA_VIEW.set(matrices.getViewMatrix());
        BACKUP_CAMERA_POSITION.set(matrices.getCameraPosition());
        final float backupNearPlane = matrices.getNearPlane();
        final float backupFarPlane = matrices.getFarPlane();
        matrices.update(projectionMat, poseStack.last().pose(),
                new Vector3f((float) cameraPosition.x(), (float) cameraPosition.y(), (float) cameraPosition.z()),
                0.05f, gameRenderer.getDepthFar());

        final PoseStack modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.pushPose();
        modelViewStack.setIdentity();
        modelViewStack.mulPoseMatrix(poseStack.last().pose());
        RenderSystem.applyModelViewMatrix();

        fbo.bind(true);

        try {
            Lighting.setupNetherLevel(RenderSystem.getModelViewMatrix());
            ((LightTextureExtension) lightTexture).simulated$makeDiagramLightTexture(0.65f);

            SimpleSubLevelGroupRenderer.RENDERING_SIMPLE = true;
            for (final RenderType layer : RenderType.chunkBufferLayers()) {
                layer.setupRenderState();
                final ShaderInstance shader = RenderSystem.getShader();
                ShaderInstanceCompat.setDefaultUniforms(shader, VertexFormat.Mode.QUADS, RenderSystem.getModelViewMatrix(), projectionMat, minecraft.getWindow());
                shader.apply();
                SubLevelRenderDispatcher.get().renderSectionLayer(subLevels, layer, shader, cameraPosition.x, cameraPosition.y, cameraPosition.z, RenderSystem.getModelViewMatrix(), projectionMat, partialTicks);

                // single block sub-levels
                final VertexConsumer consumer = bufferSource.getBuffer(layer);

                for (final ClientSubLevel sublevel : subLevels) {
                    final SubLevelRenderData data = sublevel.getRenderData();

                    if (!(data instanceof final VanillaSingleSubLevelRenderData singleRenderData)) {
                        continue;
                    }

                    singleRenderData.renderSingleBlock(layer, consumer, modelView, cameraPosition.x, cameraPosition.y, cameraPosition.z);
                }

                bufferSource.endBatch(layer);
                shader.clear();
                layer.clearRenderState();
            }
            ((LightTextureExtension) lightTexture).simulated$makeDiagramLightTexture(1.0f);
            SimpleSubLevelGroupRenderer.RENDERING_SIMPLE = false;

            final VisualizationManager visualizationManager = VisualizationManager.get(level);

            // Render block-entities with visuals normally
            if (visualizationManager instanceof final VisualizationManagerExtension extension) {
                extension.sable$setDrawingDiagram(true);

                for (final ClientSubLevel beSubLevel : subLevels) {
                    final BlockEntityRenderDispatcherExtension dispatcher = (BlockEntityRenderDispatcherExtension) beRenderer.getBlockEntityRenderDispatcher();

                    final SubLevelEmbedding embeddingInfo = ((VisualManagerExtension) visualizationManager.blockEntities()).sable$getBEEmbeddingInfo(beSubLevel);

                    if (embeddingInfo == null) {
                        continue;
                    }

                    final Vector3d chunkOffset = new Vector3d();
                    final Matrix4f transformation = new Matrix4f();
                    final Matrix4f transformationInverse = new Matrix4f();

                    final SubLevelRenderData data = beSubLevel.getRenderData();

                    beSubLevel.renderPose().rotationPoint().negate(chunkOffset.zero());
                    data.getTransformation(cameraPosition.x, cameraPosition.y, cameraPosition.z, transformation);

                    final Vector3f c = transformation.invert(transformationInverse).transformPosition(new Vector3f());
                    dispatcher.sable$setCameraPosition(new Vec3(c.x - chunkOffset.x(), c.y - chunkOffset.y(), c.z - chunkOffset.z()));

                    final PoseStack beMatrices = new PoseStack();
                    beMatrices.pushPose();
                    beMatrices.mulPoseMatrix(transformation);
                    beRenderer.renderBlockEntities(embeddingInfo.blockEntities(), beMatrices, partialTicks, -chunkOffset.x, -chunkOffset.y, -chunkOffset.z);
                    beMatrices.popPose();

                    dispatcher.sable$setCameraPosition(null);
                }
            }

            // Render normal block-entities
            SubLevelRenderDispatcher.get().renderBlockEntities(subLevels, beRenderer, cameraPosition.x, cameraPosition.y, cameraPosition.z, partialTicks);

            for (final ClientSubLevel entitySubLevel : subLevels) {
                final List<Entity> entities = level.getEntitiesOfClass(Entity.class, entitySubLevel.getPlot().getBoundingBox().toAABB().inflate(16.0));

                final PoseStack entityPoseStack = new PoseStack();
                entityPoseStack.pushPose();
                entityPoseStack.mulPoseMatrix(TRANSFORM.set(modelView));

                for (final Entity entity : entities) {
                    if (Sable.HELPER.getContaining(entity) != entitySubLevel && Sable.HELPER.getTrackingOrVehicleSubLevel(entity) != entitySubLevel) {
                        continue;
                    }

                    if (!renderPlayers && entity instanceof Player) {
                        continue;
                    }

                    final float partialTick = minecraft.getFrameTime();

                    minecraft.levelRenderer.renderEntity(entity, cameraPosition.x, cameraPosition.y, cameraPosition.z, partialTick, entityPoseStack, bufferSource);
                }
                entityPoseStack.popPose();
            }

            if (visualizationManager instanceof final VisualizationManagerExtension extension) {
                extension.sable$setDrawingDiagram(false);
            }

            bufferSource.endBatch();
        } finally {
            modelViewStack.popPose();
            RenderSystem.applyModelViewMatrix();

            if (level.effects().constantAmbientLight()) {
                Lighting.setupNetherLevel(RenderSystem.getModelViewMatrix());
            } else {
                Lighting.setupLevel(RenderSystem.getModelViewMatrix());
            }

            matrices.update(BACKUP_CAMERA_PROJECTION, BACKUP_CAMERA_VIEW, BACKUP_CAMERA_POSITION, backupNearPlane, backupFarPlane);

            gameRenderer.resetProjectionMatrix(BACKUP_PROJECTION);
            AdvancedFbo.unbind();

            lightTexture.updateLightTexture(partialTicks);
        }
    }
}

package dev.simulated_team.simulated.content.physics_staff;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.item.render.CustomRenderedItemModel;
import com.simibubi.create.foundation.item.render.CustomRenderedItemModelRenderer;
import com.simibubi.create.foundation.item.render.PartialItemModelRenderer;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.SimulatedClient;
import dev.simulated_team.simulated.index.SimPartialModels;
import dev.simulated_team.simulated.index.SimRenderTypes;
import dev.simulated_team.simulated.util.SimDistUtil;
import dev.simulated_team.simulated.util.SimMathUtils;
import foundry.veil.Veil;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.irisshaders.iris.Iris;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.*;
import java.lang.Math;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class PhysicsStaffItemRenderer extends CustomRenderedItemModelRenderer {
    private static final Vector3d focusPos = new Vector3d();
    private static final Matrix4f itemProjMat = new Matrix4f();
    private static final AtomicInteger FOCUS_TRACES = new AtomicInteger();

    public static Vec3 getFirstPersonFocusPos(final float pt) {
        final GameRenderer gameRenderer = Minecraft.getInstance().gameRenderer;
        final Camera camera = gameRenderer.getMainCamera();

        final Matrix4f worldProjection = gameRenderer.getProjectionMatrix(gameRenderer.getFov(camera, pt, true));
        return getFirstPersonFocusPos(pt, worldProjection, null);
    }

    public static Vec3 getFirstPersonFocusPos(final float pt, final Matrix4fc worldProjection,
                                               final Matrix4fc worldViewPose) {
        final GameRenderer gameRenderer = Minecraft.getInstance().gameRenderer;
        final Camera camera = gameRenderer.getMainCamera();

        final Vector3d focusPoint = new Vector3d(PhysicsStaffItemRenderer.focusPos);
        final Vector4f itemClip = new Vector4f((float) focusPoint.x, (float) focusPoint.y, (float) focusPoint.z, 1.0f);

        itemProjMat.transform(itemClip);
        final Vector4f worldView = new Matrix4f(worldProjection).invert()
                .transform(new Vector4f(itemClip));
        if (Math.abs(worldView.w) > 1.0e-6f) {
            worldView.div(worldView.w);
        }

        final Vector4f output = new Vector4f(worldView.x, worldView.y, worldView.z, 1.0f);
        if (worldViewPose != null) {
            new Matrix4f(worldViewPose).invert().transform(output);
            if (Math.abs(output.w) > 1.0e-6f) {
                output.div(output.w);
            }
        } else {
            camera.rotation().transform(new Vector3d(output.x, output.y, output.z), focusPoint);
            output.set((float) focusPoint.x, (float) focusPoint.y, (float) focusPoint.z, 1.0f);
        }

        if (FOCUS_TRACES.getAndIncrement() < 12) {
            final Vector4f checkView = new Vector4f(output);
            if (worldViewPose != null) {
                worldViewPose.transform(checkView);
            } else {
                camera.rotation().transformInverse(checkView);
            }
            final Vector4f checkClip = worldProjection.transform(checkView, new Vector4f());
            final double screenError = ndcDistance(itemClip, checkClip);
            Simulated.LOGGER.info("SIMULATED-STAFF-FOCUS: partialTick={}, exactPose={}, itemView={}, "
                            + "itemNdc=[{},{}], worldView=[{},{},{}], outputRelative=[{},{},{}], "
                            + "checkNdc=[{},{}], screenError={}, itemProjection=[{},{},{},{}], "
                            + "worldProjection=[{},{},{},{}]",
                    pt, worldViewPose != null, focusPoint,
                    ndcX(itemClip), ndcY(itemClip), worldView.x, worldView.y, worldView.z,
                    output.x, output.y, output.z, ndcX(checkClip), ndcY(checkClip), screenError,
                    itemProjMat.m00(), itemProjMat.m11(), itemProjMat.m20(), itemProjMat.m21(),
                    worldProjection.m00(), worldProjection.m11(), worldProjection.m20(), worldProjection.m21());
        }

        return new Vec3(output.x, output.y, output.z);
    }

    private static double ndcX(final Vector4fc value) {
        return Math.abs(value.w()) > 1.0e-6f ? value.x() / value.w() : Double.NaN;
    }

    private static double ndcY(final Vector4fc value) {
        return Math.abs(value.w()) > 1.0e-6f ? value.y() / value.w() : Double.NaN;
    }

    private static double ndcDistance(final Vector4fc first, final Vector4fc second) {
        return Math.hypot(ndcX(first) - ndcX(second), ndcY(first) - ndcY(second));
    }

    @Override
    protected void render(final ItemStack stack, final CustomRenderedItemModel model, final PartialItemModelRenderer renderer, final ItemDisplayContext context, final PoseStack ms,
                          final MultiBufferSource buffer, final int light, final int overlay) {
        float openAmount = 0;
        float cubeScale = 0;
        final PhysicsStaffClientHandler clientHandler = SimulatedClient.PHYSICS_STAFF_CLIENT_HANDLER;
        final Minecraft minecraft = Minecraft.getInstance();

        final float partialTicks = AnimationTickHolder.getPartialTicks();

        // only staffs held in a players hand should attempt to render opened
        final Player player = SimDistUtil.getClientPlayer();

        if (player != null && (context.firstPerson() || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND || context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND)) {
            if (player.getMainHandItem() == stack || player.getOffhandItem() == stack) {
                // smooth lerping for your own staff you're holding
                openAmount = Mth.lerp(partialTicks, clientHandler.previousExtension, clientHandler.extension);
                cubeScale = Mth.lerp(partialTicks, clientHandler.previousCubeScale, clientHandler.cubeScale);
            } else {
                // look up existing beams for their associated player
                for (final UUID playerUUID : clientHandler.beams.keySet()) {
                    final Player otherPlayer = minecraft.level.getPlayerByUUID(playerUUID);
                    if (otherPlayer != null && (otherPlayer.getMainHandItem() == stack || otherPlayer.getOffhandItem() == stack)) {
                        openAmount = Mth.lerp(partialTicks, clientHandler.beams.get(playerUUID).previousExtension, clientHandler.beams.get(playerUUID).extension);
                        cubeScale = Mth.lerp(partialTicks, clientHandler.beams.get(playerUUID).previousCubeScale, clientHandler.beams.get(playerUUID).cubeScale);
                        break;
                    }
                }
            }
        }

        final float tiltAmount = Mth.lerp(partialTicks, clientHandler.previousTilt, clientHandler.tilt);
        final Quaternionf utilQuat = new Quaternionf();

        final boolean irisCompatibleModLoaded = Veil.platform().isModLoaded("iris") || Veil.platform().isModLoaded("oculus");
        final boolean shadersActive = irisCompatibleModLoaded && Iris.isPackInUseQuick();

        if (context.firstPerson()) {
            if (clientHandler.getDragSession() != null) {
                final PhysicsStaffClientHandler.ClientDragSession dragSession = clientHandler.getDragSession();

                final Quaternionf rotation = minecraft.gameRenderer.getMainCamera().rotation();
                final Vector3d globalAnchor = ((ClientSubLevel) dragSession.dragSubLevel()).renderPose(partialTicks).transformPosition(new Vector3d(dragSession.dragLocalAnchor()));
                final Vector3d dirToAnchor = globalAnchor.sub(JOMLConversion.toJOML(minecraft.gameRenderer.getMainCamera().getPosition())).normalize();
                rotation.transformInverse(dirToAnchor);

                final Quaternionf quat = SimMathUtils.getQuaternionfFromVectorRotation(new Vector3d(0.0, 0.0, -1.0), dirToAnchor);
                ms.mulPose(utilQuat.identity().rotateY(-Mth.HALF_PI));
                ms.mulPose(quat.slerp(utilQuat.identity(), 0.6f));
                ms.mulPose(utilQuat.identity().rotateY(Mth.HALF_PI));
            }
            final float tiltMultiplier = context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND ? -1.0f : 1.0f;

            ms.mulPose(utilQuat.identity().rotateZ((float) Math.toRadians((tiltAmount * 0.5 + 0.5) * -61.0f) * tiltMultiplier));
        }


        renderer.render(model.getOriginalModel(), Sheets.cutoutBlockSheet(), light);

        renderer.render(SimPartialModels.PHYSICS_STAFF_CORE.get(), SimRenderTypes.itemGlowingSolid(shadersActive), LightTexture.FULL_BRIGHT);
        renderer.render(SimPartialModels.PHYSICS_STAFF_CORE_GLOW.get(), SimRenderTypes.itemGlowingTranslucent(shadersActive), LightTexture.FULL_BRIGHT);
        final float worldTime = AnimationTickHolder.getRenderTime() / 20;

        ms.pushPose();
        ms.translate(0, 6.5 / 16.0, 0);
        renderer.render(SimPartialModels.PHYSICS_STAFF_RING.get(), Sheets.cutoutBlockSheet(), light);
        ms.popPose();

        ms.translate(0, 9 / 16.0, 0);
        for (int i = 0; i < 2; i++) {
            ms.pushPose();
            ms.mulPose(Axis.YP.rotationDegrees(i * 180));
            ms.translate(-3 / 16.0, 0, 0);
            ms.mulPose(Axis.ZP.rotationDegrees(openAmount * 20));
            renderer.render(SimPartialModels.PHYSICS_STAFF_SIGMA.get(), Sheets.cutoutBlockSheet(), light);
            ms.popPose();
        }
        ms.translate(0, 6 / 16.0, 0);

        if (context.firstPerson()) {
            if (clientHandler.getDragSession() != null) {
                clientHandler.lastCubeOrientation.set(clientHandler.getDragSession().dragOrientation());
            }

            final Matrix4f m = new Matrix4f(ms.last().pose());
            m.m30(0).m31(0).m32(0);
            m.invert();
            m.rotate(clientHandler.lastCubeOrientation);
            ms.mulPoseMatrix(m);
        }

        cubeScale = Mth.lerp(cubeScale, -0.05f, 1f);
        cubeScale = Mth.clamp(cubeScale, 0, 1);
        cubeScale *= 0.8f;
        ms.scale(cubeScale, cubeScale, cubeScale);
        renderer.render(SimPartialModels.PHYSICS_STAFF_INNER_CUBE.get(), SimRenderTypes.itemGlowingSolid(shadersActive), LightTexture.FULL_BRIGHT);

        if (context.firstPerson()) {
            final Vector3f focusPoint = new Vector3f();
            ms.last().pose().transformPosition(focusPoint);

            itemProjMat.set(RenderSystem.getProjectionMatrix());
            focusPos.set(focusPoint.x, focusPoint.y, focusPoint.z);
        }

        ms.scale(1.2f, 1.2f, 1.2f);
        renderer.render(SimPartialModels.PHYSICS_STAFF_OUTER_CUBE.get(), SimRenderTypes.itemGlowingTranslucent(shadersActive), LightTexture.FULL_BRIGHT);

        // Iris doesn't allow individual render types to be ended, so all batches must be ended for the translucent parts to draw correctly
        if (irisCompatibleModLoaded && !shadersActive) {
            Minecraft.getInstance().renderBuffers().bufferSource().endBatch();
        }
    }
}

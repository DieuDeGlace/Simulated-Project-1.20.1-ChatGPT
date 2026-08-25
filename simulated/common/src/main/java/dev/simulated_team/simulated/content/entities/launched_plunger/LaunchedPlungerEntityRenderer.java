package dev.simulated_team.simulated.content.entities.launched_plunger;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.lib.transform.PoseTransformStack;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.SubLevelHelper;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.content.items.plunger_launcher.PlungerLauncherItemRenderer;
import dev.simulated_team.simulated.index.SimPartialModels;
import dev.simulated_team.simulated.index.SimRenderTypes;
import dev.simulated_team.simulated.util.CatmulRomSpline;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.shader.program.ShaderProgram;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.joml.*;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import java.lang.Math;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class LaunchedPlungerEntityRenderer extends EntityRenderer<LaunchedPlungerEntity> {

    private static final Quaternionf POSITIVE_Y = new Quaternionf().setAngleAxis(Math.PI / 2, 1, 0, 0);
    private static final Quaternionf NEGATIVE_Y = new Quaternionf().setAngleAxis(-Math.PI / 2, 1, 0, 0);
    private static final Matrix4f FRUSTUM = new Matrix4f();
    private static final Matrix4f PROJECTION = new Matrix4f();

    private static final Quaternionf ORIENTATION = new Quaternionf();
    private static final Quaternionf NEXT_ORIENTATION = new Quaternionf();
    private static final Vector3f POS = new Vector3f();
    private static final Vector3f NORMAL = new Vector3f();
    private static final Vector3f NEXT_NORMAL = new Vector3f();
    private static final Vector3f FACE_NORMAL = new Vector3f();
    private static final Vector3f TARGET = new Vector3f();
    private static final Vector3f SELF = new Vector3f();

    private static final List<Vec3> CABLE_POINTS = new ArrayList<>();
    private static final List<Vec3> PREV_CABLE_POINTS = new ArrayList<>();

    private static final BlockPos.MutableBlockPos LIGHT_POS = new BlockPos.MutableBlockPos();
    private static final AtomicInteger RENDER_TRACES = new AtomicInteger();
    private static final AtomicInteger ROPE_TRACES = new AtomicInteger();
    private static final AtomicInteger PAIRED_TRACES = new AtomicInteger();
    private static final AtomicInteger PAIRED_GEOMETRY_TRACES = new AtomicInteger();
    private static final AtomicInteger SETTLED_GEOMETRY_TRACES = new AtomicInteger();
    private static final AtomicInteger FOCUS_TRACES = new AtomicInteger();

    public LaunchedPlungerEntityRenderer(final EntityRendererProvider.Context context) {
        super(context);
    }


    public static Vec3 getFirstPersonFocusPos(final float pt) {
        final GameRenderer gameRenderer = Minecraft.getInstance().gameRenderer;
        final Camera camera = gameRenderer.getMainCamera();

        final Matrix4f worldProjection = gameRenderer.getProjectionMatrix(gameRenderer.getFov(camera, pt, true));
        return getFirstPersonFocusPos(pt, worldProjection, null);
    }

    private static Vec3 getFirstPersonFocusPos(final float pt, final Matrix4fc worldProjection,
                                                final Matrix4fc worldViewPose) {
        final GameRenderer gameRenderer = Minecraft.getInstance().gameRenderer;
        final Camera camera = gameRenderer.getMainCamera();

        final Vector3d focusPoint = new Vector3d(PlungerLauncherItemRenderer.focusPos);
        final Vector4f itemClip = new Vector4f((float) focusPoint.x, (float) focusPoint.y, (float) focusPoint.z, 1.0f);

        // First match the held-item point in clip space. Then use the exact world projection
        // and pose active where the rope is submitted. This includes shader projection changes,
        // view bobbing and hurt-camera transforms that camera.rotation() alone cannot undo.
        PlungerLauncherItemRenderer.itemProjMat.transform(itemClip);
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
            final Vector3d cameraRelative = new Vector3d(output.x, output.y, output.z);
            camera.rotation().transform(cameraRelative);
            final Vec3 cameraPosition = camera.getPosition();
            output.set((float) (cameraRelative.x + cameraPosition.x),
                    (float) (cameraRelative.y + cameraPosition.y),
                    (float) (cameraRelative.z + cameraPosition.z), 1.0f);
        }

        if (FOCUS_TRACES.getAndIncrement() < 12) {
            final Vector4f checkView = new Vector4f(output);
            if (worldViewPose != null) {
                worldViewPose.transform(checkView);
            } else {
                final Vec3 cameraPosition = camera.getPosition();
                checkView.sub((float) cameraPosition.x, (float) cameraPosition.y,
                        (float) cameraPosition.z, 0.0f);
                camera.rotation().transformInverse(checkView);
            }
            final Vector4f checkClip = worldProjection.transform(checkView, new Vector4f());
            final double screenError = ndcDistance(itemClip, checkClip);
            Simulated.LOGGER.info("SIMULATED-PLUNGER-FOCUS: partialTick={}, exactPose={}, itemView={}, "
                            + "itemNdc=[{},{}], worldView=[{},{},{}], output=[{},{},{}], "
                            + "checkNdc=[{},{}], screenError={}, itemProjection=[{},{},{},{}], "
                            + "worldProjection=[{},{},{},{}]",
                    pt, worldViewPose != null, focusPoint,
                    ndcX(itemClip), ndcY(itemClip), worldView.x, worldView.y, worldView.z,
                    output.x, output.y, output.z, ndcX(checkClip), ndcY(checkClip), screenError,
                    PlungerLauncherItemRenderer.itemProjMat.m00(), PlungerLauncherItemRenderer.itemProjMat.m11(),
                    PlungerLauncherItemRenderer.itemProjMat.m20(), PlungerLauncherItemRenderer.itemProjMat.m21(),
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
    public void render(final LaunchedPlungerEntity entity, final float f, final float pt, final PoseStack poseStack, final MultiBufferSource multiBufferSource, final int light) {
        super.render(entity, f, pt, poseStack, multiBufferSource, light);

        // render to sublevel and invert rotate rope to be accurate
        final LaunchedPlungerEntity other = entity.getOther();

        Vec3 selfNormal = Vec3.ZERO;
        Vec3 perpendicularNormal = Vec3.ZERO;
        final Direction dir = entity.getData(LaunchedPlungerEntity.PLUNGED_DIRECTION);
        if (entity.isPlunged()) {
            selfNormal = Vec3.atLowerCornerOf(dir.getNormal());
            if (dir.getAxis().isHorizontal()) {
                perpendicularNormal = Vec3.atLowerCornerOf(Direction.UP.getNormal());
            } else {
                perpendicularNormal = Vec3.atLowerCornerOf(Direction.NORTH.getNormal());
            }
        } else {
            selfNormal = entity.calculateViewVector(-Mth.lerp(pt, entity.xRotO, entity.getXRot()),-Mth.lerp(pt, entity.yRotO, entity.getYRot())).reverse();
            perpendicularNormal = entity.calculateViewVector(-Mth.lerp(pt, entity.xRotO, entity.getXRot()),-Mth.lerp(pt, entity.yRotO, entity.getYRot())-90).reverse();
        }

        poseStack.pushPose();
        Vec3 oldPos = new Vec3(entity.xo, entity.yo, entity.zo);
        Vec3 newPos = entity.position();
        final Vec3 localOldPos = oldPos;
        final Vec3 localNewPos = newPos;
        final float scalingFactor = 0.6f;
        final SubLevel subLevel = Sable.HELPER.getContainingClient(newPos);
        if (subLevel != null) {
            final ClientSubLevel clientSubLevel = (ClientSubLevel) subLevel;
            final Pose3dc clientPos = clientSubLevel.renderPose(pt);
            newPos = clientPos.transformPosition(newPos);
            selfNormal = clientPos.transformNormal(selfNormal);
            perpendicularNormal = clientPos.transformNormal(perpendicularNormal);

            final Quaterniondc quaterniondc = clientPos.orientation();
            poseStack.mulPose(new Quaternionf(quaterniondc.x(), quaterniondc.y(), quaterniondc.z(), quaterniondc.w()).conjugate());
        }

        if (RENDER_TRACES.getAndIncrement() < 16) {
            Simulated.LOGGER.info("SIMULATED-PLUNGER-RENDER: entity={}, tick={}, partialTick={}, localOld={}, "
                            + "localNew={}, projectedNew={}, subLevel={}, other={}, poseTranslation=[{},{},{}], "
                            + "poseDet={}, finite={}",
                    entity.getId(), entity.tickCount, pt, oldPos, entity.position(), newPos,
                    subLevel == null ? "none" : subLevel.getUniqueId(), other == null ? "none" : other.getId(),
                    poseStack.last().pose().m30(), poseStack.last().pose().m31(), poseStack.last().pose().m32(),
                    poseStack.last().pose().determinant(), poseStack.last().pose().isFinite());
        }

        final SubLevel oldSubLevel = Sable.HELPER.getContainingClient(oldPos);
        if (oldSubLevel != null) {
            final ClientSubLevel clientSubLevel = (ClientSubLevel) oldSubLevel;
            final Pose3dc clientPos = clientSubLevel.renderPose(pt);
            oldPos = clientPos.transformPosition(oldPos);
        }
        final Vec3 renderPos = oldPos.lerp(newPos, pt);
        final Vec3 pos = renderPos.add(selfNormal.scale(scalingFactor));

        poseStack.translate(-renderPos.x, -renderPos.y, -renderPos.z);

        final Vec3 target;
        if (other != null) {
            Vec3 otherNormal = Vec3.ZERO;
            if (other.isPlunged()) {
                final Direction otherDir = other.getData(LaunchedPlungerEntity.PLUNGED_DIRECTION);
                otherNormal = Vec3.atLowerCornerOf(otherDir.getNormal());
            } else {
                otherNormal = other.calculateViewVector(-Mth.lerp(pt, other.xRotO, other.getXRot()),-Mth.lerp(pt, other.yRotO, other.getYRot())).reverse();
            }

            Vec3 targetOldPos = new Vec3(other.xo, other.yo, other.zo);
            Vec3 targetNewPos = other.position();
            if (other.isRemoved()) {
                targetOldPos = entity.getEntityData().get(LaunchedPlungerEntity.TARGET_POS);
                targetNewPos = targetOldPos;
            }
            final Vec3 targetLocalOldPos = targetOldPos;
            final Vec3 targetLocalNewPos = targetNewPos;
            final SubLevel targetSublevel = Sable.HELPER.getContainingClient(targetNewPos);
            if (targetSublevel != null) {
                targetNewPos = ((ClientSubLevel) targetSublevel).renderPose(pt).transformPosition(targetNewPos);
                otherNormal = ((ClientSubLevel) targetSublevel).renderPose(pt).transformNormal(otherNormal);
            }
            // Resolve the previous endpoint in its own coordinate space. Looking up the
            // interpolated entity position can fall between plot regions and then blend an
            // unprojected old endpoint with a projected new endpoint.
            final SubLevel targetOldSublevel = Sable.HELPER.getContainingClient(targetOldPos);
            if (targetOldSublevel != null) {
                targetOldPos = ((ClientSubLevel) targetOldSublevel).renderPose(pt).transformPosition(targetOldPos);
            }
            target = targetOldPos.lerp(targetNewPos, pt).add(otherNormal.scale(scalingFactor));

            if (PAIRED_TRACES.get() < 16 && PAIRED_TRACES.getAndIncrement() < 16) {
                Simulated.LOGGER.info("SIMULATED-PLUNGER-PAIR: entity={}, other={}, tick={}, partialTick={}, "
                                + "selfLocalOld={}, selfLocalNew={}, selfProjectedOld={}, selfProjectedNew={}, "
                                + "targetLocalOld={}, targetLocalNew={}, targetProjectedOld={}, targetProjectedNew={}, "
                                + "selfSubLevel={}, targetOldSubLevel={}, targetNewSubLevel={}, start={}, end={}, finite={}",
                        entity.getId(), other.getId(), entity.tickCount, pt,
                        localOldPos, localNewPos, oldPos, newPos,
                        targetLocalOldPos, targetLocalNewPos, targetOldPos, targetNewPos,
                        subLevel == null ? "none" : subLevel.getUniqueId(),
                        targetOldSublevel == null ? "none" : targetOldSublevel.getUniqueId(),
                        targetSublevel == null ? "none" : targetSublevel.getUniqueId(),
                        pos, target, isFinite(pos) && isFinite(target));
            }
        } else {
            final Entity owner = entity.getOwner();
            if (entity.getData(LaunchedPlungerEntity.OTHER_PLUNGER).isEmpty() && owner == Minecraft.getInstance().player && Minecraft.getInstance().options.getCameraType().isFirstPerson()) {
                target = getFirstPersonFocusPos(pt, RenderSystem.getProjectionMatrix(), poseStack.last().pose());
            } else {
                if (owner instanceof final AbstractClientPlayer player && entity.getData(LaunchedPlungerEntity.OTHER_PLUNGER).isEmpty()) {
                    final PlayerRenderer playerrenderer = (PlayerRenderer) Minecraft.getInstance().getEntityRenderDispatcher()
                            .getRenderer(player);
                    float headYDirection = Mth.lerp(pt, player.yHeadRotO, player.yHeadRot);
                    final float bodyRotation = Mth.lerp(pt, player.yBodyRotO, player.yBodyRot);
                    final float bodyDifference = Math.abs(headYDirection - bodyRotation) / 50f;
                    final float headXDirection = Mth.lerp(pt, player.xRotO, player.getXRot());
                    final float lookDelta = Math.abs(Mth.map(headXDirection, 90, 0, 1f, 0f));
                    headYDirection = Mth.lerp(lookDelta, headYDirection, bodyRotation);
                    final Vec3 viewDirection = player.calculateViewVector(headXDirection, headYDirection);
                    final Vec3 handDirection = player.calculateViewVector(0, headYDirection + 90.0f);
                    target = player.getPosition(pt).add(0.0, 1.28, 0.0).add(viewDirection.scale(0.875)).add(handDirection.scale(Math.abs(Mth.map(headXDirection, 90, 0, 0.325f, 0f)) * (1)));
                } else {
                    target = Vec3.ZERO;
                }
            }
        }

        final Vector3f firstRotation = new Vector3f();
        final Vector3f secondRotation = new Vector3f();
        final Vector3f finalRotation = new Vector3f();

        if ((entity.getData(LaunchedPlungerEntity.IS_FIRST) || other == null || other.isRemoved()) && !target.equals(Vec3.ZERO)) {
            final float renderTime = (entity.tickCount + pt + entity.getAnimationOffset());
            final List<Vec3> points = new ObjectArrayList<>();
            final Vec3 start = pos;
            final Vec3 end = target;
            final Vec3 toTarget = end.subtract(start);
            final Vec3 normalizedScalar = toTarget.normalize();
            final float length = (float) renderPos.distanceTo(target);
            final float animationScale = Math.max(0, 1 - (entity.tickCount + pt) / 40f
                    - ((entity.getPlungedTime() + (entity.getPlungedTime() > 0 ? pt : 0)) / 8f));
            points.add(start);
           
            if (length < 1000.0) {
                for (float j = 0.01f; j < length; j += 0.5f) {
                    finalRotation.set(0, 0, 0);
                    final float delta = j / length;
                    final Vec3 point = start.add(toTarget.scale(delta));
                    firstRotation.set(Math.cos((renderTime / 10) + j) * (1 - Math.abs(normalizedScalar.x)), Math.cos((renderTime / 10) + j) * (1 - Math.abs(normalizedScalar.y)), Math.cos((renderTime / 10) + j / 2) * (1 - Math.abs(normalizedScalar.z)));
                    secondRotation.set(Math.sin((renderTime / 10) + j / 4) * (1 - Math.abs(normalizedScalar.x)) * 2, Math.sin((renderTime / 10) + j / 4) * (1 - Math.abs(normalizedScalar.y)), Math.sin((renderTime / 10) + j / 4) * (1 - Math.abs(normalizedScalar.z)));
                    finalRotation.add(firstRotation);
                    finalRotation.add(secondRotation);
                    finalRotation.mul(animationScale); // Scales down the animation over time
                    finalRotation.mul((float) (1.0 - Math.pow(2 * delta - 1, 2.0)));
                    points.add(point.subtract(finalRotation.x, finalRotation.y, finalRotation.z));
                }
            }

            points.add(start.add(toTarget));
            points.add(start.add(toTarget));

            renderRope(points, multiBufferSource, Minecraft.getInstance().level, poseStack,
                    entity.getId(), entity.tickCount, other != null, animationScale);
        }

        // Restore the entity-local pose after the rope has consumed the world-to-view
        // correction prepared above. The plunger model itself uses the normal entity pose.
        poseStack.popPose();

        final double distanceIncludingSublevels = pos.distanceTo(target);

        final PoseTransformStack stack = TransformStack.of(poseStack);
        //render launched plunger and spool
        poseStack.pushPose();
        if (entity.isPlunged()) {
            stack.rotate(dir.getRotation());
        } else {
            poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(pt, entity.yRotO, entity.getYRot()) - 90.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(pt, entity.xRotO, entity.getXRot())));
            stack.rotateZDegrees(90f);
        }
        stack.rotateXDegrees(-90);

        stack.scale(1.75f, 1.75f, 1.75f);
        stack.translate(0, 0, 2.5f / 16f);

        final VertexConsumer vb = multiBufferSource.getBuffer(RenderType.solid());
        final SuperByteBuffer body = CachedBuffers.partial(SimPartialModels.LAUNCHED_PLUNGER_BODY, Blocks.AIR.defaultBlockState());
        final SuperByteBuffer spool = CachedBuffers.partial(SimPartialModels.LAUNCHED_PLUNGER_SPOOL, Blocks.AIR.defaultBlockState());
        final SuperByteBuffer joint = CachedBuffers.partial(SimPartialModels.LAUNCHED_PLUNGER_JOINT, Blocks.AIR.defaultBlockState());

        stack.rotateZDegrees(90f);
        body.light(light).renderInto(poseStack, vb);

        FACE_NORMAL.set(selfNormal.x, selfNormal.y, selfNormal.z);
        SELF.set(pos.x, pos.y, pos.z);
        TARGET.set(target.x, target.y, target.z);
        TARGET.add(SELF.mul(-1)).normalize();
        POS.set(perpendicularNormal.x, perpendicularNormal.y, perpendicularNormal.z);

        float angle = 0;
        if (entity.getData(LaunchedPlungerEntity.IS_PLUNGED)) {
            angle = (float) (POS.angleSigned(TARGET, FACE_NORMAL) + Math.PI / 2f);
        }
        if (Float.isNaN(angle)) {
            angle = 0;
        }

        poseStack.pushPose();
        stack.rotateZDegrees((float) Math.toDegrees(angle));
        joint.light(light).renderInto(poseStack, vb);
        stack.translate(0, 0, 3f / 16f);
        stack.rotateXDegrees((float) distanceIncludingSublevels * 90f * 2.6f);
        spool.light(light).renderInto(poseStack, vb);
        poseStack.popPose();

        poseStack.popPose();
    }

    public static void renderRope(final List<Vec3> positions, final MultiBufferSource multiBufferSource,
                                  final BlockAndTintGetter level, final PoseStack poseStack,
                                  final int entityId, final int tickCount, final boolean paired,
                                  final float animationScale) {
        final Vec3 cameraPosition = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();

        final RenderType renderType = SimRenderTypes.rope();
        final VertexConsumer builder = multiBufferSource.getBuffer(renderType);

        if (ROPE_TRACES.getAndIncrement() < 16) {
            double maxCameraRelative = 0.0;
            for (final Vec3 position : positions) {
                maxCameraRelative = Math.max(maxCameraRelative, position.distanceTo(cameraPosition));
            }
            final ShaderInstance shader = RenderSystem.getShader();
            final ShaderProgram ropeProgram = VeilRenderSystem.renderer().getShaderManager()
                    .getShader(Simulated.path("rope/rope"));
            Simulated.LOGGER.info("SIMULATED-PLUNGER-ROPE: points={}, start={}, end={}, camera={}, "
                            + "maxCameraRelative={}, renderType={}, queuedShader={}, queuedGlProgram={}, "
                            + "veilProgramPresent={}, veilProgram={}, poseTranslation=[{},{},{}], "
                            + "poseDet={}, finite={}",
                    positions.size(), positions.get(0), positions.get(positions.size() - 1), cameraPosition,
                    maxCameraRelative, renderType, shader == null ? "null" : shader.getName(),
                    GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM), ropeProgram != null,
                    ropeProgram == null ? -1 : ropeProgram.getProgram(), poseStack.last().pose().m30(),
                    poseStack.last().pose().m31(), poseStack.last().pose().m32(),
                    poseStack.last().pose().determinant(), poseStack.last().pose().isFinite());
        }

        CABLE_POINTS.clear();
        for (final Vec3 position : positions) {
            // Keep the spline in projected world space. The pose prepared by render()
            // removes the entity/sublevel transform and translates world coordinates into
            // camera-relative space when each vertex is written.
            CABLE_POINTS.add(position);
        }

        final List<Vec3> splinePoints = CatmulRomSpline.generateSpline(CABLE_POINTS, 4);

        if (splinePoints.size() < 2) {
            return;
        }

        final boolean tracePairedGeometry = paired && PAIRED_GEOMETRY_TRACES.get() < 16
                && PAIRED_GEOMETRY_TRACES.getAndIncrement() < 16;
        final boolean traceSettledGeometry = tickCount >= 40 && SETTLED_GEOMETRY_TRACES.get() < 8
                && SETTLED_GEOMETRY_TRACES.getAndIncrement() < 8;
        if (tracePairedGeometry || traceSettledGeometry) {
            final Vec3 lineStart = positions.get(0);
            final Vec3 lineEnd = positions.get(positions.size() - 1);
            double maxControlDeviation = 0.0;
            for (final Vec3 point : CABLE_POINTS) {
                maxControlDeviation = Math.max(maxControlDeviation,
                        distanceToSegment(point, lineStart, lineEnd));
            }

            double maxSplineDeviation = 0.0;
            double minSegmentLength = Double.POSITIVE_INFINITY;
            double maxSegmentLength = 0.0;
            double minTangentAlignment = 1.0;
            final Quaternionf diagnosticOrientation = new Quaternionf();
            final Vector3f diagnosticAxis = new Vector3f();
            for (int i = 0; i < splinePoints.size() - 1; i++) {
                final Vec3 point = splinePoints.get(i);
                final Vec3 nextPoint = splinePoints.get(i + 1);
                final Vec3 tangent = nextPoint.subtract(point);
                final double segmentLength = tangent.length();
                maxSplineDeviation = Math.max(maxSplineDeviation,
                        distanceToSegment(point, lineStart, lineEnd));
                minSegmentLength = Math.min(minSegmentLength, segmentLength);
                maxSegmentLength = Math.max(maxSegmentLength, segmentLength);
                if (segmentLength >= 1.0E-8 && Double.isFinite(segmentLength)) {
                    calculateOrientation(diagnosticOrientation, point.x, point.y, point.z, nextPoint);
                    diagnosticOrientation.transform(diagnosticAxis.set(0, 0, 1));
                    minTangentAlignment = Math.min(minTangentAlignment,
                            diagnosticAxis.x * tangent.x / segmentLength
                                    + diagnosticAxis.y * tangent.y / segmentLength
                                    + diagnosticAxis.z * tangent.z / segmentLength);
                }
            }
            maxSplineDeviation = Math.max(maxSplineDeviation,
                    distanceToSegment(splinePoints.get(splinePoints.size() - 1), lineStart, lineEnd));
            final Vector3f poseStart = poseStack.last().pose().transformPosition(
                    new Vector3f((float) lineStart.x, (float) lineStart.y, (float) lineStart.z));
            final Vector3f poseEnd = poseStack.last().pose().transformPosition(
                    new Vector3f((float) lineEnd.x, (float) lineEnd.y, (float) lineEnd.z));
            final Vec3 expectedStart = lineStart.subtract(cameraPosition);
            final Vec3 expectedEnd = lineEnd.subtract(cameraPosition);
            final double poseStartError = expectedStart.distanceTo(new Vec3(poseStart.x, poseStart.y, poseStart.z));
            final double poseEndError = expectedEnd.distanceTo(new Vec3(poseEnd.x, poseEnd.y, poseEnd.z));
            Simulated.LOGGER.info("SIMULATED-PLUNGER-GEOMETRY: entity={}, tick={}, paired={}, animationScale={}, "
                            + "controlPoints={}, splinePoints={}, lineStart={}, lineEnd={}, firstSpline={}, lastSpline={}, "
                            + "maxControlDeviation={}, maxSplineDeviation={}, minSegmentLength={}, maxSegmentLength={}, "
                            + "minTangentAlignment={}, poseStart=[{},{},{}], poseEnd=[{},{},{}], "
                            + "poseStartError={}, poseEndError={}, finite={}",
                    entityId, tickCount, paired, animationScale, positions.size(), splinePoints.size(),
                    lineStart, lineEnd, splinePoints.get(0), splinePoints.get(splinePoints.size() - 1),
                    maxControlDeviation, maxSplineDeviation, minSegmentLength, maxSegmentLength,
                    minTangentAlignment, poseStart.x, poseStart.y, poseStart.z, poseEnd.x, poseEnd.y, poseEnd.z,
                    poseStartError, poseEndError, isFinite(lineStart) && isFinite(lineEnd)
                            && isFinite(splinePoints.get(0)) && isFinite(splinePoints.get(splinePoints.size() - 1)));
        }

        // ORIENTATION is shared scratch storage. Recompute the first frame explicitly so
        // the first rope segment cannot inherit the final segment orientation from the
        // previous entity/frame.
        final Vec3 firstSplinePoint = splinePoints.get(0);
        final Vec3 secondSplinePoint = splinePoints.get(1);
        calculateOrientation(ORIENTATION, firstSplinePoint.x, firstSplinePoint.y, firstSplinePoint.z,
                secondSplinePoint);

        final int color = 0xFFFFFFFF;
        final float constantRadius = (2f / 16.0f) / 2.0f;
        final float u = 2f / 16F;
        float v = 0;
        float nextV;
        final PoseStack.Pose ropePose = poseStack.last();

        for (int i = 0; i < splinePoints.size() - 1; i++) {
            final float delta = (float) i / (splinePoints.size() - 1);
            final float nextDelta = (float) (i + 1) / (splinePoints.size() - 1);
            final float cableRadius = constantRadius - (0.001f * delta);
            final float nextCableRadius = constantRadius - (0.001f * nextDelta);
            final Vec3 point = splinePoints.get(i);
            final Vec3 nextPoint = splinePoints.get(i + 1);

            final double x = point.x;
            final double y = point.y;
            final double z = point.z;
            final double nextX = nextPoint.x;
            final double nextY = nextPoint.y;
            final double nextZ = nextPoint.z;

            if (i < splinePoints.size() - 2) {
                calculateOrientation(NEXT_ORIENTATION, nextX, nextY, nextZ, splinePoints.get(i + 2));
            } else {
                NEXT_ORIENTATION.set(ORIENTATION);
            }

            final int lightStart = LevelRenderer.getLightColor(level, LIGHT_POS.set(x, y, z));
            final int lightEnd = LevelRenderer.getLightColor(level, LIGHT_POS.set(nextX, nextY, nextZ));

            final double length = Math.sqrt((nextX - x) * (nextX - x) + (nextY - y) * (nextY - y) + (nextZ - z) * (nextZ - z));
            nextV = v + (float) (length * (17 / 16f));

            // Down
            ORIENTATION.transform(NORMAL.set(0, -1, 0));
            NEXT_ORIENTATION.transform(NEXT_NORMAL.set(0, -1, 0));

            NEXT_ORIENTATION.transform(POS.set(-nextCableRadius, -nextCableRadius, 0));
            addRopeVertex(builder, ropePose, nextX + POS.x, nextY + POS.y, nextZ + POS.z, color, 0, nextV, lightEnd, NEXT_NORMAL);

            ORIENTATION.transform(POS.set(-cableRadius, -cableRadius, 0));
            addRopeVertex(builder, ropePose, x + POS.x, y + POS.y, z + POS.z, color, 0, v, lightStart, NORMAL);

            ORIENTATION.transform(POS.set(cableRadius, -cableRadius, 0));
            addRopeVertex(builder, ropePose, x + POS.x, y + POS.y, z + POS.z, color, u, v, lightStart, NORMAL);

            NEXT_ORIENTATION.transform(POS.set(nextCableRadius, -nextCableRadius, 0));
            addRopeVertex(builder, ropePose, nextX + POS.x, nextY + POS.y, nextZ + POS.z, color, u, nextV, lightEnd, NEXT_NORMAL);

            // Up
            ORIENTATION.transform(NORMAL.set(0, 1, 0));
            NEXT_ORIENTATION.transform(NEXT_NORMAL.set(0, 1, 0));

            ORIENTATION.transform(POS.set(-cableRadius, cableRadius, 0));
            addRopeVertex(builder, ropePose, x + POS.x, y + POS.y, z + POS.z, color, 0, v, lightStart, NORMAL);

            NEXT_ORIENTATION.transform(POS.set(-nextCableRadius, nextCableRadius, 0));
            addRopeVertex(builder, ropePose, nextX + POS.x, nextY + POS.y, nextZ + POS.z, color, 0, nextV, lightEnd, NEXT_NORMAL);

            NEXT_ORIENTATION.transform(POS.set(nextCableRadius, nextCableRadius, 0));
            addRopeVertex(builder, ropePose, nextX + POS.x, nextY + POS.y, nextZ + POS.z, color, u, nextV, lightEnd, NEXT_NORMAL);

            ORIENTATION.transform(POS.set(cableRadius, cableRadius, 0));
            addRopeVertex(builder, ropePose, x + POS.x, y + POS.y, z + POS.z, color, u, v, lightStart, NORMAL);

            // West
            ORIENTATION.transform(NORMAL.set(-1, 0, 0));
            NEXT_ORIENTATION.transform(NEXT_NORMAL.set(-1, 0, 0));

            NEXT_ORIENTATION.transform(POS.set(-nextCableRadius, -nextCableRadius, 0));
            addRopeVertex(builder, ropePose, nextX + POS.x, nextY + POS.y, nextZ + POS.z, color, u, nextV, lightEnd, NEXT_NORMAL);

            NEXT_ORIENTATION.transform(POS.set(-nextCableRadius, nextCableRadius, 0));
            addRopeVertex(builder, ropePose, nextX + POS.x, nextY + POS.y, nextZ + POS.z, color, 0, nextV, lightEnd, NEXT_NORMAL);

            ORIENTATION.transform(POS.set(-cableRadius, cableRadius, 0));
            addRopeVertex(builder, ropePose, x + POS.x, y + POS.y, z + POS.z, color, 0, v, lightStart, NORMAL);

            ORIENTATION.transform(POS.set(-cableRadius, -cableRadius, 0));
            addRopeVertex(builder, ropePose, x + POS.x, y + POS.y, z + POS.z, color, u, v, lightStart, NORMAL);

            // East
            ORIENTATION.transform(NORMAL.set(1, 0, 0));
            NEXT_ORIENTATION.transform(NEXT_NORMAL.set(1, 0, 0));

            ORIENTATION.transform(POS.set(cableRadius, -cableRadius, 0));
            addRopeVertex(builder, ropePose, x + POS.x, y + POS.y, z + POS.z, color, u, v, lightStart, NORMAL);

            ORIENTATION.transform(POS.set(cableRadius, cableRadius, 0));
            addRopeVertex(builder, ropePose, x + POS.x, y + POS.y, z + POS.z, color, 0, v, lightStart, NORMAL);

            NEXT_ORIENTATION.transform(POS.set(nextCableRadius, nextCableRadius, 0));
            addRopeVertex(builder, ropePose, nextX + POS.x, nextY + POS.y, nextZ + POS.z, color, 0, nextV, lightEnd, NEXT_NORMAL);

            NEXT_ORIENTATION.transform(POS.set(nextCableRadius, -nextCableRadius, 0));
            addRopeVertex(builder, ropePose, nextX + POS.x, nextY + POS.y, nextZ + POS.z, color, u, nextV, lightEnd, NEXT_NORMAL);

            ORIENTATION.set(NEXT_ORIENTATION);
            v = nextV;
        }
    }

    private static void addRopeVertex(final VertexConsumer builder, final PoseStack.Pose pose,
                                      final double x, final double y, final double z,
                                      final int color, final float u, final float v, final int light,
                                      final Vector3f normal) {
        builder.vertex(pose.pose(), (float) x, (float) y, (float) z)
                .color(color)
                .uv(u, v)
                .uv2(light)
                .normal(pose.normal(), normal.x, normal.y, normal.z)
                .endVertex();
    }

    private static void calculateOrientation(final Quaternionf store, final double x, final double y, final double z, final Vec3 nextPoint) {
        final double dx = nextPoint.x - x;
        final double dy = nextPoint.y - y;
        final double dz = nextPoint.z - z;
        final double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (!Double.isFinite(length) || length < 1.0E-8) {
            store.identity();
            return;
        }

        // Floating-point rounding can push an almost vertical segment slightly outside
        // acos's [-1, 1] domain and poison all vertices produced from this quaternion.
        final double vertical = Math.max(-1.0, Math.min(1.0, dy / length));
        final float factor = 0;//(float) Mth.smoothstep(1.0-Mth.clamp(8*Math.sqrt(dx * dx + dz * dz), 0.0, 1.0));
        store.identity()
                .rotateAxis((float) Math.atan2(dx, dz), 0, 1, 0)
                .rotateAxis((float) (Math.acos(vertical) - Math.PI / 2.0), 1, 0, 0)
                .slerp(dy < 0 ? POSITIVE_Y : NEGATIVE_Y, factor);
    }

    private static double distanceToSegment(final Vec3 point, final Vec3 start, final Vec3 end) {
        final Vec3 delta = end.subtract(start);
        final double lengthSquared = delta.lengthSqr();
        if (!Double.isFinite(lengthSquared) || lengthSquared < 1.0E-12) {
            return point.distanceTo(start);
        }
        final double t = Mth.clamp(point.subtract(start).dot(delta) / lengthSquared, 0.0, 1.0);
        return point.distanceTo(start.add(delta.scale(t)));
    }

    private static boolean isFinite(final Vec3 value) {
        return Double.isFinite(value.x) && Double.isFinite(value.y) && Double.isFinite(value.z);
    }

    @Override
    public ResourceLocation getTextureLocation(final LaunchedPlungerEntity entity) {
        return ResourceLocation.withDefaultNamespace("missing");
    }

    @Override
    public boolean shouldRender(final LaunchedPlungerEntity entity, final Frustum frustum, final double d, final double e, final double f) {
        return true;
    }
}

package dev.simulated_team.simulated.validation;

import com.mojang.logging.LogUtils;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.impl.visualization.VisualManagerImpl;
import dev.eriksonn.aeronautics.content.blocks.propeller.small.andesite.AndesitePropellerBlockEntity;
import dev.eriksonn.aeronautics.index.AeroBlocks;
import dev.ryanhcode.offroad.content.blocks.wheel_mount.WheelMountBlockEntity;
import dev.ryanhcode.offroad.index.OffroadBlocks;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.sublevel.ClientSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.neoforge.mixinhelper.compatibility.flywheel.SubLevelEmbedding;
import dev.ryanhcode.sable.neoforge.mixinterface.compatibility.flywheel.BlockEntityStorageExtension;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import dev.simulated_team.simulated.Simulated;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/** Development-only synchronized Sable-sublevel rendering validation. */
@Mod.EventBusSubscriber(modid = Simulated.MOD_ID, value = Dist.CLIENT)
public final class SubLevelRenderClientValidation {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final boolean ENABLED = Boolean.getBoolean("simulated.validation.subLevelRender");
    private static final int TIMEOUT_TICKS = 600;
    private static final int REQUIRED_VISIBLE_TICKS = 20;

    private static final Set<BlockEntity> CREATED_PROPELLER_VISUALS =
            Collections.synchronizedSet(Collections.newSetFromMap(new IdentityHashMap<>()));
    private static final Set<BlockEntity> FRAMED_PROPELLER_VISUALS =
            Collections.synchronizedSet(Collections.newSetFromMap(new IdentityHashMap<>()));
    private static final Set<BlockEntity> RENDERED_WHEELS =
            Collections.synchronizedSet(Collections.newSetFromMap(new IdentityHashMap<>()));
    private static final AtomicInteger PROPELLER_FRAMES = new AtomicInteger();
    private static final AtomicInteger WHEEL_RENDER_CALLS = new AtomicInteger();

    private static Phase phase = Phase.WAITING_FOR_WORLD;
    private static int phaseTicks;
    private static int visibleTicks;
    private static int exitTicks;
    private static volatile UUID fixtureId;
    private static volatile UUID localPlayerId;
    private static volatile boolean fixtureReady;
    private static volatile boolean staleCleanupReady;
    private static volatile boolean serverMaintenanceScheduled;
    private static AndesitePropellerBlockEntity clientPropeller;
    private static WheelMountBlockEntity clientWheel;

    private SubLevelRenderClientValidation() {
    }

    public static void recordPropellerVisualCreated(final AndesitePropellerBlockEntity blockEntity) {
        if (ENABLED && blockEntity != null) {
            CREATED_PROPELLER_VISUALS.add(blockEntity);
        }
    }

    public static void recordPropellerVisualFrame(final AndesitePropellerBlockEntity blockEntity) {
        if (ENABLED && blockEntity != null) {
            FRAMED_PROPELLER_VISUALS.add(blockEntity);
            PROPELLER_FRAMES.incrementAndGet();
        }
    }

    public static void recordWheelRendererEntry(final WheelMountBlockEntity blockEntity) {
        if (ENABLED && blockEntity != null) {
            RENDERED_WHEELS.add(blockEntity);
            WHEEL_RENDER_CALLS.incrementAndGet();
        }
    }

    @SubscribeEvent
    public static void onClientTick(final TickEvent.ClientTickEvent event) {
        if (!ENABLED || event.phase != TickEvent.Phase.END || phase == Phase.DONE) {
            return;
        }

        final Minecraft minecraft = Minecraft.getInstance();
        final MinecraftServer server = minecraft.getSingleplayerServer();
        if (minecraft.level == null || minecraft.player == null || server == null) {
            if (phase == Phase.WAITING_FOR_WORLD) {
                return;
            }
            fail("integrated server or local player disappeared during validation");
        }

        if (phase == Phase.WAITING_FOR_WORLD) {
            localPlayerId = minecraft.player.getUUID();
            phase = Phase.WAITING_FOR_STALE_CLEANUP;
            phaseTicks = 0;
            server.execute(() -> removeStaleValidationFixtures(server));
            return;
        }

        if (++phaseTicks > TIMEOUT_TICKS) {
            fail("timed out in phase " + phase + ", fixture=" + fixtureId
                    + ", propellerFrames=" + PROPELLER_FRAMES.get()
                    + ", wheelRenders=" + WHEEL_RENDER_CALLS.get());
        }

        scheduleFixtureMaintenance(server);

        if (phase == Phase.WAITING_FOR_STALE_CLEANUP) {
            final ClientSubLevelContainer container = SubLevelContainer.getContainer(minecraft.level);
            boolean clientFixtureRemaining = false;
            if (container != null) {
                for (final ClientSubLevel subLevel : container.getAllSubLevels()) {
                    clientFixtureRemaining |= isValidationFixture(subLevel);
                }
            }
            if (staleCleanupReady && !clientFixtureRemaining) {
                phase = Phase.WAITING_FOR_SUBLEVEL;
                phaseTicks = 0;
                server.execute(() -> createFixture(server, localPlayerId));
            }
            return;
        }

        if (phase == Phase.WAITING_FOR_SUBLEVEL) {
            final ClientSubLevel subLevel = findClientSubLevel(minecraft);
            if (fixtureReady && subLevel != null && subLevel.isFinalized() && findClientActors(subLevel)) {
                LOGGER.info("SIMULATED-SUBLEVEL-RENDER-SYNC: id={}, plot={}, propeller={}, wheel={}",
                        fixtureId, subLevel.getPlot().getCenterChunk(), clientPropeller.getBlockPos(), clientWheel.getBlockPos());
                phase = Phase.VERIFYING_RENDER;
                phaseTicks = 0;
            }
            return;
        }

        if (phase == Phase.VERIFYING_RENDER) {
            final ClientSubLevel subLevel = findClientSubLevel(minecraft);
            if (subLevel == null || clientPropeller == null || clientWheel == null) {
                fail("synchronized fixture disappeared while verifying rendering");
            }

            final Vec3 localTarget = Vec3.atCenterOf(clientPropeller.getBlockPos())
                    .add(Vec3.atCenterOf(clientWheel.getBlockPos())).scale(0.5);
            final Vec3 physicalTarget = subLevel.renderPose().transformPosition(localTarget);
            final double cameraDot = aimCamera(minecraft, physicalTarget);

            final VisualizationManager visualizationManager = VisualizationManager.get(minecraft.level);
            if (visualizationManager == null) {
                fail("Flywheel did not provide a visualization manager for the client level");
            }
            if (!(visualizationManager.blockEntities() instanceof VisualManagerImpl<?, ?>)) {
                fail("Flywheel block-entity manager implementation was unexpected: "
                        + visualizationManager.blockEntities().getClass().getName());
            }
            final Object storage = ((VisualManagerImpl<?, ?>) visualizationManager.blockEntities()).getStorage();
            if (!(storage instanceof BlockEntityStorageExtension)) {
                fail("Sable Flywheel storage extension was not applied: " + storage.getClass().getName());
            }
            final BlockEntityStorageExtension storageExtension = (BlockEntityStorageExtension) storage;
            final SubLevelEmbedding embedding = storageExtension.sable$getEmbeddingInfo(subLevel);
            final boolean embedded = embedding != null && embedding.blockEntities().contains(clientPropeller);
            final boolean propellerCreated = CREATED_PROPELLER_VISUALS.contains(clientPropeller);
            final boolean propellerFramed = FRAMED_PROPELLER_VISUALS.contains(clientPropeller);
            final boolean wheelRendered = RENDERED_WHEELS.contains(clientWheel);

            if (cameraDot > 0.995 && embedded && propellerCreated && propellerFramed && wheelRendered) {
                visibleTicks++;
            } else {
                visibleTicks = 0;
            }

            if (visibleTicks >= REQUIRED_VISIBLE_TICKS) {
                LOGGER.info("SIMULATED-SUBLEVEL-RENDER-PASS: id={}, physicalTarget={}, cameraDot={}, "
                                + "flywheelVisualCount={}, embedded=true, propellerCreated=true, "
                                + "propellerFramed=true, propellerFrames={}, wheelRendererEntered=true, wheelRenders={}",
                        fixtureId, physicalTarget, cameraDot, visualizationManager.blockEntities().visualCount(),
                        PROPELLER_FRAMES.get(), WHEEL_RENDER_CALLS.get());
                fixtureReady = false;
                server.execute(() -> movePlayerOutOfTrackingRange(server, localPlayerId));
                phase = Phase.WAITING_FOR_UNTRACK;
                phaseTicks = 0;
            }
            return;
        }

        if (phase == Phase.WAITING_FOR_UNTRACK) {
            if (findClientSubLevel(minecraft) == null) {
                phase = Phase.DRAINING_NETWORK;
                phaseTicks = 0;
                exitTicks = 0;
                LOGGER.info("SIMULATED-SUBLEVEL-RENDER-UNTRACKED: id={}", fixtureId);
            }
            return;
        }

        if (phase == Phase.DRAINING_NETWORK && ++exitTicks >= 20) {
            server.execute(() -> removeFixture(server, fixtureId));
            phase = Phase.EXITING;
            phaseTicks = 0;
            exitTicks = 0;
            return;
        }

        if (phase == Phase.EXITING && ++exitTicks >= 20) {
            phase = Phase.DONE;
            minecraft.stop();
        }
    }

    private static void createFixture(final MinecraftServer server, final UUID playerId) {
        final ServerPlayer player = server.getPlayerList().getPlayer(playerId);
        if (player == null) {
            fail("local server player was not available for fixture creation");
        }

        final ServerLevel level = player.serverLevel();
        final ServerSubLevelContainer container = SubLevelContainer.getContainer(level);
        if (container == null) {
            fail("Sable server sub-level container was unavailable for fixture creation");
        }
        final BlockPos anchor = player.blockPosition().offset(0, 3, 6);
        final BlockPos propellerPos = anchor.above();
        final BlockPos wheelPos = anchor.east();
        final List<BlockPos> blocks = List.of(anchor, propellerPos, wheelPos);
        for (final BlockPos pos : blocks) {
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        }
        level.setBlockAndUpdate(anchor, Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(propellerPos, AeroBlocks.ANDESITE_PROPELLER.getDefaultState());
        level.setBlockAndUpdate(wheelPos, OffroadBlocks.WHEEL_MOUNT.getDefaultState());
        if (!(level.getBlockEntity(wheelPos) instanceof WheelMountBlockEntity)) {
            fail("server fixture did not create an Offroad wheel mount at " + wheelPos);
        }
        final WheelMountBlockEntity wheelMount = (WheelMountBlockEntity) level.getBlockEntity(wheelPos);
        wheelMount.getInventory().setItem(0, new ItemStack(OffroadBlocks.ROCK_CUTTER_BLOCK.get()));
        wheelMount.setChanged();

        final ServerSubLevel subLevel = SubLevelAssemblyHelper.assembleBlocks(level, anchor, blocks,
                new BoundingBox3i(anchor.getX(), anchor.getY(), anchor.getZ(),
                        wheelPos.getX() + 1, propellerPos.getY() + 1, anchor.getZ() + 1));
        fixtureId = subLevel.getUniqueId();
        fixtureReady = true;
        stabilizeFixture(subLevel);
        LOGGER.info("SIMULATED-SUBLEVEL-RENDER-FIXTURE: id={}, anchor={}, player={}",
                fixtureId, anchor, player.getGameProfile().getName());
    }

    private static void scheduleFixtureMaintenance(final MinecraftServer server) {
        if (!fixtureReady || serverMaintenanceScheduled) {
            return;
        }
        serverMaintenanceScheduled = true;
        server.execute(() -> {
            try {
                for (final ServerLevel level : server.getAllLevels()) {
                    final ServerSubLevelContainer container = SubLevelContainer.getContainer(level);
                    if (container == null) {
                        continue;
                    }
                    for (final ServerSubLevel subLevel : container.getAllSubLevels()) {
                        if (subLevel.getUniqueId().equals(fixtureId)) {
                            stabilizeFixture(subLevel);
                            return;
                        }
                    }
                }
            } finally {
                serverMaintenanceScheduled = false;
            }
        });
    }

    private static void stabilizeFixture(final ServerSubLevel subLevel) {
        final RigidBodyHandle handle = RigidBodyHandle.of(subLevel);
        if (handle == null) {
            fail("Sable did not create a rigid-body handle for fixture " + subLevel.getUniqueId());
        }
        handle.addLinearAndAngularVelocity(
                handle.getLinearVelocity(new Vector3d()).negate(),
                handle.getAngularVelocity(new Vector3d()).negate());
        final Quaterniond upright = new Quaterniond();
        handle.teleport(subLevel.logicalPose().position(), upright);
        subLevel.logicalPose().orientation().identity();
    }

    private static void removeFixture(final MinecraftServer server, final UUID id) {
        for (final ServerLevel level : server.getAllLevels()) {
            final ServerSubLevelContainer container = SubLevelContainer.getContainer(level);
            if (container == null) {
                continue;
            }
            for (final ServerSubLevel subLevel : new ArrayList<>(container.getAllSubLevels())) {
                if (subLevel.getUniqueId().equals(id)) {
                    container.removeSubLevel(subLevel, SubLevelRemovalReason.REMOVED);
                    LOGGER.info("SIMULATED-SUBLEVEL-RENDER-CLEANUP: id={}", id);
                    return;
                }
            }
        }
    }

    private static void movePlayerOutOfTrackingRange(final MinecraftServer server, final UUID playerId) {
        final ServerPlayer player = server.getPlayerList().getPlayer(playerId);
        if (player == null) {
            fail("local server player was not available for fixture untracking");
        }
        player.teleportTo(player.getX() + 1024.0, player.getY(), player.getZ());
    }

    private static void removeStaleValidationFixtures(final MinecraftServer server) {
        int removed = 0;
        for (final ServerLevel level : server.getAllLevels()) {
            final ServerSubLevelContainer container = SubLevelContainer.getContainer(level);
            if (container == null) {
                continue;
            }
            for (final ServerSubLevel subLevel : new ArrayList<>(container.getAllSubLevels())) {
                if (isValidationFixture(subLevel)) {
                    container.removeSubLevel(subLevel, SubLevelRemovalReason.REMOVED);
                    removed++;
                }
            }
        }
        staleCleanupReady = true;
        LOGGER.info("SIMULATED-SUBLEVEL-RENDER-STALE-CLEANUP: removed={}", removed);
    }

    private static boolean isValidationFixture(final SubLevel subLevel) {
        int actorCount = 0;
        boolean propeller = false;
        boolean wheel = false;
        for (final BlockEntitySubLevelActor actor : subLevel.getPlot().getBlockEntityActors()) {
            actorCount++;
            propeller |= actor instanceof AndesitePropellerBlockEntity;
            wheel |= actor instanceof WheelMountBlockEntity;
        }
        return actorCount == 2 && propeller && wheel;
    }

    private static ClientSubLevel findClientSubLevel(final Minecraft minecraft) {
        if (fixtureId == null) {
            return null;
        }
        final ClientSubLevelContainer container = SubLevelContainer.getContainer(minecraft.level);
        if (container == null) {
            return null;
        }
        for (final ClientSubLevel subLevel : container.getAllSubLevels()) {
            if (subLevel.getUniqueId().equals(fixtureId)) {
                return subLevel;
            }
        }
        return null;
    }

    private static boolean findClientActors(final ClientSubLevel subLevel) {
        AndesitePropellerBlockEntity propeller = null;
        WheelMountBlockEntity wheel = null;
        for (final BlockEntitySubLevelActor actor : subLevel.getPlot().getBlockEntityActors()) {
            if (actor instanceof final AndesitePropellerBlockEntity foundPropeller) {
                propeller = foundPropeller;
            } else if (actor instanceof final WheelMountBlockEntity foundWheel) {
                wheel = foundWheel;
            }
        }
        if (propeller != null && wheel != null && !wheel.getHeldItem().isEmpty()) {
            clientPropeller = propeller;
            clientWheel = wheel;
            return true;
        }
        return false;
    }

    private static double aimCamera(final Minecraft minecraft, final Vec3 target) {
        final Vec3 eye = minecraft.player.getEyePosition(1.0f);
        final Vec3 delta = target.subtract(eye);
        final double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        final float yaw = (float) Mth.wrapDegrees(Math.toDegrees(Math.atan2(-delta.x, delta.z)));
        final float pitch = (float) Mth.wrapDegrees(-Math.toDegrees(Math.atan2(delta.y, horizontal)));
        minecraft.player.setYRot(yaw);
        minecraft.player.setXRot(pitch);
        minecraft.player.yRotO = yaw;
        minecraft.player.xRotO = pitch;
        minecraft.player.yHeadRot = yaw;
        minecraft.player.yHeadRotO = yaw;
        return minecraft.player.getViewVector(1.0f).dot(delta.normalize());
    }

    private static void fail(final String message) {
        LOGGER.error("SIMULATED-SUBLEVEL-RENDER-FAIL: {}", message);
        throw new IllegalStateException("Simulated sublevel render validation failed: " + message);
    }

    private enum Phase {
        WAITING_FOR_WORLD,
        WAITING_FOR_STALE_CLEANUP,
        WAITING_FOR_SUBLEVEL,
        VERIFYING_RENDER,
        WAITING_FOR_UNTRACK,
        DRAINING_NETWORK,
        EXITING,
        DONE
    }
}

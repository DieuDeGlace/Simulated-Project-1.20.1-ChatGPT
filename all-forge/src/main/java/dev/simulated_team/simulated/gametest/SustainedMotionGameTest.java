package dev.simulated_team.simulated.gametest;

import com.mojang.logging.LogUtils;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.eriksonn.aeronautics.content.blocks.propeller.small.andesite.AndesitePropellerBlockEntity;
import dev.eriksonn.aeronautics.index.AeroBlocks;
import dev.ryanhcode.offroad.content.blocks.wheel_mount.WheelMountBlock;
import dev.ryanhcode.offroad.content.blocks.wheel_mount.WheelMountBlockEntity;
import dev.ryanhcode.offroad.index.OffroadBlocks;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.physics.force.ForceGroups;
import dev.ryanhcode.sable.api.physics.force.QueuedForceGroup;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import dev.simulated_team.simulated.Simulated;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@GameTestHolder(Simulated.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SustainedMotionGameTest {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final double MIN_HORIZONTAL_DISPLACEMENT = 0.025;
    private static final double MIN_HORIZONTAL_SPEED = 0.005;

    private SustainedMotionGameTest() {
    }

    @GameTest(template = "extrakineticstest.analogtransmission", timeoutTicks = 190)
    public static void aeronauticsPropellerSustainsFreeBodyMotion(final GameTestHelper helper) {
        final MotionFixture[] fixture = new MotionFixture[1];
        helper.startSequence()
                .thenExecute(() -> fixture[0] = createPropellerFixture(helper))
                .thenIdle(60)
                .thenExecute(() -> fixture[0] = beginMotionSample(fixture[0], "Aeronautics propeller"))
                .thenIdle(80)
                .thenExecute(() -> {
                    try {
                        final MotionFixture state = requireFixture(fixture[0], "Aeronautics");
                        final AndesitePropellerBlockEntity propeller = requireBlockEntity(
                                helper.getLevel(), state.actorPositions().get(0), AndesitePropellerBlockEntity.class);
                        if (!propeller.isActive() || Math.abs(propeller.getThrust()) <= 1.0e-6) {
                            throw new GameTestAssertException("Aeronautics propeller did not remain active during sustained motion");
                        }
                        final Map<?, QueuedForceGroup> groups = state.subLevel().getQueuedForceGroups();
                        final QueuedForceGroup propulsion = groups == null ? null : groups.get(ForceGroups.PROPULSION.get());
                        if (propulsion == null || propulsion.getRecordedPointForces().isEmpty()) {
                            throw new GameTestAssertException("Aeronautics propulsion force was not present at the sustained sample");
                        }
                        final MotionSample sample = requireHorizontalMotion("Aeronautics propeller", state);
                        if (Math.signum(propeller.getScaledThrust()) * sample.xDisplacement()
                                <= MIN_HORIZONTAL_DISPLACEMENT) {
                            throw new GameTestAssertException("Aeronautics body did not move along its production thrust axis: "
                                    + "scaledThrust=" + propeller.getScaledThrust()
                                    + ", xDisplacement=" + sample.xDisplacement());
                        }
                    } finally {
                        cleanup(helper.getLevel(), fixture[0]);
                    }
                })
                .thenSucceed();
    }

    @GameTest(template = "extrakineticstest.analogtransmission", timeoutTicks = 190)
    public static void offroadWheelsSustainFreeBodyMotion(final GameTestHelper helper) {
        final MotionFixture[] fixture = new MotionFixture[1];
        helper.startSequence()
                .thenExecute(() -> fixture[0] = createOffroadFixture(helper))
                .thenIdle(60)
                .thenExecute(() -> {
                    final MotionFixture state = requireFixture(fixture[0], "Offroad");
                    state.handle().teleport(state.subLevel().logicalPose().position(), new Quaterniond());
                    state.subLevel().logicalPose().orientation().identity();
                    fixture[0] = beginMotionSample(state, "Offroad wheels");
                })
                .thenIdle(80)
                .thenExecute(() -> {
                    try {
                        final MotionFixture state = requireFixture(fixture[0], "Offroad");
                        WheelMountBlockEntity sampledWheel = null;
                        for (final BlockPos pos : state.actorPositions()) {
                            final WheelMountBlockEntity wheel = requireBlockEntity(helper.getLevel(), pos,
                                    WheelMountBlockEntity.class);
                            if (wheel.getHeldItem().isEmpty() || Math.abs(wheel.getSpeed()) < 0.01f) {
                                throw new GameTestAssertException("Offroad wheel lost its tire or drive speed at " + pos);
                            }
                            sampledWheel = wheel;
                        }
                        final MotionSample sample = requireHorizontalMotion("Offroad wheels", state);
                        final double speed = sampledWheel == null ? 0.0 : sampledWheel.getSpeed();
                        final Vector3d physicalDriveAxis = new Vector3d(0.0, 0.0, Math.signum(speed));
                        state.subLevel().logicalPose().transformNormal(physicalDriveAxis);
                        final double forwardSpeed = sample.velocity().dot(physicalDriveAxis);
                        LOGGER.info("SIMULATED-SUSTAINED-OFFROAD-AXIS: id={}, kineticSpeed={}, physicalDriveAxis={}, forwardSpeed={}",
                                state.subLevel().getUniqueId(), speed, physicalDriveAxis, forwardSpeed);
                        if (sampledWheel == null || forwardSpeed <= MIN_HORIZONTAL_SPEED) {
                            throw new GameTestAssertException("Offroad body did not retain forward velocity along the wheel's "
                                    + "pose-transformed production drive axis: speed=" + speed
                                    + ", driveAxis=" + physicalDriveAxis + ", forwardSpeed=" + forwardSpeed);
                        }
                    } finally {
                        cleanup(helper.getLevel(), fixture[0]);
                    }
                })
                .thenSucceed();
    }

    private static MotionFixture createPropellerFixture(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos anchor = helper.absolutePos(new BlockPos(2, 250, 2));
        final BlockPos shaftSource = anchor.east();
        final BlockPos propellerSource = anchor.east(2);
        final List<BlockPos> sourceBlocks = List.of(anchor, shaftSource, propellerSource);
        level.setBlockAndUpdate(anchor, AllBlocks.CREATIVE_MOTOR.getDefaultState()
                .setValue(BlockStateProperties.FACING, Direction.EAST));
        level.setBlockAndUpdate(shaftSource, AllBlocks.SHAFT.getDefaultState()
                .setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
        level.setBlockAndUpdate(propellerSource, AeroBlocks.ANDESITE_PROPELLER.getDefaultState()
                .setValue(BlockStateProperties.FACING, Direction.EAST));

        final ServerSubLevel subLevel = SubLevelAssemblyHelper.assembleBlocks(level, anchor,
                sourceBlocks, bounds(sourceBlocks));
        subLevel.enableIndividualQueuedForcesTracking(true);
        final BlockPos propellerPos = subLevel.getPlot().getCenterBlock().east(2);
        final AndesitePropellerBlockEntity propeller = requireBlockEntity(
                level, propellerPos, AndesitePropellerBlockEntity.class);
        requireActor(subLevel, propeller, "Aeronautics propeller");

        final RigidBodyHandle handle = requireHandle(subLevel);
        cancelMotion(handle);
        final Vector3d start = new Vector3d(subLevel.logicalPose().position());
        LOGGER.info("SIMULATED-SUSTAINED-PROPELLER-START: id={}, position={}, propeller={}",
                subLevel.getUniqueId(), start, propellerPos);
        return new MotionFixture(subLevel, handle, start, List.of(propellerPos), List.of());
    }

    private static MotionFixture createOffroadFixture(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos anchor = helper.absolutePos(new BlockPos(2, 12, 2));
        final BlockPos shaftSource = anchor.east();
        final BlockPos wheelSource = anchor.east(2);
        final BlockPos northBallast = anchor.north();
        final BlockPos southBallast = anchor.south();
        final List<BlockPos> sourceBlocks = List.of(anchor, shaftSource, wheelSource, northBallast, southBallast);
        final List<BlockPos> platform = new ArrayList<>();

        for (int x = -16; x <= 16; x++) {
            for (int z = -16; z <= 16; z++) {
                final BlockPos pos = anchor.offset(x, -2, z);
                level.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
                platform.add(pos);
            }
        }

        level.setBlockAndUpdate(anchor, AllBlocks.CREATIVE_MOTOR.getDefaultState()
                .setValue(BlockStateProperties.FACING, Direction.EAST));
        level.setBlockAndUpdate(shaftSource, AllBlocks.SHAFT.getDefaultState()
                .setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
        level.setBlockAndUpdate(wheelSource, OffroadBlocks.WHEEL_MOUNT.getDefaultState()
                .setValue(WheelMountBlock.HORIZONTAL_FACING, Direction.EAST));
        level.setBlockAndUpdate(northBallast, Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(southBallast, Blocks.STONE.defaultBlockState());
        requireBlockEntity(level, wheelSource, WheelMountBlockEntity.class).getInventory()
                .setItem(0, new ItemStack(OffroadBlocks.ROCK_CUTTER_BLOCK.get()));

        final ServerSubLevel subLevel = SubLevelAssemblyHelper.assembleBlocks(
                level, anchor, sourceBlocks, bounds(sourceBlocks));
        final BlockPos plotAnchor = subLevel.getPlot().getCenterBlock();
        final List<BlockPos> wheelPositions = List.of(plotAnchor.east(2));
        for (final BlockPos pos : wheelPositions) {
            final WheelMountBlockEntity wheel = requireBlockEntity(level, pos, WheelMountBlockEntity.class);
            requireActor(subLevel, wheel, "Offroad wheel mount");
        }

        final RigidBodyHandle handle = requireHandle(subLevel);
        cancelMotion(handle);
        handle.teleport(subLevel.logicalPose().position(), new Quaterniond());
        subLevel.logicalPose().orientation().identity();
        final Vector3d start = new Vector3d(subLevel.logicalPose().position());
        LOGGER.info("SIMULATED-SUSTAINED-OFFROAD-START: id={}, position={}, wheels={}, platformY={}",
                subLevel.getUniqueId(), start, wheelPositions, anchor.getY() - 2);
        return new MotionFixture(subLevel, handle, start, wheelPositions, platform);
    }

    private static MotionFixture beginMotionSample(final MotionFixture fixture, final String label) {
        final MotionFixture state = requireFixture(fixture, label);
        for (final BlockPos actorPos : state.actorPositions()) {
            final BlockEntity actor = state.subLevel().getLevel().getBlockEntity(actorPos);
            if (!(actor instanceof final KineticBlockEntity kinetic) || Math.abs(kinetic.getSpeed()) < 0.01f) {
                throw new GameTestAssertException(label + " did not receive a real Create kinetic speed at "
                        + actorPos + ": " + actor);
            }
        }
        cancelMotion(state.handle());
        final Vector3d start = new Vector3d(state.subLevel().logicalPose().position());
        LOGGER.info("SIMULATED-SUSTAINED-MOTION-SAMPLE-START: label={}, id={}, position={}",
                label, state.subLevel().getUniqueId(), start);
        return new MotionFixture(state.subLevel(), state.handle(), start,
                state.actorPositions(), state.platformBlocks());
    }

    private static MotionSample requireHorizontalMotion(final String label, final MotionFixture fixture) {
        final Vector3d position = new Vector3d(fixture.subLevel().logicalPose().position());
        final Vector3d velocity = fixture.handle().getLinearVelocity(new Vector3d());
        final double dx = position.x - fixture.startPosition().x;
        final double dz = position.z - fixture.startPosition().z;
        final double horizontalDisplacement = Math.sqrt(dx * dx + dz * dz);
        final double horizontalSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        LOGGER.info("SIMULATED-SUSTAINED-MOTION-SAMPLE: label={}, id={}, start={}, position={}, velocity={}, "
                        + "horizontalDisplacement={}, horizontalSpeed={}",
                label, fixture.subLevel().getUniqueId(), fixture.startPosition(), position, velocity,
                horizontalDisplacement, horizontalSpeed);
        if (horizontalDisplacement <= MIN_HORIZONTAL_DISPLACEMENT
                || horizontalSpeed <= MIN_HORIZONTAL_SPEED) {
            throw new GameTestAssertException(label + " did not produce sustained horizontal rigid-body motion: displacement="
                    + horizontalDisplacement + ", speed=" + horizontalSpeed + ", velocity=" + velocity);
        }
        return new MotionSample(position, velocity, dx, dz, horizontalDisplacement, horizontalSpeed);
    }

    private static void requireActor(final ServerSubLevel subLevel, final BlockEntity expected, final String label) {
        for (final BlockEntitySubLevelActor actor : subLevel.getPlot().getBlockEntityActors()) {
            if (actor == expected) {
                return;
            }
        }
        throw new GameTestAssertException(label + " was not admitted to the Sable actor registry");
    }

    private static RigidBodyHandle requireHandle(final ServerSubLevel subLevel) {
        return Objects.requireNonNull(RigidBodyHandle.of(subLevel),
                "Missing Sable rigid-body handle for " + subLevel.getUniqueId());
    }

    private static void cancelMotion(final RigidBodyHandle handle) {
        handle.addLinearAndAngularVelocity(
                handle.getLinearVelocity(new Vector3d()).negate(),
                handle.getAngularVelocity(new Vector3d()).negate());
    }

    private static BoundingBox3i bounds(final List<BlockPos> blocks) {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (final BlockPos pos : blocks) {
            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX());
            maxY = Math.max(maxY, pos.getY());
            maxZ = Math.max(maxZ, pos.getZ());
        }
        return new BoundingBox3i(
                minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1);
    }

    private static MotionFixture requireFixture(final MotionFixture fixture, final String label) {
        if (fixture == null) {
            throw new GameTestAssertException(label + " sustained-motion fixture was not created");
        }
        if (fixture.subLevel().isRemoved() || !fixture.handle().isValid()) {
            throw new GameTestAssertException(label + " rigid body disappeared during sustained motion");
        }
        return fixture;
    }

    private static <T extends BlockEntity> T requireBlockEntity(
            final ServerLevel level, final BlockPos pos, final Class<T> type) {
        final BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!type.isInstance(blockEntity)) {
            throw new GameTestAssertException("Expected " + type.getSimpleName() + " at " + pos + ", got " + blockEntity);
        }
        return type.cast(blockEntity);
    }

    private static void cleanup(final ServerLevel level, final MotionFixture fixture) {
        if (fixture == null) {
            return;
        }
        final ServerSubLevelContainer container = SubLevelContainer.getContainer(level);
        if (container != null && !fixture.subLevel().isRemoved()) {
            container.removeSubLevel(fixture.subLevel(), SubLevelRemovalReason.REMOVED);
        }
        for (final BlockPos pos : fixture.platformBlocks()) {
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        }
    }

    private record MotionFixture(ServerSubLevel subLevel, RigidBodyHandle handle, Vector3d startPosition,
                                 List<BlockPos> actorPositions, List<BlockPos> platformBlocks) {
    }

    private record MotionSample(Vector3d position, Vector3d velocity, double xDisplacement, double zDisplacement,
                                double horizontalDisplacement, double horizontalSpeed) {
    }
}

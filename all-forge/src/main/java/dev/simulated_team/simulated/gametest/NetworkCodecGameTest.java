package dev.simulated_team.simulated.gametest;

import dev.eriksonn.aeronautics.network.packets.LevititeCatalystCrystallizationPacket;
import dev.ryanhcode.offroad.handlers.server.MultiMiningServerManager.BlockBreakingData;
import dev.ryanhcode.offroad.network.borehead_bearing.ClientboundMultiMiningSync;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.physics.force.ForceGroups;
import dev.ryanhcode.sable.api.physics.force.QueuedForceGroup;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.content.blocks.altitude_sensor.AltitudeSensorBlockEntity;
import dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.LinkedTypewriterEntries.KeyboardEntry;
import dev.simulated_team.simulated.content.blocks.redstone.modulating_receiver.ModulatingLinkedReceiverBlockEntity;
import dev.simulated_team.simulated.content.blocks.throttle_lever.ThrottleLeverBlockEntity;
import dev.simulated_team.simulated.content.end_sea.EndSeaPhysics;
import dev.simulated_team.simulated.content.entities.diagram.DiagramConfig;
import dev.simulated_team.simulated.content.physics_staff.PhysicsStaffAction;
import dev.simulated_team.simulated.index.SimBlocks;
import dev.simulated_team.simulated.network.SimNetwork;
import dev.simulated_team.simulated.network.codec.StreamCodec;
import dev.simulated_team.simulated.network.context.ServerPacketContext;
import dev.simulated_team.simulated.network.packet.SimPacket;
import dev.simulated_team.simulated.network.packets.*;
import dev.simulated_team.simulated.network.packets.contraption_diagram.*;
import dev.simulated_team.simulated.network.packets.end_sea.ClientboundEndSeaPacket;
import dev.simulated_team.simulated.network.packets.handle.ClientboundPlayersHoldingHandlePacket;
import dev.simulated_team.simulated.network.packets.honey_glue.*;
import dev.simulated_team.simulated.network.packets.linked_typewriter.*;
import dev.simulated_team.simulated.network.packets.lodestone_compass.UpdateClientLodestonePositionPacket;
import dev.simulated_team.simulated.network.packets.name_plate.NameplateChangeNamePacket;
import dev.simulated_team.simulated.network.packets.physics_assembler.*;
import dev.simulated_team.simulated.network.packets.physics_staff.*;
import dev.simulated_team.simulated.network.packets.rope.*;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import net.createmod.catnip.data.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import org.joml.Quaterniond;
import org.joml.Vector3d;

import java.lang.reflect.Field;
import java.util.*;

@GameTestHolder(Simulated.MOD_ID)
@PrefixGameTestTemplate(false)
public final class NetworkCodecGameTest {
    private static final UUID FIRST_UUID = UUID.fromString("01234567-89ab-cdef-0123-456789abcdef");
    private static final UUID SECOND_UUID = UUID.fromString("fedcba98-7654-3210-fedc-ba9876543210");

    private NetworkCodecGameTest() {
    }

    @GameTest(template = "extrakineticstest.analogtransmission")
    public static void registeredPacketCodecsRoundTrip(final GameTestHelper helper) {
        final BlockPos firstPos = new BlockPos(7, -3, 11);
        final BlockPos secondPos = new BlockPos(-5, 19, 2);
        final KeyboardEntry keyEntry = KeyboardEntry.createFromCodec(
                new ItemStack(Items.DIAMOND, 3), new ItemStack(Items.REDSTONE, 5), 65);
        final DiagramConfig diagramConfig = new DiagramConfig(
                List.of(ResourceLocation.fromNamespaceAndPath("sable", "gravity")), true, true, 32.5, -17.25,
                new DiagramConfig.NoteConfigs(new BoundingBox3d(-1, -2, -3, 4, 5, 6), 8.5, -4.25, true));
        final ClientboundMultiMiningSync miningSync = ClientboundMultiMiningSync.serverOutboundData(73);
        final BlockBreakingData breakingData = new BlockBreakingData(firstPos);
        breakingData.setDestroyProgress(4.0f);
        miningSync.inData.put(firstPos, breakingData);

        final Set<ResourceLocation> packetIds = new HashSet<>();
        int checked = 0;
        checked += verify(packetIds, AssemblePacket.CODEC, new AssemblePacket(firstPos));
        checked += verify(packetIds, BlockEntityObservedPacket.CODEC, new BlockEntityObservedPacket(secondPos));
        checked += verify(packetIds, ConfigureAltitudeSensorPacket.CODEC, new ConfigureAltitudeSensorPacket(firstPos, 0.75f, 0.25f));
        checked += verify(packetIds, ConfigureModulatingLinkedRecieverPacket.CODEC, new ConfigureModulatingLinkedRecieverPacket(secondPos, 2, 14));
        checked += verify(packetIds, ThrottleLeverSignalPacket.CODEC, new ThrottleLeverSignalPacket(firstPos, 13));
        checked += verify(packetIds, NameplateChangeNamePacket.CODEC, new NameplateChangeNamePacket(firstPos, "codec-test"));
        checked += verify(packetIds, TypewriterKeyInteractionPacket.CODEC, new TypewriterKeyInteractionPacket(firstPos, 65, 30, 1));
        checked += verify(packetIds, TypewriterKeySavePacket.CODEC, new TypewriterKeySavePacket(Map.of(65, keyEntry), secondPos, false));
        checked += verify(packetIds, TypewriterDisconnectUser.CODEC, new TypewriterDisconnectUser(firstPos));
        checked += verify(packetIds, TypewriterMenuModifySlots.CODEC, new TypewriterMenuModifySlots(new ItemStack(Items.IRON_INGOT, 2), new ItemStack(Items.GOLD_INGOT, 4)));
        checked += verify(packetIds, TypewriterSaveKeyToItemPacket.CODEC, new TypewriterSaveKeyToItemPacket(InteractionHand.OFF_HAND, keyEntry));
        checked += verify(packetIds, SteeringWheelPacket.CODEC, new SteeringWheelPacket(false, -42.5f, secondPos));
        checked += verify(packetIds, PlaceSpringPacket.CODEC, new PlaceSpringPacket(firstPos, secondPos, Direction.NORTH, Direction.UP, InteractionHand.OFF_HAND));
        checked += verify(packetIds, PlaceMergingGluePacket.CODEC, new PlaceMergingGluePacket(firstPos, secondPos, Direction.WEST, Direction.DOWN, InteractionHand.MAIN_HAND));
        checked += verify(packetIds, UpdatePlayerUsingHandlePacket.CODEC, new UpdatePlayerUsingHandlePacket(6.25f, false, firstPos));
        checked += verify(packetIds, HoneyGlueSpawnPacket.CODEC, new HoneyGlueSpawnPacket(firstPos, secondPos));
        checked += verify(packetIds, HoneyGlueChangeBoundsPacket.CODEC, new HoneyGlueChangeBoundsPacket(new AABB(-1, -2, -3, 4, 5, 6), FIRST_UUID));
        checked += verify(packetIds, HoneyGlueSyncBoundsPacket.CODEC, new HoneyGlueSyncBoundsPacket(new AABB(1, 2, 3, 7, 8, 9), 41, SECOND_UUID));
        checked += verify(packetIds, RopeRidingPacket.CODEC, new RopeRidingPacket(FIRST_UUID, false));
        checked += verify(packetIds, RopeBreakPacket.CODEC, new RopeBreakPacket(SECOND_UUID));
        checked += verify(packetIds, PhysicsStaffActionPacket.CODEC, new PhysicsStaffActionPacket(PhysicsStaffAction.START_DRAG, FIRST_UUID, new Vector3d(1.25, -2.5, 3.75)));
        checked += verify(packetIds, PhysicsStaffDragPacket.CODEC, new PhysicsStaffDragPacket(FIRST_UUID, new Vector3d(1, 2, 3), new Vector3d(-4, 5, -6), new Quaterniond().rotateXYZ(0.1, 0.2, 0.3)));
        checked += verify(packetIds, PhysicsStaffBeamPacket.CODEC, new PhysicsStaffBeamPacket(FIRST_UUID, new Vector3d(1, 2, 3), new Vector3d(4, 5, 6)));
        checked += verify(packetIds, PhysicsStaffLocksPacket.CODEC, new PhysicsStaffLocksPacket(Level.OVERWORLD, List.of(FIRST_UUID, SECOND_UUID)));
        checked += verify(packetIds, PhysicsStaffDragSessionsPacket.CODEC, new PhysicsStaffDragSessionsPacket(Level.OVERWORLD, List.of(Pair.of(FIRST_UUID, new Vector3d(7, 8, 9)))));
        checked += verify(packetIds, PlungerLauncherShootPacket.CODEC, new PlungerLauncherShootPacket(InteractionHand.MAIN_HAND));
        checked += verify(packetIds, PhysicsAssemblerFailedPacket.CODEC, new PhysicsAssemblerFailedPacket(firstPos));
        checked += verify(packetIds, PhysicsAssemblerFlickAndHoldLeverPacket.CODEC, new PhysicsAssemblerFlickAndHoldLeverPacket(secondPos, true));
        checked += verify(packetIds, ClientboundPlayersHoldingHandlePacket.CODEC, new ClientboundPlayersHoldingHandlePacket(List.of(FIRST_UUID, SECOND_UUID)));
        checked += verify(packetIds, ClientboundRopeDataPacket.CODEC, new ClientboundRopeDataPacket(9, firstPos, FIRST_UUID, List.of(new Vector3d(1, 2, 3), new Vector3d(-4, -5, -6)), secondPos, null));
        checked += verify(packetIds, ClientboundRopeStoppedPacket.CODEC, new ClientboundRopeStoppedPacket(secondPos));
        checked += verify(packetIds, ClientboundEndSeaPacket.CODEC, new ClientboundEndSeaPacket(List.of(new EndSeaPhysics(ResourceLocation.fromNamespaceAndPath("minecraft", "overworld"), Optional.of(4), -40.0, 1.5, 0.75))));
        checked += verify(packetIds, RequestDiagramDataPacket.CODEC, new RequestDiagramDataPacket(FIRST_UUID));
        checked += verify(packetIds, DiagramDataPacket.CODEC, new DiagramDataPacket(Map.of(ForceGroups.GRAVITY.get(), List.of(new QueuedForceGroup.PointForce(new Vector3d(1, 2, 3), new Vector3d(4, 5, 6)))), 123.5));
        checked += verify(packetIds, DiagramSaveConfigPacket.CODEC, new DiagramSaveConfigPacket(47, diagramConfig));
        checked += verify(packetIds, DiagramOpenPacket.CODEC, new DiagramOpenPacket(48, diagramConfig));
        checked += verify(packetIds, UpdateClientLodestonePositionPacket.STREAM_CODEC, new UpdateClientLodestonePositionPacket(SECOND_UUID, new Vector3d(-7.5, 8.25, 9.75)));
        checked += verify(packetIds, LevititeCatalystCrystallizationPacket.STREAM_CODEC, new LevititeCatalystCrystallizationPacket(firstPos, InteractionHand.OFF_HAND));
        checked += verifyDirectional(packetIds, ClientboundMultiMiningSync.CODEC, miningSync);

        if (checked != 39 || packetIds.size() != 39) {
            throw new GameTestAssertException("Expected 39 unique registered packet codecs, checked " + checked
                    + " with " + packetIds.size() + " unique packet ids");
        }
        assertRuntimeRegistrationSet(packetIds);
        helper.succeed();
    }

    @GameTest(template = "extrakineticstest.analogtransmission")
    public static void serverboundConfigurationHandlersPersist(final GameTestHelper helper) {
        final BlockPos altitudePos = helper.absolutePos(new BlockPos(1, 20, 1));
        final BlockPos receiverPos = helper.absolutePos(new BlockPos(1, 21, 1));
        final BlockPos throttlePos = helper.absolutePos(new BlockPos(1, 22, 1));
        final ServerLevel level = helper.getLevel();
        final boolean restored = level.getBlockState(altitudePos).is(SimBlocks.ALTITUDE_SENSOR.get());

        if (!restored) {
            level.setBlockAndUpdate(altitudePos, SimBlocks.ALTITUDE_SENSOR.getDefaultState());
            level.setBlockAndUpdate(receiverPos, SimBlocks.MODULATING_LINKED_RECEIVER.getDefaultState());
            level.setBlockAndUpdate(throttlePos, SimBlocks.THROTTLE_LEVER.getDefaultState());
        }

        final AltitudeSensorBlockEntity altitude = requireBlockEntity(level, altitudePos, AltitudeSensorBlockEntity.class);
        final ModulatingLinkedReceiverBlockEntity receiver = requireBlockEntity(level, receiverPos, ModulatingLinkedReceiverBlockEntity.class);
        final ThrottleLeverBlockEntity throttle = requireBlockEntity(level, throttlePos, ThrottleLeverBlockEntity.class);

        if (restored) {
            assertClose("saved altitude high signal", 0.85f, altitude.highSignal);
            assertClose("saved altitude low signal", 0.15f, altitude.lowSignal);
            if (receiver.minRange != 12 || receiver.maxRange != 96) {
                throw new GameTestAssertException("Saved receiver range changed: " + receiver.minRange + ".." + receiver.maxRange);
            }
            if (throttle.getState() != 11) {
                throw new GameTestAssertException("Saved throttle signal changed: " + throttle.getState());
            }
        }

        final ServerPlayer player = FakePlayerFactory.getMinecraft(level);
        player.setPos(altitudePos.getX() + 0.5, altitudePos.getY() + 0.5, altitudePos.getZ() + 0.5);
        transfer(ConfigureAltitudeSensorPacket.CODEC,
                new ConfigureAltitudeSensorPacket(altitudePos, 0.85f, 0.15f)).handle(new ServerPacketContext(player));
        assertClose("packet-applied altitude high signal", 0.85f, altitude.highSignal);
        assertClose("packet-applied altitude low signal", 0.15f, altitude.lowSignal);

        player.setPos(receiverPos.getX() + 0.5, receiverPos.getY() + 0.5, receiverPos.getZ() + 0.5);
        transfer(ConfigureModulatingLinkedRecieverPacket.CODEC,
                new ConfigureModulatingLinkedRecieverPacket(receiverPos, 12, 96)).handle(new ServerPacketContext(player));
        if (receiver.minRange != 12 || receiver.maxRange != 96) {
            throw new GameTestAssertException("Packet did not apply receiver range: " + receiver.minRange + ".." + receiver.maxRange);
        }

        player.setPos(throttlePos.getX() + 0.5, throttlePos.getY() + 0.5, throttlePos.getZ() + 0.5);
        transfer(ThrottleLeverSignalPacket.CODEC,
                new ThrottleLeverSignalPacket(throttlePos, 11)).handle(new ServerPacketContext(player));
        if (throttle.getState() != 11) {
            throw new GameTestAssertException("Packet did not apply throttle signal: " + throttle.getState());
        }

        helper.succeed();
    }

    @GameTest(template = "extrakineticstest.swivelbearing", timeoutTicks = 100)
    public static void serverboundConfigurationHandlerTargetsSubLevel(final GameTestHelper helper) {
        helper.startSequence()
                .thenIdle(20)
                .thenExecute(() -> {
                    final SubLevel subLevel = requireSingleSubLevel(helper);
                    final ServerLevel level = helper.getLevel();
                    final BlockPos storagePos = subLevel.getPlot().getCenterBlock().offset(1, 0, 0);
                    level.setBlockAndUpdate(storagePos, SimBlocks.ALTITUDE_SENSOR.getDefaultState());
                    final AltitudeSensorBlockEntity altitude = requireBlockEntity(level, storagePos, AltitudeSensorBlockEntity.class);
                    final Vec3 physicalPos = Sable.HELPER.projectOutOfSubLevel(level, Vec3.atBottomCenterOf(storagePos));

                    if (physicalPos.distanceToSqr(Vec3.atBottomCenterOf(storagePos)) <= 20.0) {
                        throw new GameTestAssertException("Sub-level packet regression did not separate storage and physical coordinates");
                    }

                    final ServerPlayer player = FakePlayerFactory.getMinecraft(level);
                    player.setPos(physicalPos.x, physicalPos.y, physicalPos.z);
                    transfer(ConfigureAltitudeSensorPacket.CODEC,
                            new ConfigureAltitudeSensorPacket(storagePos, 0.65f, 0.35f)).handle(new ServerPacketContext(player));
                    assertClose("sub-level packet-applied altitude high signal", 0.65f, altitude.highSignal);
                    assertClose("sub-level packet-applied altitude low signal", 0.35f, altitude.lowSignal);
                })
                .thenSucceed();
    }

    private static SubLevel requireSingleSubLevel(final GameTestHelper helper) {
        final double[] bounds = {Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY};
        helper.forEveryBlockInStructure(relative -> {
            final BlockPos absolute = helper.absolutePos(relative);
            bounds[0] = Math.min(bounds[0], absolute.getX());
            bounds[1] = Math.min(bounds[1], absolute.getY());
            bounds[2] = Math.min(bounds[2], absolute.getZ());
            bounds[3] = Math.max(bounds[3], absolute.getX() + 1);
            bounds[4] = Math.max(bounds[4], absolute.getY() + 1);
            bounds[5] = Math.max(bounds[5], absolute.getZ() + 1);
        });

        SubLevel found = null;
        int count = 0;
        for (final SubLevel candidate : Sable.HELPER.getAllIntersecting(helper.getLevel(),
                new BoundingBox3d(bounds[0], bounds[1], bounds[2], bounds[3], bounds[4], bounds[5]))) {
            found = candidate;
            count++;
        }
        if (count != 1) {
            throw new GameTestAssertException("Expected 1 sub-level, found " + count);
        }
        return found;
    }

    private static <T> T requireBlockEntity(final ServerLevel level, final BlockPos pos, final Class<T> type) {
        final Object blockEntity = level.getBlockEntity(pos);
        if (!type.isInstance(blockEntity)) {
            throw new GameTestAssertException("Expected " + type.getSimpleName() + " at " + pos + ", got " + blockEntity);
        }
        return type.cast(blockEntity);
    }

    private static void assertClose(final String label, final float expected, final float actual) {
        if (Math.abs(expected - actual) > 1.0e-6f) {
            throw new GameTestAssertException(label + ": expected " + expected + ", got " + actual);
        }
    }

    private static <P> P transfer(final StreamCodec<? super FriendlyByteBuf, P> codec, final P outbound) {
        final FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            codec.encode(buffer, outbound);
            final P inbound = codec.decode(buffer);
            if (buffer.isReadable()) {
                throw new GameTestAssertException("Transferred packet left " + buffer.readableBytes() + " unread bytes");
            }
            return inbound;
        } finally {
            buffer.release();
        }
    }

    private static void assertRuntimeRegistrationSet(final Set<ResourceLocation> expectedIds) {
        try {
            final Field registryField = SimNetwork.class.getDeclaredField("CHANNELS_BY_TYPE");
            registryField.setAccessible(true);
            final Map<?, ?> registry = (Map<?, ?>) registryField.get(null);
            final Set<ResourceLocation> actualIds = new HashSet<>();
            for (final Object key : registry.keySet()) {
                actualIds.add(((SimPacket.Type<?>) key).id());
            }
            if (!actualIds.equals(expectedIds)) {
                final Set<ResourceLocation> missing = new HashSet<>(actualIds);
                missing.removeAll(expectedIds);
                final Set<ResourceLocation> stale = new HashSet<>(expectedIds);
                stale.removeAll(actualIds);
                throw new GameTestAssertException("Packet codec regression does not match runtime registration; missing samples="
                        + missing + ", stale samples=" + stale);
            }
        } catch (ReflectiveOperationException exception) {
            throw new GameTestAssertException("Unable to inspect runtime packet registration: " + exception);
        }
    }

    private static <P extends SimPacket> int verify(final Set<ResourceLocation> packetIds,
                                                     final StreamCodec<? super FriendlyByteBuf, P> codec,
                                                     final P sample) {
        final ResourceLocation id = sample.type().id();
        if (!packetIds.add(id)) {
            throw new GameTestAssertException("Duplicate packet id in codec regression: " + id);
        }

        final FriendlyByteBuf encoded = new FriendlyByteBuf(Unpooled.buffer());
        final FriendlyByteBuf reencoded = new FriendlyByteBuf(Unpooled.buffer());
        try {
            codec.encode(encoded, sample);
            final P decoded = codec.decode(encoded);
            if (encoded.isReadable()) {
                throw new GameTestAssertException("Codec left " + encoded.readableBytes() + " unread bytes for " + id);
            }
            codec.encode(reencoded, decoded);
            encoded.readerIndex(0);
            if (!ByteBufUtil.equals(encoded, reencoded)) {
                throw new GameTestAssertException("Codec re-encode differed for " + id + ": "
                        + ByteBufUtil.hexDump(encoded) + " != " + ByteBufUtil.hexDump(reencoded));
            }
            return 1;
        } catch (RuntimeException exception) {
            throw new GameTestAssertException("Codec round-trip failed for " + id + ": " + exception);
        } finally {
            encoded.release();
            reencoded.release();
        }
    }

    private static <P extends SimPacket> int verifyDirectional(final Set<ResourceLocation> packetIds,
                                                                final StreamCodec<? super FriendlyByteBuf, P> codec,
                                                                final P outboundSample) {
        final ResourceLocation id = outboundSample.type().id();
        if (!packetIds.add(id)) {
            throw new GameTestAssertException("Duplicate packet id in codec regression: " + id);
        }

        final FriendlyByteBuf encoded = new FriendlyByteBuf(Unpooled.buffer());
        try {
            codec.encode(encoded, outboundSample);
            codec.decode(encoded);
            if (encoded.isReadable()) {
                throw new GameTestAssertException("Codec left " + encoded.readableBytes() + " unread bytes for " + id);
            }
            return 1;
        } catch (RuntimeException exception) {
            throw new GameTestAssertException("Directional codec transfer failed for " + id + ": " + exception);
        } finally {
            encoded.release();
        }
    }
}

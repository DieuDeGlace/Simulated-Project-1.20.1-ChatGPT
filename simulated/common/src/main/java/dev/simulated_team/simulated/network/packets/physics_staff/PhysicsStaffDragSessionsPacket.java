package dev.simulated_team.simulated.network.packets.physics_staff;

import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.SimulatedClient;
import dev.simulated_team.simulated.util.SimCodecUtil;
import dev.simulated_team.simulated.network.context.PacketContext;
import net.createmod.catnip.data.Pair;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import dev.simulated_team.simulated.network.codec.StreamCodec;
import dev.simulated_team.simulated.network.packet.SimPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.joml.Vector3d;

import java.util.List;
import java.util.UUID;

public record PhysicsStaffDragSessionsPacket(ResourceKey<Level> dimension, List<Pair<UUID, Vector3d>> sessions) implements SimPacket {
    public static Type<PhysicsStaffDragSessionsPacket> TYPE = new Type<>(Simulated.path("physics_staff_drag_sessions"), PhysicsStaffDragSessionsPacket.class);

    public static final StreamCodec<FriendlyByteBuf, PhysicsStaffDragSessionsPacket> CODEC = StreamCodec.composite(
            dev.simulated_team.simulated.network.codec.SimVanillaCodecs.resourceKey(Registries.DIMENSION), i -> i.dimension,
            SimCodecUtil.catnipPair(dev.simulated_team.simulated.network.codec.SimVanillaCodecs.UUID,
                    SimCodecUtil.STREAM_VECTOR3D).apply(dev.simulated_team.simulated.network.codec.ByteBufCodecs.list()), i -> i.sessions,
            PhysicsStaffDragSessionsPacket::new
    );

    @Override
    public Type<? extends SimPacket> type() {
        return TYPE;
    }

    public void handle(final PacketContext context) {
        SimulatedClient.PHYSICS_STAFF_CLIENT_HANDLER.setServerDragSessions(this.dimension, this.sessions);
    }
}

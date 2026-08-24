package dev.simulated_team.simulated.network.packets.physics_staff;

import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.SimulatedClient;
import dev.simulated_team.simulated.network.context.PacketContext;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import dev.simulated_team.simulated.network.codec.StreamCodec;
import dev.simulated_team.simulated.network.packet.SimPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class PhysicsStaffLocksPacket implements SimPacket {

    public static Type<PhysicsStaffLocksPacket> TYPE = new Type<>(Simulated.path("physics_staff_locks"), PhysicsStaffLocksPacket.class);

    public static final StreamCodec<FriendlyByteBuf, PhysicsStaffLocksPacket> CODEC = StreamCodec.composite(
            dev.simulated_team.simulated.network.codec.SimVanillaCodecs.resourceKey(Registries.DIMENSION), packet -> packet.dimension,
            dev.simulated_team.simulated.network.codec.SimVanillaCodecs.UUID.apply(
                    dev.simulated_team.simulated.network.codec.ByteBufCodecs.list()), packet -> packet.locks,
            PhysicsStaffLocksPacket::new
    );

    protected final List<UUID> locks;
    private final ResourceKey<Level> dimension;

    public PhysicsStaffLocksPacket(final ResourceKey<Level> dimension, final Collection<UUID> locks) {
        this.dimension = dimension;
        this.locks = new ObjectArrayList<>(locks);
    }

    @Override
    public Type<? extends SimPacket> type() {
        return TYPE;
    }

    public void handle(final PacketContext context) {
        SimulatedClient.PHYSICS_STAFF_CLIENT_HANDLER.setLocks(this.dimension, this.locks);
    }
}

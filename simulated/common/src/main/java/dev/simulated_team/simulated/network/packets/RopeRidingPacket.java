package dev.simulated_team.simulated.network.packets;

import com.simibubi.create.content.kinetics.chainConveyor.ServerChainConveyorHandler;
import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.network.context.ServerPacketContext;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import dev.simulated_team.simulated.network.codec.ByteBufCodecs;
import dev.simulated_team.simulated.network.codec.StreamCodec;
import dev.simulated_team.simulated.network.packet.SimPacket;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public record RopeRidingPacket(UUID uuid, boolean stop) implements SimPacket {
    public static Type<RopeRidingPacket> TYPE = new Type<>(Simulated.path("ride_rope"), RopeRidingPacket.class);

    public static StreamCodec<FriendlyByteBuf, RopeRidingPacket> CODEC = StreamCodec.composite(
            dev.simulated_team.simulated.network.codec.SimVanillaCodecs.UUID, RopeRidingPacket::uuid,
            ByteBufCodecs.BOOL, RopeRidingPacket::stop,
            RopeRidingPacket::new
    );

    @Override
    public Type<? extends SimPacket> type() {
        return TYPE;
    }

    public void handle(final ServerPacketContext ctx) {
        final ServerPlayer player = ctx.player();

        player.connection.aboveGroundTickCount = 0;
        player.connection.aboveGroundVehicleTickCount = 0;
        player.fallDistance = 0.0f;

        if (this.stop)
            ServerChainConveyorHandler.handleStopRidingPacket(player);
        else
            ServerChainConveyorHandler.handleTTLPacket(player);

    }
}

package dev.simulated_team.simulated.network.packets;

import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.content.blocks.throttle_lever.ThrottleLeverBlockEntity;
import dev.simulated_team.simulated.util.hold_interaction.BlockHoldInteraction;
import dev.simulated_team.simulated.network.context.ServerPacketContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import dev.simulated_team.simulated.network.codec.ByteBufCodecs;
import dev.simulated_team.simulated.network.codec.StreamCodec;
import dev.simulated_team.simulated.network.packet.SimPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;

public record ThrottleLeverSignalPacket(BlockPos pos, int signal) implements SimPacket {
    public static final Type<ThrottleLeverSignalPacket> TYPE = new Type<>(Simulated.path("throttle_lever_signal"), ThrottleLeverSignalPacket.class);
    public static final StreamCodec<ByteBuf, ThrottleLeverSignalPacket> CODEC = StreamCodec.composite(
            dev.simulated_team.simulated.network.codec.SimVanillaCodecs.BLOCK_POS, ThrottleLeverSignalPacket::pos,
            ByteBufCodecs.INT, ThrottleLeverSignalPacket::signal,
            ThrottleLeverSignalPacket::new
    );

    @Override
    public Type<ThrottleLeverSignalPacket> type() {
        return TYPE;
    }

    public void handle(final ServerPacketContext context) {
        final ServerPlayer player = context.player();
        final ServerLevel level = (ServerLevel) player.level();

        final BlockEntity blockEntity = level.getBlockEntity(this.pos);

        if (blockEntity instanceof final ThrottleLeverBlockEntity throttleLever) {
            if (!BlockHoldInteraction.inInteractionRange(player, this.pos.getCenter(), 1)) return;

            throttleLever.setSignal(this.signal);
        }
    }
}

package dev.simulated_team.simulated.network.packets;

import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.content.blocks.physics_assembler.PhysicsAssemblerBlockEntity;
import dev.simulated_team.simulated.index.SimStats;
import dev.simulated_team.simulated.network.context.ServerPacketContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import dev.simulated_team.simulated.network.codec.StreamCodec;
import dev.simulated_team.simulated.network.packet.SimPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

public record AssemblePacket(BlockPos pos) implements SimPacket {
    public static final Type<AssemblePacket> TYPE = new Type<>(Simulated.path("assemble"), AssemblePacket.class);

    public static final StreamCodec<ByteBuf, AssemblePacket> CODEC = StreamCodec.composite(
            dev.simulated_team.simulated.network.codec.SimVanillaCodecs.BLOCK_POS, packet -> packet.pos,
            AssemblePacket::new);

    @Override
    public Type<AssemblePacket> type() {
        return TYPE;
    }

    public void handle(final ServerPacketContext context) {
        final ServerLevel level = (ServerLevel) context.player().level();

        final BlockEntity blockEntity = level.getBlockEntity(this.pos);

        if (blockEntity instanceof final PhysicsAssemblerBlockEntity assembler) {
            assembler.assembleOrDisassemble();
            SimStats.INTERACT_WITH_ASSEMBLER.awardTo(context.player());
        }
    }
}

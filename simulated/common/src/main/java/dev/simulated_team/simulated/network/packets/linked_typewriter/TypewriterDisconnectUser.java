package dev.simulated_team.simulated.network.packets.linked_typewriter;

import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.LinkedTypewriterBlockEntity;
import dev.simulated_team.simulated.network.context.ServerPacketContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import dev.simulated_team.simulated.network.codec.StreamCodec;
import dev.simulated_team.simulated.network.packet.SimPacket;

public record TypewriterDisconnectUser(BlockPos pos) implements SimPacket {

    public static Type<TypewriterDisconnectUser> TYPE = new Type<>(Simulated.path("typewriter_disconnect_user"), TypewriterDisconnectUser.class);

    public static StreamCodec<ByteBuf, TypewriterDisconnectUser> CODEC = StreamCodec.composite(
            dev.simulated_team.simulated.network.codec.SimVanillaCodecs.BLOCK_POS, TypewriterDisconnectUser::pos, TypewriterDisconnectUser::new
    );

    public void handle(final ServerPacketContext context) {
        if (context.level().getBlockEntity(this.pos) instanceof final LinkedTypewriterBlockEntity lbe) {
            if (lbe.checkUser(context.player().getUUID())) {
                lbe.disconnectUser();
            }
        }
    }

    @Override
    public Type<? extends SimPacket> type() {
        return TYPE;
    }
}

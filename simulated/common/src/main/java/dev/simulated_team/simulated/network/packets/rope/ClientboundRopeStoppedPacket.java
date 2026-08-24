package dev.simulated_team.simulated.network.packets.rope;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.content.blocks.rope.RopeStrandHolderBehavior;
import dev.simulated_team.simulated.network.context.ClientPacketContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import dev.simulated_team.simulated.network.codec.StreamCodec;
import dev.simulated_team.simulated.network.packet.SimPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public record ClientboundRopeStoppedPacket(BlockPos ownerPos) implements SimPacket {
    public static final StreamCodec<ByteBuf, ClientboundRopeStoppedPacket> CODEC = dev.simulated_team.simulated.network.codec.SimVanillaCodecs.BLOCK_POS.map(ClientboundRopeStoppedPacket::new, ClientboundRopeStoppedPacket::ownerPos);
    public static Type<ClientboundRopeStoppedPacket> TYPE = new Type<>(Simulated.path("rope_stopped"), ClientboundRopeStoppedPacket.class);

    public void handle(final ClientPacketContext context) {
        final LocalPlayer player = context.player();
        final Level level = player.level();

        final BlockEntity blockEntity = level.getBlockEntity(this.ownerPos);

        if (!(blockEntity instanceof final SmartBlockEntity smartBlockEntity)) {
            return;
        }

        final RopeStrandHolderBehavior ropeHolder = smartBlockEntity.getBehaviour(RopeStrandHolderBehavior.TYPE);

        if (ropeHolder == null) {
            return;
        }

        ropeHolder.receiveClientStrandStopped();
    }

    @Override
    public Type<? extends SimPacket> type() {
        return TYPE;
    }
}

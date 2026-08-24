package dev.simulated_team.simulated.network.packets.physics_assembler;

import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.content.blocks.physics_assembler.PhysicsAssemblerBlockEntity;
import dev.simulated_team.simulated.network.context.ClientPacketContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import dev.simulated_team.simulated.network.codec.ByteBufCodecs;
import dev.simulated_team.simulated.network.codec.StreamCodec;
import dev.simulated_team.simulated.network.packet.SimPacket;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public record PhysicsAssemblerFlickAndHoldLeverPacket(BlockPos pos, boolean flicked) implements SimPacket {

    public static Type<PhysicsAssemblerFlickAndHoldLeverPacket> TYPE = new Type<>(Simulated.path("flick_assembler_lever"), PhysicsAssemblerFlickAndHoldLeverPacket.class);
    public static StreamCodec<ByteBuf, PhysicsAssemblerFlickAndHoldLeverPacket> CODEC = StreamCodec.composite(
            dev.simulated_team.simulated.network.codec.SimVanillaCodecs.BLOCK_POS,
            PhysicsAssemblerFlickAndHoldLeverPacket::pos,
            ByteBufCodecs.BOOL,
            PhysicsAssemblerFlickAndHoldLeverPacket::flicked,
            PhysicsAssemblerFlickAndHoldLeverPacket::new
    );

    @Override
    public @NotNull Type<? extends SimPacket> type() {
        return TYPE;
    }

    public void handle(final ClientPacketContext context) {
        final Level level = context.level();

        assert level != null;

        if (level.getBlockEntity(this.pos) instanceof final PhysicsAssemblerBlockEntity blockEntity) {
            blockEntity.clientFlickLeverTo(this.flicked);
            blockEntity.setClientHoldLeverInPlace(true);
        }
    }
}

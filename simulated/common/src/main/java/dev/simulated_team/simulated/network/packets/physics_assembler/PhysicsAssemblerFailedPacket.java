package dev.simulated_team.simulated.network.packets.physics_assembler;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.SubLevelHelper;
import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.content.blocks.physics_assembler.PhysicsAssemblerBlockEntity;
import dev.simulated_team.simulated.index.SimSoundEvents;
import dev.simulated_team.simulated.network.context.ClientPacketContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import dev.simulated_team.simulated.network.codec.StreamCodec;
import dev.simulated_team.simulated.network.packet.SimPacket;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public record PhysicsAssemblerFailedPacket(BlockPos pos) implements SimPacket {

    public static Type<PhysicsAssemblerFailedPacket> TYPE = new Type<>(Simulated.path("assembler_failed"), PhysicsAssemblerFailedPacket.class);
    public static StreamCodec<ByteBuf, PhysicsAssemblerFailedPacket> CODEC = StreamCodec.composite(
            dev.simulated_team.simulated.network.codec.SimVanillaCodecs.BLOCK_POS,
            PhysicsAssemblerFailedPacket::pos,
            PhysicsAssemblerFailedPacket::new
    );

    @Override
    public @NotNull Type<? extends SimPacket> type() {
        return TYPE;
    }

    public void handle(final ClientPacketContext context) {
        final Level level = context.level();

        assert level != null;

        if (level.getBlockEntity(this.pos) instanceof final PhysicsAssemblerBlockEntity blockEntity) {
            blockEntity.clientFlickLeverTo(Sable.HELPER.getContaining(level, this.pos) != null);
            blockEntity.setClientHoldLeverInPlace(false);
            SimSoundEvents.ASSEMBLER_FAIL.playAt(level, this.pos, 1.0f, 1.0f, false);
        }
    }
}

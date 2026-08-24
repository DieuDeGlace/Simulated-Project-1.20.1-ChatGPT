package dev.simulated_team.simulated.network.packets;

import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.util.Observable;
import dev.simulated_team.simulated.network.context.ServerPacketContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import dev.simulated_team.simulated.network.codec.StreamCodec;
import dev.simulated_team.simulated.network.packet.SimPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.ForgeMod;
import org.jetbrains.annotations.NotNull;

public record BlockEntityObservedPacket(BlockPos pos) implements SimPacket {

    public static Type<BlockEntityObservedPacket> TYPE = new Type<>(Simulated.path("be_observed"), BlockEntityObservedPacket.class);
    public static StreamCodec<ByteBuf, BlockEntityObservedPacket> CODEC = dev.simulated_team.simulated.network.codec.SimVanillaCodecs.BLOCK_POS.map(BlockEntityObservedPacket::new, BlockEntityObservedPacket::pos);

    @Override
    public @NotNull Type<? extends SimPacket> type() {
        return TYPE;
    }

    public void handle(final ServerPacketContext context) {
        final Level level = context.level();
        final ServerPlayer player = context.player();

        // More than 4 blocks + interaction range is way too far to observe a block
        final double interactionRange = player.getAttributeValue(ForgeMod.BLOCK_REACH.get()) + 4.0;
        if (new AABB(this.pos).distanceToSqr(player.getEyePosition()) >= interactionRange * interactionRange) {
            return;
        }

        if (level.getBlockEntity(this.pos) instanceof final Observable observable) {
            observable.onObserved(player);
        }
    }
}

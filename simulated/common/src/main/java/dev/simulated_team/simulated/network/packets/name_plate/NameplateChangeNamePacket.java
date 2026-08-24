package dev.simulated_team.simulated.network.packets.name_plate;

import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.content.blocks.nameplate.NameplateBlockEntity;
import dev.simulated_team.simulated.network.context.ServerPacketContext;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import dev.simulated_team.simulated.network.codec.ByteBufCodecs;
import dev.simulated_team.simulated.network.codec.StreamCodec;
import dev.simulated_team.simulated.network.packet.SimPacket;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public record NameplateChangeNamePacket(BlockPos controllerPos, @Nullable String name) implements SimPacket {

    public static Type<NameplateChangeNamePacket> TYPE = new Type<>(Simulated.path("nameplate_change_name"), NameplateChangeNamePacket.class);

    public static StreamCodec<FriendlyByteBuf, NameplateChangeNamePacket> CODEC = StreamCodec.composite(
            dev.simulated_team.simulated.network.codec.SimVanillaCodecs.BLOCK_POS, NameplateChangeNamePacket::controllerPos,
            ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8), (packet) -> Optional.ofNullable(packet.name()),
            NameplateChangeNamePacket::fromCodec);

    public static NameplateChangeNamePacket fromCodec(final BlockPos controllerPos, final Optional<String> name) {
        return new NameplateChangeNamePacket(controllerPos, name.orElse(null));
    }

    public void handle(final ServerPacketContext context) {
        final Level level = context.level();
        if (level.getBlockEntity(this.controllerPos()) instanceof final NameplateBlockEntity nbe && nbe.allowsEditing()) {
            nbe.setName(this.name, true, context.player());
        }
    }

    @Override
    public Type<? extends SimPacket> type() {
        return TYPE;
    }
}

package dev.simulated_team.simulated.network.packets;

import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.content.blocks.redstone.modulating_receiver.ModulatingLinkedReceiverBlockEntity;
import dev.simulated_team.simulated.network.packets.helpers.SimBlockEntityConfigurationPacket;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import dev.simulated_team.simulated.network.codec.ByteBufCodecs;
import dev.simulated_team.simulated.network.codec.StreamCodec;
import dev.simulated_team.simulated.network.packet.SimPacket;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

public class ConfigureModulatingLinkedRecieverPacket extends SimBlockEntityConfigurationPacket<ModulatingLinkedReceiverBlockEntity> {
    public static final Type<ConfigureModulatingLinkedRecieverPacket> TYPE = new Type<>(
            Simulated.path("configure_modulating_linked_reciever"), ConfigureModulatingLinkedRecieverPacket.class);
    public static final StreamCodec<ByteBuf, ConfigureModulatingLinkedRecieverPacket> CODEC = StreamCodec.composite(
            dev.simulated_team.simulated.network.codec.SimVanillaCodecs.BLOCK_POS, SimBlockEntityConfigurationPacket::getPos,
            ByteBufCodecs.INT, ConfigureModulatingLinkedRecieverPacket::getMinRange,
            ByteBufCodecs.INT, ConfigureModulatingLinkedRecieverPacket::getMaxRange,
            ConfigureModulatingLinkedRecieverPacket::new);

    private final int minRange;
    private final int maxRange;

    public ConfigureModulatingLinkedRecieverPacket(final BlockPos pos, final int minRange, final int maxRange) {
        super(pos);
        this.minRange = minRange;
        this.maxRange = maxRange;
    }

    public int getMinRange() {
        return this.minRange;
    }

    public int getMaxRange() {
        return this.maxRange;
    }

    @Override
    public @NotNull Type<? extends SimPacket> type() {
        return TYPE;
    }

    @Override
    protected void applySettings(final ServerPlayer serverPlayer, final ModulatingLinkedReceiverBlockEntity be) {
        be.minRange = this.minRange;
        be.maxRange = this.maxRange;
        be.notifyUpdate();
    }
}

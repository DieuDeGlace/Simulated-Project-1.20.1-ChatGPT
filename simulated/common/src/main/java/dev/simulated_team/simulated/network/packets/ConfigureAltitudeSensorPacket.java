package dev.simulated_team.simulated.network.packets;

import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.content.blocks.altitude_sensor.AltitudeSensorBlockEntity;
import dev.simulated_team.simulated.network.packets.helpers.SimBlockEntityConfigurationPacket;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import dev.simulated_team.simulated.network.codec.ByteBufCodecs;
import dev.simulated_team.simulated.network.codec.StreamCodec;
import dev.simulated_team.simulated.network.packet.SimPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

public class ConfigureAltitudeSensorPacket extends SimBlockEntityConfigurationPacket<AltitudeSensorBlockEntity> {
    public static final Type<ConfigureAltitudeSensorPacket> TYPE = new Type<>(
            Simulated.path("configure_altitude_sensor"), ConfigureAltitudeSensorPacket.class);
    public static final StreamCodec<ByteBuf, ConfigureAltitudeSensorPacket> CODEC = StreamCodec.composite(
            dev.simulated_team.simulated.network.codec.SimVanillaCodecs.BLOCK_POS, SimBlockEntityConfigurationPacket::getPos,
            ByteBufCodecs.FLOAT, ConfigureAltitudeSensorPacket::highSignal,
            ByteBufCodecs.FLOAT, ConfigureAltitudeSensorPacket::lowSignal,
            ConfigureAltitudeSensorPacket::new
    );

    private final float highSignal;
    private final float lowSignal;

    public ConfigureAltitudeSensorPacket(final BlockPos pos, final float highSignal, final float lowSignal) {
        super(pos);
        this.highSignal = Mth.clamp(highSignal, 0.0f, 1.0f);
        this.lowSignal = Mth.clamp(lowSignal, 0.0f, 1.0f);
    }

    private float lowSignal() {
        return this.lowSignal;
    }

    private float highSignal() {
        return this.highSignal;
    }


    @Override
    public @NotNull Type<? extends SimPacket> type() {
        return TYPE;
    }

    @Override
    protected void applySettings(final ServerPlayer serverPlayer, final AltitudeSensorBlockEntity be) {
        be.highSignal = this.highSignal;
        be.lowSignal = this.lowSignal;
        be.notifyUpdate();
    }
}

package dev.simulated_team.simulated.network.packet;

import net.minecraft.resources.ResourceLocation;

/**
 * Packet identity used by the Forge 1.20.1 transport. The packet class is
 * carried explicitly because Forge SimpleChannel registers messages by class.
 */
public interface SimPacket {
    Type<? extends SimPacket> type();

    record Type<T extends SimPacket>(ResourceLocation id, Class<T> packetClass) {
    }
}

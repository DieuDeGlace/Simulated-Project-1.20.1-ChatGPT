package dev.simulated_team.simulated.network.packets.handle;

import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.content.blocks.handle.PlayerHoldingHandleRenderer;
import dev.simulated_team.simulated.network.context.ClientPacketContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import dev.simulated_team.simulated.network.codec.ByteBufCodecs;
import dev.simulated_team.simulated.network.codec.StreamCodec;
import dev.simulated_team.simulated.network.packet.SimPacket;

import java.util.Collection;
import java.util.HashSet;
import java.util.UUID;

public record ClientboundPlayersHoldingHandlePacket(Collection<UUID> uuids) implements SimPacket {
	public static Type<ClientboundPlayersHoldingHandlePacket> TYPE = new Type<>(Simulated.path("players_holding_handles"), ClientboundPlayersHoldingHandlePacket.class);
	public static final StreamCodec<ByteBuf, ClientboundPlayersHoldingHandlePacket> CODEC = StreamCodec.composite(
		ByteBufCodecs.collection(HashSet::new, dev.simulated_team.simulated.network.codec.SimVanillaCodecs.UUID), ClientboundPlayersHoldingHandlePacket::uuids,
	    ClientboundPlayersHoldingHandlePacket::new
	);

	public void handle(final ClientPacketContext context) {
		PlayerHoldingHandleRenderer.updatePlayerList(this.uuids);
	}

	@Override
	public Type<? extends SimPacket> type() {
		return TYPE;
	}
}

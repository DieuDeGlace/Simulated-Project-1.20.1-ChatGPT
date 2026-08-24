package dev.simulated_team.simulated.network.packets.lodestone_compass;

import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.content.navigation_targets.lodestone_compass_compatability.ClientLodestonePositions;
import dev.simulated_team.simulated.network.context.PacketContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.UUIDUtil;
import dev.simulated_team.simulated.network.codec.StreamCodec;
import dev.simulated_team.simulated.network.packet.SimPacket;
import net.minecraft.world.level.Level;
import org.joml.Vector3d;

import java.util.UUID;

public record UpdateClientLodestonePositionPacket(UUID id, Vector3d sentPosition) implements SimPacket {

	public static final Type<UpdateClientLodestonePositionPacket> TYPE = new Type<>(Simulated.path("update_client_lodestone"), UpdateClientLodestonePositionPacket.class);

	public static final StreamCodec<ByteBuf, UpdateClientLodestonePositionPacket> STREAM_CODEC = StreamCodec.composite(
			dev.simulated_team.simulated.network.codec.SimVanillaCodecs.UUID, UpdateClientLodestonePositionPacket::id,
			StreamCodec.of((byteBuf, p) -> {
				byteBuf.writeDouble(p.x);
				byteBuf.writeDouble(p.y);
				byteBuf.writeDouble(p.z);
			}, (byteBuf) ->
					new Vector3d(byteBuf.readDouble(), byteBuf.readDouble(), byteBuf.readDouble())), UpdateClientLodestonePositionPacket::sentPosition,
			UpdateClientLodestonePositionPacket::new
	);

	@Override
	public Type<? extends SimPacket> type() {
		return TYPE;
	}

	public void handle(final PacketContext context) {
		final Level level = context.level();
		if (level instanceof final ClientLevel cl) {
			final ClientLodestonePositions positions = ClientLodestonePositions.clientPositions.get(cl);
			positions.CLIENT_LODESTONE_MAP.put(this.id, this.sentPosition);
		}
	}
}

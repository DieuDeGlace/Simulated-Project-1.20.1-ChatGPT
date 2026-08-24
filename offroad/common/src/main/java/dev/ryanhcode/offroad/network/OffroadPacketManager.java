package dev.ryanhcode.offroad.network;

import dev.ryanhcode.offroad.Offroad;
import dev.ryanhcode.offroad.network.borehead_bearing.ClientboundMultiMiningSync;
import dev.simulated_team.simulated.network.SimNetwork;

public class OffroadPacketManager {
	public static SimNetwork INSTANCE = SimNetwork.create(Offroad.MOD_ID, "0.1");

	public static void init() {
		INSTANCE.registerClientbound(ClientboundMultiMiningSync.TYPE, ClientboundMultiMiningSync.CODEC, ClientboundMultiMiningSync::handle);
	}
}

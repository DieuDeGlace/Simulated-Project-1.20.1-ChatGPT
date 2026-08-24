package dev.eriksonn.aeronautics.network;

import dev.eriksonn.aeronautics.Aeronautics;
import dev.eriksonn.aeronautics.network.packets.LevititeCatalystCrystallizationPacket;
import dev.simulated_team.simulated.network.SimNetwork;

public class AeroPacketManager {
	public static SimNetwork INSTANCE = SimNetwork.create(Aeronautics.MOD_ID, "0.1");

	public static void init() {
		INSTANCE.registerServerbound(LevititeCatalystCrystallizationPacket.TYPE, LevititeCatalystCrystallizationPacket.STREAM_CODEC, LevititeCatalystCrystallizationPacket::handle);
	}
}

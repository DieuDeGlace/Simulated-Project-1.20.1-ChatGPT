package dev.ryanhcode.offroad.neoforge;

import dev.ryanhcode.offroad.Offroad;
import dev.ryanhcode.offroad.OffroadClient;
import net.createmod.catnip.config.ui.BaseConfigScreen;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;

public class OffroadNeoForgeClient {
	public OffroadNeoForgeClient(final IEventBus modBus) {
		this.listenClientEvents(modBus);
		ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
				() -> new ConfigScreenHandler.ConfigScreenFactory(parent -> new BaseConfigScreen(parent, Offroad.MOD_ID)));

		OffroadClient.init();
	}

	private void listenClientEvents(final IEventBus modBus) {

	}
}

package dev.eriksonn.aeronautics.neoforge;

import com.google.common.collect.ImmutableList;
import dev.eriksonn.aeronautics.Aeronautics;
import dev.eriksonn.aeronautics.AeronauticsClient;
import dev.eriksonn.aeronautics.index.client.AeroRenderTypes;
import dev.eriksonn.aeronautics.neoforge.events.AeroNeoForgeClientEvents;
import dev.eriksonn.aeronautics.neoforge.mixin.ForgeRenderTypeStageHandlerAccessor;
import foundry.veil.forge.ForgeRenderTypeStageHandler;
import foundry.veil.forge.event.ForgeVeilRegisterBlockLayerEvent;
import net.minecraft.client.renderer.RenderType;
import net.createmod.catnip.config.ui.BaseConfigScreen;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class AeronauticsNeoForgeClient {
	public AeronauticsNeoForgeClient(final IEventBus modBus) {
		MinecraftForge.EVENT_BUS.register(AeroNeoForgeClientEvents.class);
		modBus.register(AeroNeoForgeClientEvents.ModBusEvents.class);
		ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
				() -> new ConfigScreenHandler.ConfigScreenFactory(parent -> new BaseConfigScreen(parent, Aeronautics.MOD_ID)));

		modBus.<ForgeVeilRegisterBlockLayerEvent>addListener(event -> {
			event.registerBlockLayer(AeroRenderTypes.levitite());
			event.registerBlockLayer(AeroRenderTypes.levititeGhosts());
		});
		installLateVeilBlockLayers();

		AeronauticsClient.init();
	}

	private static void installLateVeilBlockLayers() {
		final List<RenderType> currentLayers = ForgeRenderTypeStageHandler.getBlockLayers();
		if (currentLayers == null) {
			return;
		}

		final Set<RenderType> currentCustomLayers = ForgeRenderTypeStageHandlerAccessor.getCustomBlockLayers();
		final Set<RenderType> orderedCustomLayers = new LinkedHashSet<>();
		for (final RenderType renderType : currentLayers) {
			if (currentCustomLayers.contains(renderType)) {
				orderedCustomLayers.add(renderType);
			}
		}
		orderedCustomLayers.add(AeroRenderTypes.levitite());
		orderedCustomLayers.add(AeroRenderTypes.levititeGhosts());

		ForgeRenderTypeStageHandlerAccessor.setBlockLayers(null);
		final ImmutableList.Builder<RenderType> builder = ImmutableList.builder();
		builder.addAll(orderedCustomLayers);
		ForgeRenderTypeStageHandler.setBlockLayers(builder);
	}
}

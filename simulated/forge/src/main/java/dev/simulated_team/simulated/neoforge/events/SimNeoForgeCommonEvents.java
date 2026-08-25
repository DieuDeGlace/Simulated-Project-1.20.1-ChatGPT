package dev.simulated_team.simulated.neoforge.events;


import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.command.SimCommand;
import dev.simulated_team.simulated.content.end_sea.EndSeaPhysicsData;
import dev.simulated_team.simulated.data.advancements.SimAdvancementTriggers;
import dev.simulated_team.simulated.data.advancements.SimAdvancements;
import dev.simulated_team.simulated.data.neoforge.SimProcessingRecipeGen;
import dev.simulated_team.simulated.events.SimulatedCommonEvents;
import dev.simulated_team.simulated.index.SimArmInteractions;
import dev.simulated_team.simulated.index.SimDataComponents;
import dev.simulated_team.simulated.index.SimSoundEvents;
import dev.simulated_team.simulated.index.SimTags;
import dev.simulated_team.simulated.index.neoforge.NeoForgeSimStats;
import dev.simulated_team.simulated.network.SimNetwork;
import dev.simulated_team.simulated.multiloader.energy.SingleBattery;
import dev.simulated_team.simulated.multiloader.energy.SingleBatteryWrapper;
import dev.simulated_team.simulated.multiloader.inventory.AbstractContainer;
import dev.simulated_team.simulated.multiloader.inventory.neoforge.ContainerWrapper;
import dev.simulated_team.simulated.multiloader.tanks.SingleTank;
import dev.simulated_team.simulated.multiloader.tanks.neoforge.SingleTankWrapper;
import dev.simulated_team.simulated.neoforge.service.NeoForgeSimConfigService;
import dev.simulated_team.simulated.neoforge.service.NeoForgeSimInventoryService;
import dev.simulated_team.simulated.util.hold_interaction.HoldInteractionManager;
import net.createmod.catnip.config.ConfigBase;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Direction;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.event.TickEvent.ServerTickEvent;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

@EventBusSubscriber(modid = Simulated.MOD_ID)
public class SimNeoForgeCommonEvents {

	@SubscribeEvent
	public static void loadChunk(final ChunkEvent.Load event) {
		SimulatedCommonEvents.onChunkLoad(event.getLevel(), event.getChunk(), event.isNewChunk());
	}

	@SubscribeEvent
	public static void playerLoggedIn(final PlayerEvent.PlayerLoggedInEvent event) {
		final Player player = event.getEntity();
		SimulatedCommonEvents.onPlayerLoggedIn(player);
	}

	@SubscribeEvent
	public static void registerCommands(final RegisterCommandsEvent event) {
		SimCommand.register(event.getDispatcher(), event.getBuildContext());
	}

	@SubscribeEvent
	public static void serverStopped(final ServerStoppedEvent event) {
		SimulatedCommonEvents.onServerStopped(event.getServer());
	}

	@SubscribeEvent
	public static void postServerTick(final ServerTickEvent event) {
		if (event.phase != net.minecraftforge.event.TickEvent.Phase.END) {
			return;
		}
		final MinecraftServer server = event.getServer();
		for (final ServerLevel level : server.getAllLevels()) {
			SimulatedCommonEvents.onServerTickEnd(level);
		}
	}

	@SubscribeEvent
	public static void syncDataPack(final OnDatapackSyncEvent event) {
		EndSeaPhysicsData.syncDataPacket(packet -> {
			if (event.getPlayer() != null) {
				SimNetwork.player(event.getPlayer()).sendPacket(packet);
			} else {
				event.getPlayerList().getPlayers().forEach(player -> SimNetwork.player(player).sendPacket(packet));
			}
		});
	}

	@SubscribeEvent
	public static void addReloadListeners(final AddReloadListenerEvent event) {
		event.addListener(EndSeaPhysicsData.ReloadListener.INSTANCE);
	}

	@SubscribeEvent
	public static void rightClickBlock(final PlayerInteractEvent.RightClickBlock event) {
		final InteractionResult result = SimulatedCommonEvents.rightClickBlock(event.getLevel(), event.getPos(), event.getEntity(), event.getItemStack());
		if (result != null) {
			event.setCancellationResult(result);
			event.setCanceled(true);
		}
	}

	@SubscribeEvent
	public static void addDefaultItemAttributes(final ItemAttributeModifierEvent event) {
		SimulatedCommonEvents.addDefaultAttributeModifiers(
				event.getItemStack(), event.getSlotType(), event::addModifier);
	}

	@SubscribeEvent
	public static void attachStorageCapabilities(final AttachCapabilitiesEvent<BlockEntity> event) {
		final BlockEntity blockEntity = event.getObject();
		if (!hasStorageRegistration(blockEntity)) {
			return;
		}

		final StorageCapabilityProvider provider = new StorageCapabilityProvider(blockEntity);
		event.addCapability(Simulated.path("storage"), provider);
		event.addListener(provider::invalidate);
	}

	private static boolean hasStorageRegistration(final BlockEntity blockEntity) {
		return NeoForgeSimInventoryService.inventoryGetters.stream().anyMatch(holder -> holder.type() == blockEntity.getType())
				|| NeoForgeSimInventoryService.fluidTankGetters.stream().anyMatch(holder -> holder.type() == blockEntity.getType())
				|| NeoForgeSimInventoryService.energyGetters.stream().anyMatch(holder -> holder.type() == blockEntity.getType());
	}

	private static final class StorageCapabilityProvider implements ICapabilityProvider {
		private final BlockEntity blockEntity;
		private final Map<Direction, CapabilityEntry<AbstractContainer, IItemHandler>> itemHandlers = new HashMap<>();
		private final Map<Direction, CapabilityEntry<SingleTank, IFluidHandler>> fluidHandlers = new HashMap<>();
		private final Map<Direction, CapabilityEntry<SingleBattery, IEnergyStorage>> energyHandlers = new HashMap<>();

		private StorageCapabilityProvider(final BlockEntity blockEntity) {
			this.blockEntity = blockEntity;
		}

		@Override
		public <T> @NotNull LazyOptional<T> getCapability(final @NotNull Capability<T> capability,
														 final @Nullable Direction side) {
			if (capability == ForgeCapabilities.ITEM_HANDLER) {
				return this.createItemHandler(side).cast();
			}
			if (capability == ForgeCapabilities.FLUID_HANDLER) {
				return this.createFluidHandler(side).cast();
			}
			if (capability == ForgeCapabilities.ENERGY) {
				return this.createEnergyHandler(side).cast();
			}
			return LazyOptional.empty();
		}

		private LazyOptional<IItemHandler> createItemHandler(final Direction side) {
			for (final NeoForgeSimInventoryService.InventoryGetterHolder<? extends BlockEntity> holder : NeoForgeSimInventoryService.inventoryGetters) {
				if (holder.type() == this.blockEntity.getType()) {
					final AbstractContainer container = holder.castBlockEntityAndGetInv(this.blockEntity, side);
					return update(this.itemHandlers, side, container, ContainerWrapper::new);
				}
			}
			return update(this.itemHandlers, side, null, ContainerWrapper::new);
		}

		private LazyOptional<IFluidHandler> createFluidHandler(final Direction side) {
			for (final NeoForgeSimInventoryService.TankGetterHolder<? extends BlockEntity> holder : NeoForgeSimInventoryService.fluidTankGetters) {
				if (holder.type() == this.blockEntity.getType()) {
					final SingleTank tank = holder.castBlockEntityAndGetInv(this.blockEntity, side);
					return update(this.fluidHandlers, side, tank, SingleTankWrapper::new);
				}
			}
			return update(this.fluidHandlers, side, null, SingleTankWrapper::new);
		}

		private LazyOptional<IEnergyStorage> createEnergyHandler(final Direction side) {
			for (final NeoForgeSimInventoryService.EnergyGetterHolder<? extends BlockEntity> holder : NeoForgeSimInventoryService.energyGetters) {
				if (holder.type() == this.blockEntity.getType()) {
					final SingleBattery battery = holder.castBlockEntityAndGetInv(this.blockEntity, side);
					return update(this.energyHandlers, side, battery, SingleBatteryWrapper::new);
				}
			}
			return update(this.energyHandlers, side, null, SingleBatteryWrapper::new);
		}

		private static <S, T> LazyOptional<T> update(final Map<Direction, CapabilityEntry<S, T>> cache,
				final Direction side, final @Nullable S source, final Function<S, T> wrapper) {
			final CapabilityEntry<S, T> previous = cache.get(side);
			if (previous != null && previous.source() == source) {
				return previous.capability();
			}
			if (previous != null) {
				previous.capability().invalidate();
				cache.remove(side);
			}
			if (source == null) {
				return LazyOptional.empty();
			}
			final LazyOptional<T> capability = LazyOptional.of(() -> wrapper.apply(source));
			cache.put(side, new CapabilityEntry<>(source, capability));
			return capability;
		}

		private void invalidate() {
			invalidate(this.itemHandlers);
			invalidate(this.fluidHandlers);
			invalidate(this.energyHandlers);
		}

		private static void invalidate(final Map<?, ? extends CapabilityEntry<?, ?>> cache) {
			cache.values().forEach(entry -> entry.capability().invalidate());
			cache.clear();
		}

		private record CapabilityEntry<S, T>(S source, LazyOptional<T> capability) {
		}
	}

	@EventBusSubscriber(modid = Simulated.MOD_ID)
	public static class ModBusEvents {

		@SubscribeEvent(priority = EventPriority.HIGHEST)
		public static void gatherDataHighPriority(final GatherDataEvent event) {
			SimTags.addGenerators();
		}

		@SubscribeEvent
		public static void gatherData(final GatherDataEvent event) {
			final DataGenerator generator = event.getGenerator();

			final PackOutput output = generator.getPackOutput();
			final CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();

			if (event.includeClient()) {
				generator.addProvider(true, SimSoundEvents.REGISTRY.getProvider(output));
			}

			generator.addProvider(event.includeServer(), new SimAdvancements(output, lookupProvider));
			generator.addProvider(event.includeServer(), SimProcessingRecipeGen.registerAll(output, lookupProvider));
		}

		@SubscribeEvent
		public static void loadConfig(final ModConfigEvent.Loading event) {
			for (final ConfigBase config : NeoForgeSimConfigService.CONFIGS.values()) {
				if (config.specification == event.getConfig().getSpec()) {
					config.onLoad();
				}
			}

		}

		@SubscribeEvent
		public static void reloadConfig(final ModConfigEvent.Reloading event) {
			for (final ConfigBase config : NeoForgeSimConfigService.CONFIGS.values()) {
				if (config.specification == event.getConfig().getSpec()) {
					config.onReload();
				}
			}

		}

		@SubscribeEvent
		public static void commonSetup(final FMLCommonSetupEvent event) {
			event.enqueueWork(() -> {
				SimAdvancements.register();
				SimAdvancementTriggers.register();
				SimDataComponents.registerDefaults();
			});
		}

		@SubscribeEvent
		public static void postRegister(final FMLLoadCompleteEvent event) {
			NeoForgeSimStats.bootstrap();
		}
	}

}

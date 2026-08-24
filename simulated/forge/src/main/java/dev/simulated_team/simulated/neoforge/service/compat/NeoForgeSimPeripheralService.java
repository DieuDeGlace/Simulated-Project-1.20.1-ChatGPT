package dev.simulated_team.simulated.neoforge.service.compat;

import dan200.computercraft.api.ForgeComputerCraftAPI;
import dan200.computercraft.api.network.wired.WiredElement;
import dan200.computercraft.api.peripheral.IPeripheral;
import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.service.compat.SimPeripheralService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class NeoForgeSimPeripheralService implements SimPeripheralService {

    // CC:T 1.20.1 identifies wired elements through this Forge capability type.
    // This matches CC:T's own Forge Capabilities declaration without referencing
    // its non-API implementation package.
    private static final Capability<WiredElement> WIRED_ELEMENT = CapabilityManager.get(new CapabilityToken<>() {
    });

    private static final List<Entry<BlockEntity, IPeripheral>> PERIPHERALS = new ArrayList<>();
    private static final List<Entry<BlockEntity, WiredElement>> WIRED_ELEMENTS = new ArrayList<>();

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> void addPeripheral(final Supplier<BlockEntityType<T>> typeSupplier, final CapabilityGetter<T, IPeripheral> getter) {
        PERIPHERALS.add((Entry<BlockEntity, IPeripheral>) new Entry<>(typeSupplier, getter));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> void addWired(Supplier<BlockEntityType<T>> typeSupplier, CapabilityGetter<T, WiredElement> getter) {
        WIRED_ELEMENTS.add((Entry<BlockEntity, WiredElement>) new Entry<>(typeSupplier, getter));
    }

    public static void registerIntegration() {
        ForgeComputerCraftAPI.registerPeripheralProvider(NeoForgeSimPeripheralService::getPeripheral);
    }

    private static LazyOptional<IPeripheral> getPeripheral(final Level level, final BlockPos pos, final Direction direction) {
        final BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null) {
            return LazyOptional.empty();
        }

        for (final Entry<BlockEntity, IPeripheral> entry : PERIPHERALS) {
            if (entry.typeSupplier.get() == blockEntity.getType()) {
                final IPeripheral peripheral = entry.peripheralFunction().get(blockEntity, direction);
                return peripheral == null ? LazyOptional.empty() : LazyOptional.of(() -> peripheral);
            }
        }
        return LazyOptional.empty();
    }

    @SubscribeEvent
    public static void attachWiredElements(final AttachCapabilitiesEvent<BlockEntity> event) {
        final BlockEntity blockEntity = event.getObject();
        for (final Entry<BlockEntity, WiredElement> entry : WIRED_ELEMENTS) {
            if (entry.typeSupplier.get() == blockEntity.getType()) {
                final WiredElementProvider provider = new WiredElementProvider(blockEntity, entry);
                event.addCapability(Simulated.path("computercraft_wired_element"), provider);
                event.addListener(provider::invalidate);
                return;
            }
        }
    }

    private static final class WiredElementProvider implements ICapabilityProvider {
        private final BlockEntity blockEntity;
        private final Entry<BlockEntity, WiredElement> entry;
        private final java.util.Map<Direction, LazyOptional<WiredElement>> elements = new java.util.HashMap<>();

        private WiredElementProvider(final BlockEntity blockEntity, final Entry<BlockEntity, WiredElement> entry) {
            this.blockEntity = blockEntity;
            this.entry = entry;
        }

        @Override
        public <T> @NotNull LazyOptional<T> getCapability(final @NotNull Capability<T> capability,
                                                          final @Nullable Direction side) {
            if (capability != WIRED_ELEMENT) {
                return LazyOptional.empty();
            }
            return this.elements.computeIfAbsent(side, this::createElement).cast();
        }

        private LazyOptional<WiredElement> createElement(final Direction side) {
            final WiredElement element = this.entry.peripheralFunction().get(this.blockEntity, side);
            return element == null ? LazyOptional.empty() : LazyOptional.of(() -> element);
        }

        private void invalidate() {
            this.elements.values().forEach(LazyOptional::invalidate);
        }
    }

    private record Entry<T extends BlockEntity, V>(Supplier<BlockEntityType<T>> typeSupplier,
                                                   CapabilityGetter<T, V> peripheralFunction) {
    }
}

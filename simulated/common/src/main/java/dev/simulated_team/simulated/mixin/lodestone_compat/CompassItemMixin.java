package dev.simulated_team.simulated.mixin.lodestone_compat;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.simulated_team.simulated.content.navigation_targets.lodestone_compass_compatability.LodestoneTrackingMap;
import dev.simulated_team.simulated.index.SimDataComponents;
import dev.simulated_team.simulated.compat.components.ItemComponentCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.CompassItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(CompassItem.class)
public abstract class CompassItemMixin extends Item {
	public CompassItemMixin(final Properties properties) {
		super(properties);
	}

	@Inject(method = "inventoryTick", at = @At("HEAD"))
	private void simulated$checkID(final ItemStack stack, final Level level, final Entity entity, final int itemSlot, final boolean isSelected, final CallbackInfo ci) {
		if (!level.isClientSide) {
			if (ItemComponentCompat.has(stack, SimDataComponents.LODESTONE_COMPASS_SUBLEVEL_TRACKER)) {
				final UUID trackerID = ItemComponentCompat.get(stack, SimDataComponents.LODESTONE_COMPASS_SUBLEVEL_TRACKER);
				final LodestoneTrackingMap map = LodestoneTrackingMap.getOrLoad(level);
				if (map != null && entity instanceof final ServerPlayer sp) {
					map.sendUpdateForPlayer(trackerID, sp);
				}
			}
		}
	}

	@WrapOperation(method = "useOn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/CompassItem;addLodestoneTags(Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/core/BlockPos;Lnet/minecraft/nbt/CompoundTag;)V"))
	private void simulated$setLodestoneData(final CompassItem instance,
	                                        final ResourceKey<Level> dimension,
	                                        final BlockPos pos,
	                                        final CompoundTag itemTag,
	                                        final Operation<Void> original,
	                                        final UseOnContext context) {
		final LodestoneTrackingMap map = LodestoneTrackingMap.getOrLoad(context.getLevel());
		if (map != null) {
			final UUID uuid = map.addOrGetLodestoneTrackingPoint(pos);
			if (uuid != null) {
				ItemComponentCompat.set(itemTag, SimDataComponents.LODESTONE_COMPASS_SUBLEVEL_TRACKER, uuid);
			}
		}

		original.call(instance, dimension, pos, itemTag);
	}
}

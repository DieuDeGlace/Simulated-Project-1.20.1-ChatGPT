package dev.ryanhcode.offroad.events;

import com.simibubi.create.AllBlocks;
import dev.ryanhcode.offroad.content.blocks.wheel_mount.WheelMountBlockEntity;
import dev.ryanhcode.offroad.content.components.TireLike;
import dev.ryanhcode.offroad.handlers.client.MultiMiningClientHandler;
import dev.ryanhcode.offroad.handlers.server.MultiMiningServerManager;
import dev.ryanhcode.offroad.index.OffroadDataComponents;
import dev.simulated_team.simulated.compat.components.ItemComponentCompat;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;


public class OffroadCommonEvents {

    public static void registerDefaultComponents() {
        ItemComponentCompat.registerDefault(AllBlocks.FLYWHEEL, OffroadDataComponents.TIRE, TireLike.FLYWHEEL);
        ItemComponentCompat.registerDefault(AllBlocks.LARGE_WATER_WHEEL, OffroadDataComponents.TIRE, TireLike.LARGE_WATER_WHEEL);
        ItemComponentCompat.registerDefault(AllBlocks.CRUSHING_WHEEL, OffroadDataComponents.TIRE, TireLike.CRUSHING_WHEEL);
        ItemComponentCompat.registerDefault(AllBlocks.WATER_WHEEL, OffroadDataComponents.TIRE, TireLike.WATER_WHEEL);
        ItemComponentCompat.registerDefault(AllBlocks.MECHANICAL_ROLLER, OffroadDataComponents.TIRE, TireLike.MECHANICAL_ROLLER);
    }

    public static void physicsTick(final SubLevelPhysicsSystem physicsSystem, final double timeStep) {
        final ServerLevel level = physicsSystem.getLevel();
        WheelMountBlockEntity.applyAllBatchedForces(level, timeStep);
    }

    public static void tickLevelEvent(final Level level) {
        if (!level.isClientSide) {
            MultiMiningServerManager.tick(level);
        } else {
            MultiMiningClientHandler.tick(level);
        }
    }
}

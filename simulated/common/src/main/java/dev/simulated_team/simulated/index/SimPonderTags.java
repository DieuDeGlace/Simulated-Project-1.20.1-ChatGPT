package dev.simulated_team.simulated.index;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.infrastructure.ponder.AllCreatePonderTags;
import dev.simulated_team.simulated.Simulated;
import net.createmod.catnip.platform.CatnipServices;
import net.createmod.ponder.api.registration.PonderTagRegistrationHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;

public class SimPonderTags {

    public static final ResourceLocation
            NAVIGATION_ITEMS = Simulated.path("navigation_items"),
            PHYSICS_BEHAVIOR = Simulated.path("physics_behavior"),
            THRUST_PRODUCING_BLOCKS = Simulated.path("thrust_blocks"),
            PHYSICS_SENSORS = Simulated.path("physics_sensors");

    public static void register(final PonderTagRegistrationHelper<ResourceLocation> helper) {
        final PonderTagRegistrationHelper<ItemLike> itemHelper = helper.withKeyFunction(
                CatnipServices.REGISTRIES::getKeyOrThrow);

        // Sim Tags

        helper.registerTag(NAVIGATION_ITEMS)
                .addToIndex()
                .item(SimBlocks.NAVIGATION_TABLE.get())
                .title("Navigation Items")
                .description("Components which offer a destination to a Navigation Table")
                .register();

        itemHelper.addToTag(NAVIGATION_ITEMS)
                .add(SimBlocks.NAVIGATION_TABLE.get()) // why create... why
                .add(Items.COMPASS)
                .add(Blocks.LODESTONE)
                .add(Items.RECOVERY_COMPASS)
                .add(Items.FILLED_MAP)
                .add(SimBlocks.REDSTONE_MAGNET.get());

        helper.registerTag(PHYSICS_BEHAVIOR)
                .addToIndex()
                .item(SimBlocks.PHYSICS_ASSEMBLER.get())
                .title("Physics Behavior")
                .description("Components which have unique physics behavior or interactions")
                .register();

        itemHelper.addToTag(PHYSICS_BEHAVIOR)
                .add(SimBlocks.PHYSICS_ASSEMBLER.get()) // why create... why
                .add(SimBlocks.SWIVEL_BEARING.get())
                .add(AllBlocks.STICKER.get())
                .add(AllBlocks.WEIGHTED_EJECTOR.get())
                .add(SimBlocks.DOCKING_CONNECTOR.get())
                .add(SimBlocks.REDSTONE_MAGNET.get())
                .add(AllBlocks.SAIL.get())
                .add(SimBlocks.WHITE_SYMMETRIC_SAIL.get())
                .add(AllItems.BELT_CONNECTOR.get())
                .add(SimItems.SPRING.get())
                .add(SimItems.ROPE_COUPLING.get());

        helper.registerTag(THRUST_PRODUCING_BLOCKS)
                .addToIndex()
                .item(AllBlocks.ENCASED_FAN.get())
                .title("Thrust Producing Blocks")
                .description("Components which produce thrust on Simulated Contraptions")
                .register();

        itemHelper.addToTag(THRUST_PRODUCING_BLOCKS)
                .add(AllBlocks.ENCASED_FAN.get())
                .add(AllBlocks.NOZZLE.get());

        helper.registerTag(PHYSICS_SENSORS)
                .addToIndex()
                .item(SimBlocks.OPTICAL_SENSOR.get())
                .title("Physics Sensor Blocks")
                .description("Components which provide dynamic information about the world around them")
                .register();

        itemHelper.addToTag(PHYSICS_SENSORS)
                .add(SimBlocks.ALTITUDE_SENSOR.get())
                .add(SimBlocks.VELOCITY_SENSOR.get())
                .add(SimBlocks.GIMBAL_SENSOR.get())
                .add(SimBlocks.OPTICAL_SENSOR.get())
                .add(SimBlocks.NAVIGATION_TABLE.get())
                .add(SimBlocks.LASER_SENSOR.get());

        // Create Tags

        itemHelper.addToTag(AllCreatePonderTags.KINETIC_RELAYS)
                .add(SimBlocks.DIRECTIONAL_GEARSHIFT.get())
                .add(SimBlocks.TORSION_SPRING.get())
                .add(SimBlocks.ANALOG_TRANSMISSION.get());

        itemHelper.addToTag(AllCreatePonderTags.KINETIC_SOURCES)
                .add(SimBlocks.STEERING_WHEEL.get())
                .add(SimBlocks.RED_PORTABLE_ENGINE);

        itemHelper.addToTag(AllCreatePonderTags.KINETIC_APPLIANCES)
                .add(SimBlocks.SWIVEL_BEARING.get())
                .add(SimBlocks.ROPE_WINCH.get());

        itemHelper.addToTag(AllCreatePonderTags.FLUIDS)
                .add(SimBlocks.DOCKING_CONNECTOR.get());

        itemHelper.addToTag(AllCreatePonderTags.LOGISTICS)
                .add(SimBlocks.AUGER_SHAFT.get())
                .add(SimBlocks.AUGER_COG.get())
                .add(SimBlocks.DOCKING_CONNECTOR.get());

        itemHelper.addToTag(AllCreatePonderTags.REDSTONE)
                .add(SimBlocks.THROTTLE_LEVER.get())
                .add(SimBlocks.LINKED_TYPEWRITER.get())
                .add(SimBlocks.DIRECTIONAL_LINKED_RECEIVER.get())
                .add(SimBlocks.MODULATING_LINKED_RECEIVER.get())
                .add(SimBlocks.REDSTONE_ACCUMULATOR.get())
                .add(SimBlocks.REDSTONE_INDUCTOR.get());

        itemHelper.addToTag(AllCreatePonderTags.MOVEMENT_ANCHOR)
                .add(SimBlocks.PHYSICS_ASSEMBLER.get())
                .add(SimBlocks.SWIVEL_BEARING.get());

        itemHelper.addToTag(AllCreatePonderTags.SAILS)
                .add(SimBlocks.WHITE_SYMMETRIC_SAIL.get());

        itemHelper.addToTag(AllCreatePonderTags.ARM_TARGETS)
                .add(SimBlocks.RED_PORTABLE_ENGINE)
                .add(SimBlocks.NAVIGATION_TABLE.get());

        itemHelper.addToTag(AllCreatePonderTags.DISPLAY_SOURCES)
                .add(SimBlocks.AUGER_SHAFT.get())
                .add(SimBlocks.AUGER_COG.get())
                .add(SimBlocks.RED_PORTABLE_ENGINE)
                .add(SimBlocks.ALTITUDE_SENSOR.get())
                .add(SimBlocks.VELOCITY_SENSOR.get())
                .add(SimBlocks.GIMBAL_SENSOR.get())
                .add(SimBlocks.OPTICAL_SENSOR.get())
                .add(SimBlocks.NAVIGATION_TABLE.get())
                .add(SimBlocks.DOCKING_CONNECTOR.get())
                .add(SimBlocks.LINKED_TYPEWRITER.get());

        itemHelper.addToTag(AllCreatePonderTags.DISPLAY_TARGETS)
                .add(SimBlocks.NAMEPLATES.get(DyeColor.WHITE).get());

        itemHelper.addToTag(AllCreatePonderTags.THRESHOLD_SWITCH_TARGETS)
                .add(SimBlocks.ROPE_WINCH.get());
    }
}

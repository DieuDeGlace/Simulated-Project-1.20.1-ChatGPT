package dev.simulated_team.simulated.index;

import com.mojang.serialization.Codec;
import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.content.blocks.nav_table.navigation_target.NavigationTarget;
import dev.simulated_team.simulated.compat.components.ItemComponentType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;
import dev.simulated_team.simulated.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import java.util.UUID;

public class SimDataComponents {
    public static final ItemComponentType<BlockPos> ROPE_FIRST_CONNECTION = register("rope_first_connection", BlockPos.CODEC);

	public static final ItemComponentType<UUID> LODESTONE_COMPASS_SUBLEVEL_TRACKER = register("lodestone_compass_tracker", UUIDUtil.CODEC);

    public static final ItemComponentType<UUID> COMPASS_PLACER_UUID = register("compass_placer", UUIDUtil.STRING_CODEC);
    public static final ItemComponentType<GlobalPos> LAST_PLAYER_DEATH_LOCATION = register("last_player_death_location", GlobalPos.CODEC);

    public static final ItemComponentType<NavigationTarget> TARGET = register("target", SimRegistries.NAVIGATION_TARGET.byNameCodec());

    public static final ItemComponentType<Float> BOUNCINESS = register("bounciness", Codec.FLOAT);

    private static <T> ItemComponentType<T> register(final String name, final Codec<T> codec) {
        return new ItemComponentType<>(Simulated.path(name), codec);
    }

    public static void register() {
        // NBT-backed 1.20.1 component types require no registry entry.
    }

    public static void registerDefaults() {
        dev.simulated_team.simulated.registrate.SimulatedRegistrate.registerDefaultComponents();
    }
}

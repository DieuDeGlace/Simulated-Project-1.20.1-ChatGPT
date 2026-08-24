package dev.ryanhcode.offroad.index;

import dev.ryanhcode.offroad.Offroad;
import dev.ryanhcode.offroad.content.components.TireLike;
import dev.simulated_team.simulated.compat.components.ItemComponentType;

public class OffroadDataComponents {
	public static final ItemComponentType<TireLike> TIRE = create("tire", TireLike.CODEC);


	private static <T> ItemComponentType<T> create(final String name, final com.mojang.serialization.Codec<T> codec) {
		return new ItemComponentType<>(Offroad.path(name), codec);
	}

	public static void init() {
		// no-op
	}
}

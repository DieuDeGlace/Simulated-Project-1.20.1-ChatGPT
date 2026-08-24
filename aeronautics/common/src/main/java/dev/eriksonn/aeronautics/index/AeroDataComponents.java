package dev.eriksonn.aeronautics.index;

import dev.eriksonn.aeronautics.Aeronautics;
import dev.eriksonn.aeronautics.content.components.Converter;
import dev.eriksonn.aeronautics.content.components.Levitating;
import dev.simulated_team.simulated.compat.components.ItemComponentType;

public class AeroDataComponents {
	public static final ItemComponentType<Levitating> LEVITATING = create("levitating", Levitating.CODEC);

	public static final ItemComponentType<Converter> CONVERTER = create("converter", Converter.CODEC);

	private static <T> ItemComponentType<T> create(final String name, final com.mojang.serialization.Codec<T> codec) {
		return new ItemComponentType<>(Aeronautics.path(name), codec);
	}

	public static void init() {

	}

}

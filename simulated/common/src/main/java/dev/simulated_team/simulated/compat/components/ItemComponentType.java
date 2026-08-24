package dev.simulated_team.simulated.compat.components;

import com.mojang.serialization.Codec;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

/**
 * Minecraft 1.20.1 representation of a persistent item data component.
 */
public record ItemComponentType<T>(ResourceLocation id, Codec<T> codec) {
    public ItemComponentType {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(codec, "codec");
    }
}

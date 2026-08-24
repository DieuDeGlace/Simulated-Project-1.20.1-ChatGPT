package dev.simulated_team.simulated.compat.components;

import com.mojang.serialization.DataResult;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * NBT-backed compatibility for the persistent subset of Minecraft 1.21 item data
 * components used by the bundled projects.
 */
public final class ItemComponentCompat {
    private static final String VALUES_TAG = "SimulatedComponents";
    private static final String REMOVED_TAG = "SimulatedRemovedComponents";
    private static final List<DefaultValue<?>> DEFAULTS = new ArrayList<>();

    private ItemComponentCompat() {
    }

    public static <T> void registerDefault(final Supplier<? extends ItemLike> item,
                                           final ItemComponentType<T> type,
                                           final T value) {
        DEFAULTS.add(new DefaultValue<>(item, type, value));
    }

    public static <T> boolean has(final ItemStack stack, final ItemComponentType<T> type) {
        return get(stack, type) != null;
    }

    public static <T> @Nullable T get(final ItemStack stack, final ItemComponentType<T> type) {
        if (stack.isEmpty()) {
            return null;
        }

        final String key = type.id().toString();
        final CompoundTag root = stack.getTag();
        if (root != null) {
            final CompoundTag removed = root.getCompound(REMOVED_TAG);
            if (removed.getBoolean(key)) {
                return null;
            }

            final CompoundTag values = root.getCompound(VALUES_TAG);
            if (values.contains(key)) {
                return decode(type, values.get(key));
            }
        }

        return defaultValue(stack, type);
    }

    public static <T> void set(final ItemStack stack, final ItemComponentType<T> type, final T value) {
        set(stack.getOrCreateTag(), type, value);
    }

    public static <T> void set(final CompoundTag root, final ItemComponentType<T> type, final T value) {
        Objects.requireNonNull(value, "value");
        final DataResult<Tag> encoded = type.codec().encodeStart(NbtOps.INSTANCE, value);
        final Tag tag = encoded.result().orElseThrow(() -> new IllegalArgumentException(
                "Could not encode item component " + type.id() + ": "
                        + encoded.error().map(DataResult.PartialResult::message).orElse("unknown codec error")));

        child(root, REMOVED_TAG).remove(type.id().toString());
        child(root, VALUES_TAG).put(type.id().toString(), tag);
    }

    public static <T> void remove(final ItemStack stack, final ItemComponentType<T> type) {
        final CompoundTag root = stack.getOrCreateTag();
        final String key = type.id().toString();
        child(root, VALUES_TAG).remove(key);

        if (defaultValue(stack, type) != null) {
            child(root, REMOVED_TAG).putBoolean(key, true);
        } else {
            child(root, REMOVED_TAG).remove(key);
        }
    }

    private static CompoundTag child(final CompoundTag root, final String childName) {
        if (!root.contains(childName, Tag.TAG_COMPOUND)) {
            root.put(childName, new CompoundTag());
        }
        return root.getCompound(childName);
    }

    private static <T> T decode(final ItemComponentType<T> type, final Tag tag) {
        final DataResult<T> decoded = type.codec().parse(NbtOps.INSTANCE, tag);
        return decoded.result().orElseThrow(() -> new IllegalArgumentException(
                "Could not decode item component " + type.id() + ": "
                        + decoded.error().map(DataResult.PartialResult::message).orElse("unknown codec error")));
    }

    @SuppressWarnings("unchecked")
    private static <T> @Nullable T defaultValue(final ItemStack stack, final ItemComponentType<T> type) {
        for (final DefaultValue<?> candidate : DEFAULTS) {
            if (candidate.type().equals(type) && candidate.item().get().asItem() == stack.getItem()) {
                return (T) candidate.value();
            }
        }
        return null;
    }

    private record DefaultValue<T>(Supplier<? extends ItemLike> item,
                                   ItemComponentType<T> type,
                                   T value) {
    }
}

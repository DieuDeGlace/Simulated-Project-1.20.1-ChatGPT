package dev.simulated_team.simulated.network.codec;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.IntFunction;

public final class ByteBufCodecs {
    public static final StreamCodec<ByteBuf, Boolean> BOOL =
            StreamCodec.of((buffer, value) -> friendly(buffer).writeBoolean(value),
                    buffer -> friendly(buffer).readBoolean());
    public static final StreamCodec<ByteBuf, Integer> INT =
            StreamCodec.of((buffer, value) -> friendly(buffer).writeInt(value),
                    buffer -> friendly(buffer).readInt());
    public static final StreamCodec<ByteBuf, Float> FLOAT =
            StreamCodec.of((buffer, value) -> friendly(buffer).writeFloat(value),
                    buffer -> friendly(buffer).readFloat());
    public static final StreamCodec<ByteBuf, Double> DOUBLE =
            StreamCodec.of((buffer, value) -> friendly(buffer).writeDouble(value),
                    buffer -> friendly(buffer).readDouble());
    public static final StreamCodec<ByteBuf, String> STRING_UTF8 =
            StreamCodec.of((buffer, value) -> friendly(buffer).writeUtf(value),
                    buffer -> friendly(buffer).readUtf());
    public static final StreamCodec<ByteBuf, UUID> UUID =
            StreamCodec.of((buffer, value) -> friendly(buffer).writeUUID(value),
                    buffer -> friendly(buffer).readUUID());
    public static final StreamCodec<ByteBuf, ResourceLocation> RESOURCE_LOCATION =
            StreamCodec.of((buffer, value) -> friendly(buffer).writeResourceLocation(value),
                    buffer -> friendly(buffer).readResourceLocation());
    public static final StreamCodec<ByteBuf, Quaternionf> QUATERNIONF = StreamCodec.of((buffer, value) -> {
        FriendlyByteBuf buf = friendly(buffer);
        buf.writeFloat(value.x());
        buf.writeFloat(value.y());
        buf.writeFloat(value.z());
        buf.writeFloat(value.w());
    }, buffer -> {
        FriendlyByteBuf buf = friendly(buffer);
        return new Quaternionf(buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat());
    });

    private ByteBufCodecs() {
    }

    public static <B extends ByteBuf, E extends Enum<E>> StreamCodec<B, E> ofEnum(Class<E> enumClass) {
        E[] values = enumClass.getEnumConstants();
        return StreamCodec.of((buffer, value) -> friendly(buffer).writeVarInt(value.ordinal()), buffer -> {
            int ordinal = friendly(buffer).readVarInt();
            if (ordinal < 0 || ordinal >= values.length) {
                throw new IllegalArgumentException("Invalid " + enumClass.getSimpleName() + " ordinal: " + ordinal);
            }
            return values[ordinal];
        });
    }

    public static <B extends ByteBuf, V> Function<StreamCodec<B, V>, StreamCodec<B, List<V>>> list() {
        return codec -> collection(ArrayList::new, codec);
    }

    public static <B extends ByteBuf, V> Function<StreamCodec<B, V>, StreamCodec<B, List<V>>> list(int size) {
        return codec -> StreamCodec.of((buffer, values) -> {
            if (values.size() != size) {
                throw new IllegalArgumentException("Expected " + size + " elements, got " + values.size());
            }
            values.forEach(value -> codec.encode(buffer, value));
        }, buffer -> {
            List<V> values = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                values.add(codec.decode(buffer));
            }
            return values;
        });
    }

    public static <B extends ByteBuf, V, C extends Collection<V>> StreamCodec<B, C> collection(
            IntFunction<C> factory, StreamCodec<? super B, V> elementCodec) {
        return collection(factory, elementCodec, Integer.MAX_VALUE);
    }

    public static <B extends ByteBuf, V, C extends Collection<V>> StreamCodec<B, C> collection(
            IntFunction<C> factory, StreamCodec<? super B, V> elementCodec, int maxSize) {
        return StreamCodec.of((buffer, values) -> {
            if (values.size() > maxSize) {
                throw new IllegalArgumentException("Collection exceeds limit: " + values.size());
            }
            friendly(buffer).writeVarInt(values.size());
            values.forEach(value -> elementCodec.encode(buffer, value));
        }, buffer -> {
            int size = friendly(buffer).readVarInt();
            if (size < 0 || size > maxSize) {
                throw new IllegalArgumentException("Invalid collection size: " + size);
            }
            C values = factory.apply(size);
            for (int i = 0; i < size; i++) {
                values.add(elementCodec.decode(buffer));
            }
            return values;
        });
    }

    public static <B extends ByteBuf, K, V, M extends Map<K, V>> StreamCodec<B, M> map(
            IntFunction<M> factory, StreamCodec<? super B, K> keyCodec,
            StreamCodec<? super B, V> valueCodec) {
        return map(factory, keyCodec, valueCodec, Integer.MAX_VALUE);
    }

    public static <B extends ByteBuf, K, V, M extends Map<K, V>> StreamCodec<B, M> map(
            IntFunction<M> factory, StreamCodec<? super B, K> keyCodec,
            StreamCodec<? super B, V> valueCodec, int maxSize) {
        return StreamCodec.of((buffer, values) -> {
            if (values.size() > maxSize) {
                throw new IllegalArgumentException("Map exceeds limit: " + values.size());
            }
            friendly(buffer).writeVarInt(values.size());
            values.forEach((key, value) -> {
                keyCodec.encode(buffer, key);
                valueCodec.encode(buffer, value);
            });
        }, buffer -> {
            int size = friendly(buffer).readVarInt();
            if (size < 0 || size > maxSize) {
                throw new IllegalArgumentException("Invalid map size: " + size);
            }
            M values = factory.apply(size);
            for (int i = 0; i < size; i++) {
                values.put(keyCodec.decode(buffer), valueCodec.decode(buffer));
            }
            return values;
        });
    }

    public static <B extends ByteBuf, V> StreamCodec<B, Optional<V>> optional(
            StreamCodec<? super B, V> codec) {
        return StreamCodec.of((buffer, value) -> {
            friendly(buffer).writeBoolean(value.isPresent());
            value.ifPresent(element -> codec.encode(buffer, element));
        }, buffer -> friendly(buffer).readBoolean() ? Optional.of(codec.decode(buffer)) : Optional.empty());
    }

    public static <V> StreamCodec<ByteBuf, V> fromCodec(Codec<V> codec) {
        return StreamCodec.of((buffer, value) -> {
            Tag encoded = codec.encodeStart(NbtOps.INSTANCE, value)
                    .getOrThrow(false, IllegalStateException::new);
            CompoundTag envelope = new CompoundTag();
            envelope.put("value", encoded);
            friendly(buffer).writeNbt(envelope);
        }, buffer -> {
            CompoundTag envelope = friendly(buffer).readNbt(NbtAccounter.UNLIMITED);
            if (envelope == null || !envelope.contains("value")) {
                throw new IllegalArgumentException("Missing codec payload");
            }
            return codec.parse(NbtOps.INSTANCE, envelope.get("value"))
                    .getOrThrow(false, IllegalStateException::new);
        });
    }

    private static FriendlyByteBuf friendly(ByteBuf buffer) {
        return buffer instanceof FriendlyByteBuf friendly ? friendly : new FriendlyByteBuf(buffer);
    }
}

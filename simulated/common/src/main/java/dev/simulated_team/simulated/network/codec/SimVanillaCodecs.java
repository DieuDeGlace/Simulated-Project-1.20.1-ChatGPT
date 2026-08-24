package dev.simulated_team.simulated.network.codec;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public final class SimVanillaCodecs {
    public static final StreamCodec<ByteBuf, BlockPos> BLOCK_POS = StreamCodec.of(
            (buffer, value) -> buffer.writeLong(value.asLong()),
            buffer -> BlockPos.of(buffer.readLong()));
    public static final StreamCodec<ByteBuf, java.util.UUID> UUID = ByteBufCodecs.UUID;
    public static final StreamCodec<ByteBuf, Direction> DIRECTION = StreamCodec.of(
            (buffer, value) -> friendly(buffer).writeVarInt(value.get3DDataValue()),
            buffer -> Direction.from3DDataValue(friendly(buffer).readVarInt()));
    public static final StreamCodec<ByteBuf, ResourceLocation> RESOURCE_LOCATION =
            ByteBufCodecs.RESOURCE_LOCATION;
    public static final StreamCodec<FriendlyByteBuf, ItemStack> ITEM_STACK =
            StreamCodec.of(FriendlyByteBuf::writeItem, FriendlyByteBuf::readItem);
    public static final StreamCodec<ByteBuf, Vec3> VEC3 = StreamCodec.of((buffer, value) -> {
        buffer.writeDouble(value.x);
        buffer.writeDouble(value.y);
        buffer.writeDouble(value.z);
    }, buffer -> new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()));
    public static final StreamCodec<ByteBuf, InteractionHand> HAND = ByteBufCodecs.ofEnum(InteractionHand.class);

    private SimVanillaCodecs() {
    }

    public static <T> StreamCodec<ByteBuf, ResourceKey<T>> resourceKey(
            ResourceKey<? extends Registry<T>> registry) {
        return RESOURCE_LOCATION.map(location -> ResourceKey.create(registry, location), ResourceKey::location);
    }

    private static FriendlyByteBuf friendly(ByteBuf buffer) {
        return buffer instanceof FriendlyByteBuf friendly ? friendly : new FriendlyByteBuf(buffer);
    }
}

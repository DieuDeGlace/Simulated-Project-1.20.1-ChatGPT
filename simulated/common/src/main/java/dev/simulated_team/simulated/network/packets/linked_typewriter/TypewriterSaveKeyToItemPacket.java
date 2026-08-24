package dev.simulated_team.simulated.network.packets.linked_typewriter;

import com.mojang.serialization.DataResult;
import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.LinkedTypewriterEntries;
import dev.simulated_team.simulated.index.SimBlocks;
import dev.simulated_team.simulated.network.context.ServerPacketContext;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import dev.simulated_team.simulated.network.codec.ByteBufCodecs;
import dev.simulated_team.simulated.network.codec.StreamCodec;
import dev.simulated_team.simulated.network.packet.SimPacket;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import dev.simulated_team.simulated.index.SimBlockEntityTypes;
import java.util.Optional;

public record TypewriterSaveKeyToItemPacket(InteractionHand hand, LinkedTypewriterEntries.KeyboardEntry entry) implements SimPacket {

    public static Type<TypewriterSaveKeyToItemPacket> TYPE = new Type<>(Simulated.path("linked_typewriter_bind_item"), TypewriterSaveKeyToItemPacket.class);

    public static StreamCodec<FriendlyByteBuf, TypewriterSaveKeyToItemPacket> CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, (packet) -> packet.hand.ordinal(),
            LinkedTypewriterEntries.KeyboardEntry.STREAM_CODEC, TypewriterSaveKeyToItemPacket::entry,
            (h, e) -> new TypewriterSaveKeyToItemPacket(InteractionHand.values()[h], e));

    public void handle(final ServerPacketContext context) {
        final ServerPlayer player = context.player();
        final ItemStack item = player.getItemInHand(this.hand);

        final CompoundTag existingTag = BlockItem.getBlockEntityData(item);
        CompoundTag currentTag = existingTag == null ? new CompoundTag() : existingTag.copy();
        if (existingTag == null) {
            currentTag.putString("id", item.getItem().toString());
        }

        final RegistryOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, context.level().registryAccess());
        final DataResult<Tag> result = LinkedTypewriterEntries.KeyboardEntry.CODEC.encodeStart(ops, this.entry);
        final Optional<Tag> encodedEntry = result.resultOrPartial(message ->
                Simulated.LOGGER.warn("Unable to process entry for item saving: {}", message));
        if (encodedEntry.isEmpty()) {
            return;
        }

        final CompoundTag entryTag = (CompoundTag) encodedEntry.get();
        if (!currentTag.contains("Keys")) {
            currentTag.put("Keys", new ListTag());
        }

        final ListTag keys = currentTag.getList("Keys", Tag.TAG_COMPOUND);
        boolean alreadyPresent = false;

        for (int i = 0; i < keys.size(); i++) {
            final Tag key = keys.get(i);
            final int glfwKey = ((CompoundTag) key).getInt("GLFWKey");

            if (glfwKey == this.entry.glfwKeyCode) {
                alreadyPresent = true;
                keys.set(i, entryTag);
                break;
            }
        }

        if (!alreadyPresent) {
            keys.add(entryTag);
        }

        currentTag.put("Keys", keys);
        if (item.is(SimBlocks.LINKED_TYPEWRITER.get().asItem())) {
            BlockItem.setBlockEntityData(item, SimBlockEntityTypes.LINKED_TYPEWRITER.get(), currentTag);
        }
    }

    @Override
    public Type<? extends SimPacket> type() {
        return TYPE;
    }
}

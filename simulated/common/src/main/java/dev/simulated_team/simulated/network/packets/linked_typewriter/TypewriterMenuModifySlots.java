package dev.simulated_team.simulated.network.packets.linked_typewriter;

import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.screen.LinkedTypewriterMenuCommon;
import dev.simulated_team.simulated.network.context.ServerPacketContext;
import net.minecraft.network.FriendlyByteBuf;
import dev.simulated_team.simulated.network.codec.StreamCodec;
import dev.simulated_team.simulated.network.packet.SimPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public record TypewriterMenuModifySlots(ItemStack first, ItemStack second) implements SimPacket {

    public static Type<TypewriterMenuModifySlots> TYPE = new Type<>(Simulated.path("entry_modify"), TypewriterMenuModifySlots.class);

    public static StreamCodec<FriendlyByteBuf, TypewriterMenuModifySlots> CODEC = StreamCodec.composite(
            dev.simulated_team.simulated.network.codec.SimVanillaCodecs.ITEM_STACK, TypewriterMenuModifySlots::first,
            dev.simulated_team.simulated.network.codec.SimVanillaCodecs.ITEM_STACK, TypewriterMenuModifySlots::second,
            TypewriterMenuModifySlots::new
    );

    public void handle(final ServerPacketContext context) {
        final ServerPlayer player = context.player();

        if (player.containerMenu instanceof final LinkedTypewriterMenuCommon menu) {
            menu.ghostInventory.setStackInSlot(0, this.first);
            menu.ghostInventory.setStackInSlot(1, this.second);
        }
    }

    @Override
    public Type<? extends SimPacket> type() {
        return TYPE;
    }
}

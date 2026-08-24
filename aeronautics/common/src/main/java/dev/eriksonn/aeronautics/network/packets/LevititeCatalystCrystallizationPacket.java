package dev.eriksonn.aeronautics.network.packets;

import dev.eriksonn.aeronautics.Aeronautics;
import dev.eriksonn.aeronautics.api.levitite_blend_crystallization.CrystalPropagationContext;
import dev.eriksonn.aeronautics.api.levitite_blend_crystallization.LevititeBlendHelper;
import dev.eriksonn.aeronautics.index.AeroLevititeBlendPropagationContexts;
import dev.eriksonn.aeronautics.index.AeroTags;
import dev.eriksonn.aeronautics.util.CatalyzerHelper;
import dev.simulated_team.simulated.network.context.ServerPacketContext;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import dev.simulated_team.simulated.network.codec.StreamCodec;
import dev.simulated_team.simulated.network.packet.SimPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

public record LevititeCatalystCrystallizationPacket(BlockPos pos, InteractionHand hand) implements SimPacket {
	public static final Type<LevititeCatalystCrystallizationPacket> TYPE = new Type<>(Aeronautics.path("levitite_blend_crystallize"), LevititeCatalystCrystallizationPacket.class);

	public static final StreamCodec<FriendlyByteBuf, LevititeCatalystCrystallizationPacket> STREAM_CODEC = StreamCodec.composite(
			dev.simulated_team.simulated.network.codec.SimVanillaCodecs.BLOCK_POS, LevititeCatalystCrystallizationPacket::pos,
			dev.simulated_team.simulated.network.codec.SimVanillaCodecs.HAND, LevititeCatalystCrystallizationPacket::hand,
			LevititeCatalystCrystallizationPacket::new);

	@Override
	public Type<? extends SimPacket> type() {
		return TYPE;
	}

	public void handle(final ServerPacketContext context) {
		final ServerPlayer player = context.player();

		final ItemStack item = player.getItemInHand(this.hand);

		if (!CatalyzerHelper.isCatalyzer(item)) {
			return;
		}

		if (!item.is(AeroTags.ItemTags.LEVITITE_CATALYZER_NO_CONSUME)) {
			if (item.isDamageableItem()) {
				item.hurtAndBreak(1, player, this.hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
			} else if (item.isStackable() && !context.player().hasInfiniteMaterials()) {
				item.shrink(1);
			}
		}
		player.swing(this.hand);
		final CrystalPropagationContext itemContext = item.is(AeroTags.ItemTags.LEVITITE_SOUL_CATALYZER) ?
				AeroLevititeBlendPropagationContexts.SOUL_CONTEXT.get() :
				AeroLevititeBlendPropagationContexts.STANDARD_CONTEXT.get();
		LevititeBlendHelper.addLevititeBlendTicker(context.level(), this.pos, false, false,
				itemContext.getContextForSpread(context.level(), this.pos)
		);
	}
}

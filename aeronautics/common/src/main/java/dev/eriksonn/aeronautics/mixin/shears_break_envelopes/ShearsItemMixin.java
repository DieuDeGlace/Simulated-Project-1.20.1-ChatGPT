package dev.eriksonn.aeronautics.mixin.shears_break_envelopes;

import dev.eriksonn.aeronautics.index.AeroTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ShearsItem.class)
public class ShearsItemMixin {
	@Inject(method = "getDestroySpeed", at = @At("HEAD"), cancellable = true)
	private void aeronautics$envelopeDestroySpeed(final ItemStack stack, final BlockState state,
			final CallbackInfoReturnable<Float> cir) {
		if (state.is(AeroTags.BlockTags.ENVELOPE)) {
			cir.setReturnValue(5.0f);
		}
	}
}

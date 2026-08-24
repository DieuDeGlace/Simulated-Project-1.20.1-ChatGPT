package dev.simulated_team.simulated.neoforge.mixin.merging_glue;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.simulated_team.simulated.SimulatedClient;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(MultiPlayerGameMode.class)
public class MergingGlueUseMixin {
    @WrapOperation(
            method = "performUseItemOn",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;useOn(Lnet/minecraft/world/item/context/UseOnContext;)Lnet/minecraft/world/InteractionResult;")
    )
    private InteractionResult simulated$useMergingGlueAfterBlock(final ItemStack stack,
                                                                  final UseOnContext context,
                                                                  final Operation<InteractionResult> original) {
        final Player player = context.getPlayer();
        if (player != null && SimulatedClient.MERGING_GLUE_ITEM_HANDLER.onItemUseBlock(
                context.getLevel(), player, stack, context.getHand())) {
            return InteractionResult.CONSUME;
        }
        return original.call(stack, context);
    }
}

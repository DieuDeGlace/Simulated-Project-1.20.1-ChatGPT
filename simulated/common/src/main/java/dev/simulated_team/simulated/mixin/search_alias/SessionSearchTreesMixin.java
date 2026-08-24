package dev.simulated_team.simulated.mixin.search_alias;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.simulated_team.simulated.client.SearchAlias;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(Minecraft.class)
public class SessionSearchTreesMixin {

    @WrapOperation(method = "createSearchTrees", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getTooltipLines(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/TooltipFlag;)Ljava/util/List;"))
    private static List<Component> simulated$getTooltipLines(final ItemStack instance, final Player player, final TooltipFlag flag, final Operation<List<Component>> original) {
        final List<Component> tooltipLines = original.call(instance, player, flag);
        tooltipLines.addAll(SearchAlias.getAliases(instance).stream().map(Component::literal).toList());
        return tooltipLines;
    }

}

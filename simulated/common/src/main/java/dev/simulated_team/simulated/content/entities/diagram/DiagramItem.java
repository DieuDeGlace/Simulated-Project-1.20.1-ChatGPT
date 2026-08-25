package dev.simulated_team.simulated.content.entities.diagram;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.simulated_team.simulated.Simulated;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class DiagramItem extends Item {

    public DiagramItem(final Properties p_i48487_1_) {
        super(p_i48487_1_);
    }

    @Override
    public InteractionResult useOn(final UseOnContext ctx) {
        final Direction face = ctx.getClickedFace();
        final Player player = ctx.getPlayer();
        final ItemStack stack = ctx.getItemInHand();
        final BlockPos pos = ctx.getClickedPos()
                .relative(face);

        if (player != null && !player.mayUseItemAt(pos, face, stack)) {
            return InteractionResult.FAIL;
        }

        final Level world = ctx.getLevel();
        final DiagramEntity diagram = new DiagramEntity(world, pos, face, face.getAxis()
                .isHorizontal() ? Direction.DOWN : ctx.getHorizontalDirection());

        final boolean survives = diagram.survives();
        final SubLevel subLevel = Sable.HELPER.getContaining(world, pos);
        Simulated.LOGGER.info("SIMULATED-DIAGRAM-PLACE: side={}, dimension={}, pos={}, face={}, entityPos={}, subLevel={}, survives={}",
                world.isClientSide ? "client" : "server", world.dimension().location(), pos, face,
                diagram.position(), subLevel == null ? null : subLevel.getUniqueId(), survives);

        if (!survives) {
            return InteractionResult.CONSUME;
        }

        if (!world.isClientSide) {
            diagram.playPlacementSound();
            world.addFreshEntity(diagram);
        }

        stack.shrink(1);
        return InteractionResult.sidedSuccess(world.isClientSide);
    }

}

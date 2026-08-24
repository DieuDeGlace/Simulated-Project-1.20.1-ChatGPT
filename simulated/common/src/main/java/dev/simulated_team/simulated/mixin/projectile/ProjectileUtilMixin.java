package dev.simulated_team.simulated.mixin.projectile;

import dev.simulated_team.simulated.content.entities.launched_plunger.LaunchedPlungerEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Predicate;

@Mixin(ProjectileUtil.class)
public abstract class ProjectileUtilMixin {
    @Inject(method = "getHitResultOnMoveVector", at = @At("RETURN"), cancellable = true)
    private static void simulated$deflectProjectileFromPlunger(final Entity movingEntity,
                                                                final Predicate<Entity> canHit,
                                                                final CallbackInfoReturnable<HitResult> cir) {
        if (!(movingEntity instanceof final Projectile projectile)
                || !(cir.getReturnValue() instanceof final EntityHitResult entityHit)
                || !(entityHit.getEntity() instanceof final LaunchedPlungerEntity plunger)) {
            return;
        }

        plunger.applyProjectileDeflection(projectile);

        final Vec3 movement = projectile.getDeltaMovement();
        final Direction direction = Direction.getNearest(movement.x, movement.y, movement.z);
        cir.setReturnValue(BlockHitResult.miss(entityHit.getLocation(), direction, BlockPos.containing(entityHit.getLocation())));
    }
}

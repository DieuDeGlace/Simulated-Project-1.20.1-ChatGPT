package dev.simulated_team.simulated.mixin.arm_interactions;

import com.simibubi.create.api.registry.CreateBuiltInRegistries;
import dev.simulated_team.simulated.index.SimArmInteractions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Registers Simulated's arm interaction types during Create's built-in registry
 * bootstrap, before Minecraft freezes that registry on 1.20.1.
 */
@Mixin(CreateBuiltInRegistries.class)
public abstract class CreateBuiltInRegistriesMixin {
    @Inject(method = "init", at = @At("TAIL"), remap = false)
    private static void simulated$registerArmInteractions(final CallbackInfo ci) {
        SimArmInteractions.init();
    }
}

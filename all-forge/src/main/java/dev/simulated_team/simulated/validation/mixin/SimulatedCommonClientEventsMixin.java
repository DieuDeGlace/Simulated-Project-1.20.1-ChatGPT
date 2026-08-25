package dev.simulated_team.simulated.validation.mixin;

import dev.simulated_team.simulated.events.SimulatedCommonClientEvents;
import dev.simulated_team.simulated.validation.SubLevelRenderClientValidation;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SimulatedCommonClientEvents.class, remap = false)
public abstract class SimulatedCommonClientEventsMixin {
    @Inject(method = "renderOverlays", at = @At("HEAD"))
    private static void simulated$recordOverlayRender(final GuiGraphics graphics, final float partialTick,
                                                       final CallbackInfo ci) {
        SubLevelRenderClientValidation.recordOverlayRender();
    }
}

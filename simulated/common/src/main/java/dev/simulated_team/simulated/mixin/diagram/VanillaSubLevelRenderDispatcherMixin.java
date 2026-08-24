package dev.simulated_team.simulated.mixin.diagram;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.ryanhcode.sable.sublevel.render.dispatcher.VanillaSubLevelRenderDispatcher;
import dev.simulated_team.simulated.util.SimpleSubLevelGroupRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(VanillaSubLevelRenderDispatcher.class)
public class VanillaSubLevelRenderDispatcherMixin {

    @WrapOperation(method = "renderSectionLayer", at = @At(value = "INVOKE", target = "Ldev/ryanhcode/sable/sublevel/render/dispatcher/VanillaSubLevelRenderDispatcher;setupDynamicEffects(Lnet/minecraft/client/renderer/ShaderInstance;ZZLorg/joml/Matrix4fc;)V", ordinal = 0), remap = false)
    private void simulated$overrideNormalLighting(final ShaderInstance shader, final boolean onSubLevel,
                                                   final boolean upload, final Matrix4fc baseModelView,
                                                   final Operation<Void> original) {
        if (SimpleSubLevelGroupRenderer.RENDERING_SIMPLE) {
            original.call(shader, false, upload, baseModelView);
            return;
        }
        original.call(shader, onSubLevel, upload, baseModelView);
    }

}

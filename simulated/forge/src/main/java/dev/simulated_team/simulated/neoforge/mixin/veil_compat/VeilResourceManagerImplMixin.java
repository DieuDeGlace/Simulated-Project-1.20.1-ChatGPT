package dev.simulated_team.simulated.neoforge.mixin.veil_compat;

import dev.simulated_team.simulated.Simulated;
import foundry.veil.ext.PackResourcesExtension;
import foundry.veil.impl.resource.VeilResourceManagerImpl;
import net.minecraft.server.packs.PackType;
import net.minecraftforge.resource.PathPackResources;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.concurrent.atomic.AtomicInteger;

@Mixin(value = VeilResourceManagerImpl.class, remap = false)
public abstract class VeilResourceManagerImplMixin {

    private static final String FORGE_JARJAR_FILE_SYSTEM = "net.minecraftforge.jarjar.nio.pathfs.PathFileSystem";

    @Redirect(
            method = "loadPack",
            at = @At(
                    value = "INVOKE",
                    target = "Lfoundry/veil/ext/PackResourcesExtension;veil$listResources(Lfoundry/veil/ext/PackResourcesExtension$PackResourceConsumer;)V",
                    remap = false
            ),
            remap = false
    )
    private void simulated$listJarJarResources(final PackResourcesExtension extension,
                                               final PackResourcesExtension.PackResourceConsumer consumer) {
        if (!(extension instanceof final PathPackResources packResources)
                || !FORGE_JARJAR_FILE_SYSTEM.equals(packResources.getSource().getFileSystem().getClass().getName())) {
            extension.veil$listResources(consumer);
            return;
        }

        final AtomicInteger resources = new AtomicInteger();
        for (final PackType type : PackType.values()) {
            for (final String namespace : packResources.getNamespaces(type)) {
                packResources.listResources(type, namespace, "", (location, supplier) -> {
                    consumer.accept(type, location, null, null, null);
                    resources.incrementAndGet();
                });
            }
        }

        Simulated.LOGGER.info("[SIMULATED-VEIL-JARJAR] Loaded {} static resources through the standard pack API", resources.get());
    }
}

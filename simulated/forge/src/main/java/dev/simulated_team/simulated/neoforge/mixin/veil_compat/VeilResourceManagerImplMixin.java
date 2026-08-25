package dev.simulated_team.simulated.neoforge.mixin.veil_compat;

import dev.simulated_team.simulated.Simulated;
import foundry.veil.ext.PackResourcesExtension;
import foundry.veil.impl.resource.VeilResourceManagerImpl;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraftforge.resource.PathPackResources;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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

        int resources = 0;
        int invalidResources = 0;
        try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(packResources.getSource()))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }

                final String entryName = entry.getName();
                final PackType type;
                final String root;
                if (entryName.startsWith("assets/")) {
                    type = PackType.CLIENT_RESOURCES;
                    root = "assets/";
                } else if (entryName.startsWith("data/")) {
                    type = PackType.SERVER_DATA;
                    root = "data/";
                } else {
                    continue;
                }

                final int namespaceEnd = entryName.indexOf('/', root.length());
                if (namespaceEnd < 0 || namespaceEnd == root.length() || namespaceEnd + 1 >= entryName.length()) {
                    invalidResources++;
                    dev.ryanhcode.sable.util.DiagnosticLogLimiter.warn(Simulated.LOGGER, "[SIMULATED-VEIL-JARJAR] Ignoring malformed resource entry {}", entryName);
                    continue;
                }

                final String namespace = entryName.substring(root.length(), namespaceEnd);
                final String path = entryName.substring(namespaceEnd + 1);
                final ResourceLocation location = ResourceLocation.tryBuild(namespace, path);
                if (location == null) {
                    invalidResources++;
                    dev.ryanhcode.sable.util.DiagnosticLogLimiter.warn(Simulated.LOGGER, "[SIMULATED-VEIL-JARJAR] Ignoring invalid resource entry {}", entryName);
                    continue;
                }

                consumer.accept(type, location, null, null, null);
                resources++;
            }
        } catch (final IOException exception) {
            throw new UncheckedIOException("Failed to enumerate a Forge JarJar resource pack", exception);
        }

        dev.ryanhcode.sable.util.DiagnosticLogLimiter.info(Simulated.LOGGER, "[SIMULATED-VEIL-JARJAR] Loaded {} static resources from the nested archive ({} invalid)", resources, invalidResources);
    }
}

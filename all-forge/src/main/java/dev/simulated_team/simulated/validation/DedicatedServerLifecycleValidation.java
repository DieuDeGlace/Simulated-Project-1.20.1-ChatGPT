package dev.simulated_team.simulated.validation;

import com.mojang.logging.LogUtils;
import dev.simulated_team.simulated.Simulated;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

/** Development-only dedicated-server startup, save, and shutdown gate. */
@Mod.EventBusSubscriber(modid = Simulated.MOD_ID)
public final class DedicatedServerLifecycleValidation {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final boolean ENABLED = Boolean.getBoolean("simulated.validation.dedicatedServer");
    private static MinecraftServer server;
    private static int stableTicks;

    private DedicatedServerLifecycleValidation() {
    }

    @SubscribeEvent
    public static void onServerStarted(final ServerStartedEvent event) {
        if (!ENABLED || !event.getServer().isDedicatedServer()) {
            return;
        }
        server = event.getServer();
        stableTicks = 0;
        LOGGER.info("SIMULATED-DEDICATED-LIFECYCLE-STARTED: levels={}, players={}",
                server.getAllLevels().spliterator().getExactSizeIfKnown(), server.getPlayerCount());
    }

    @SubscribeEvent
    public static void onServerTick(final TickEvent.ServerTickEvent event) {
        if (!ENABLED || event.phase != TickEvent.Phase.END || server == null) {
            return;
        }
        if (++stableTicks < 40) {
            return;
        }
        final MinecraftServer completedServer = server;
        server = null;
        if (!completedServer.saveEverything(false, true, true)) {
            throw new IllegalStateException("Dedicated lifecycle validation saveEverything returned false");
        }
        LOGGER.info("SIMULATED-DEDICATED-LIFECYCLE-PASS: stableTicks={}, levels={}, players={}",
                stableTicks, completedServer.getAllLevels().spliterator().getExactSizeIfKnown(),
                completedServer.getPlayerCount());
        completedServer.halt(false);
    }
}

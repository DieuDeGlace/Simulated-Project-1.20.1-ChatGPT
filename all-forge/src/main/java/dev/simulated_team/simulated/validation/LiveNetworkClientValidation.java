package dev.simulated_team.simulated.validation;

import com.mojang.logging.LogUtils;
import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.content.blocks.altitude_sensor.AltitudeSensorBlockEntity;
import dev.simulated_team.simulated.index.SimBlocks;
import dev.simulated_team.simulated.network.SimNetwork;
import dev.simulated_team.simulated.network.packets.ConfigureAltitudeSensorPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.util.UUID;

/** Development-only live-channel validation loaded from the aggregate run project. */
@Mod.EventBusSubscriber(modid = Simulated.MOD_ID, value = Dist.CLIENT)
public final class LiveNetworkClientValidation {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final boolean ENABLED = Boolean.getBoolean("simulated.validation.liveNetwork");
    private static final float EXPECTED_HIGH = 0.73f;
    private static final float EXPECTED_LOW = 0.27f;
    private static final int TIMEOUT_TICKS = 400;

    private static Phase phase = Phase.WAITING_FOR_WORLD;
    private static int phaseTicks;
    private static int exitTicks;
    private static volatile BlockPos targetPos;
    private static volatile ResourceKey<Level> targetDimension;
    private static volatile boolean setupReady;
    private static volatile boolean serverCheckScheduled;
    private static volatile boolean handlerApplied;

    private LiveNetworkClientValidation() {
    }

    @SubscribeEvent
    public static void onClientTick(final TickEvent.ClientTickEvent event) {
        if (!ENABLED || event.phase != TickEvent.Phase.END || phase == Phase.DONE) {
            return;
        }

        final Minecraft minecraft = Minecraft.getInstance();
        final MinecraftServer server = minecraft.getSingleplayerServer();
        if (minecraft.level == null || minecraft.player == null || server == null) {
            if (phase == Phase.WAITING_FOR_WORLD) {
                return;
            }
            fail("integrated server or local player disappeared during validation");
        }

        if (phase == Phase.WAITING_FOR_WORLD) {
            final UUID playerId = minecraft.player.getUUID();
            phase = Phase.WAITING_FOR_CLIENT_SYNC;
            phaseTicks = 0;
            server.execute(() -> createFixture(server, playerId));
            return;
        }

        if (++phaseTicks > TIMEOUT_TICKS) {
            fail("timed out in phase " + phase + " at " + targetPos);
        }

        if (phase == Phase.WAITING_FOR_CLIENT_SYNC) {
            final BlockPos pos = targetPos;
            if (setupReady && pos != null
                    && minecraft.level.getBlockEntity(pos) instanceof AltitudeSensorBlockEntity) {
                SimNetwork.server().sendPacket(new ConfigureAltitudeSensorPacket(pos, EXPECTED_HIGH, EXPECTED_LOW));
                LOGGER.info("SIMULATED-LIVE-NETWORK-SENT: packet=configure_altitude_sensor, pos={}", pos);
                phase = Phase.WAITING_FOR_SERVER_HANDLER;
                phaseTicks = 0;
            }
            return;
        }

        if (phase == Phase.WAITING_FOR_SERVER_HANDLER) {
            scheduleServerCheck(server);
            if (handlerApplied) {
                LOGGER.info("SIMULATED-LIVE-NETWORK-PASS: client encode/send -> Forge channel -> server decode/handler, pos={}, high={}, low={}",
                        targetPos, EXPECTED_HIGH, EXPECTED_LOW);
                phase = Phase.EXITING;
                phaseTicks = 0;
            }
            return;
        }

        if (phase == Phase.EXITING && ++exitTicks >= 20) {
            phase = Phase.DONE;
            minecraft.stop();
        }
    }

    private static void createFixture(final MinecraftServer server, final UUID playerId) {
        final ServerPlayer player = server.getPlayerList().getPlayer(playerId);
        if (player == null) {
            fail("local server player was not available for fixture creation");
        }

        final ServerLevel level = player.serverLevel();
        final BlockPos pos = player.blockPosition().offset(2, 0, 0);
        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(pos, SimBlocks.ALTITUDE_SENSOR.getDefaultState());
        if (!(level.getBlockEntity(pos) instanceof AltitudeSensorBlockEntity)) {
            fail("server fixture did not create an altitude sensor at " + pos);
        }

        targetPos = pos;
        targetDimension = level.dimension();
        setupReady = true;
        LOGGER.info("SIMULATED-LIVE-NETWORK-FIXTURE: pos={}, player={}", pos, player.getGameProfile().getName());
    }

    private static void scheduleServerCheck(final MinecraftServer server) {
        if (serverCheckScheduled) {
            return;
        }
        serverCheckScheduled = true;
        server.execute(() -> {
            try {
                final ServerLevel level = server.getLevel(targetDimension);
                if (level != null
                        && level.getBlockEntity(targetPos) instanceof final AltitudeSensorBlockEntity sensor
                        && Math.abs(sensor.highSignal - EXPECTED_HIGH) <= 1.0e-6f
                        && Math.abs(sensor.lowSignal - EXPECTED_LOW) <= 1.0e-6f) {
                    handlerApplied = true;
                }
            } finally {
                serverCheckScheduled = false;
            }
        });
    }

    private static void fail(final String message) {
        LOGGER.error("SIMULATED-LIVE-NETWORK-FAIL: {}", message);
        throw new IllegalStateException("Simulated live network validation failed: " + message);
    }

    private enum Phase {
        WAITING_FOR_WORLD,
        WAITING_FOR_CLIENT_SYNC,
        WAITING_FOR_SERVER_HANDLER,
        EXITING,
        DONE
    }
}

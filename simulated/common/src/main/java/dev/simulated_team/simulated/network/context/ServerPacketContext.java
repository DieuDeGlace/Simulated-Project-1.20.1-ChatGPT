package dev.simulated_team.simulated.network.context;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public record ServerPacketContext(ServerPlayer player) implements PacketContext {
    @Override
    public Level level() {
        return this.player.serverLevel();
    }

    public MinecraftServer server() {
        return this.player.server;
    }

    @Override
    public void disconnect(Component reason) {
        this.player.connection.disconnect(reason);
    }
}

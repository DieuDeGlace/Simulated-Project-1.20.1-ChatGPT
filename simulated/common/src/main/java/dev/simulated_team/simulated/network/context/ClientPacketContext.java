package dev.simulated_team.simulated.network.context;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

public final class ClientPacketContext implements PacketContext {
    public static final ClientPacketContext INSTANCE = new ClientPacketContext();

    private ClientPacketContext() {
    }

    public Minecraft client() {
        return Minecraft.getInstance();
    }

    @Override
    public LocalPlayer player() {
        return this.client().player;
    }

    @Override
    public Level level() {
        return this.client().level;
    }

    @Override
    public void disconnect(Component reason) {
        if (this.client().getConnection() != null) {
            this.client().getConnection().getConnection().disconnect(reason);
        }
    }
}

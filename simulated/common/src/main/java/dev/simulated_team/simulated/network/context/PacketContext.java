package dev.simulated_team.simulated.network.context;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public interface PacketContext {
    Player player();

    default Level level() {
        Player player = this.player();
        return player != null ? player.level() : null;
    }

    void disconnect(Component reason);
}

package dev.simulated_team.simulated.network.packets;

import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.SimulatedClient;
import dev.simulated_team.simulated.content.items.plunger_launcher.PlungerLauncherItemRenderer;
import dev.simulated_team.simulated.network.context.PacketContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import dev.simulated_team.simulated.network.codec.StreamCodec;
import dev.simulated_team.simulated.network.packet.SimPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;

public class PlungerLauncherShootPacket implements SimPacket {

    public static Type<PlungerLauncherShootPacket> TYPE = new Type<>(Simulated.path("plunger_launcher_shoot"), PlungerLauncherShootPacket.class);

    public static final StreamCodec<ByteBuf, PlungerLauncherShootPacket> CODEC = StreamCodec.composite(
            dev.simulated_team.simulated.network.codec.SimVanillaCodecs.HAND, packet -> packet.hand,
            PlungerLauncherShootPacket::new
    );

    protected final InteractionHand hand;

    public PlungerLauncherShootPacket(final InteractionHand hand) {
        this.hand = hand;
    }

    @Override
    public Type<? extends SimPacket> type() {
        return TYPE;
    }

    public void handle(final PacketContext context) {
        final Entity renderViewEntity = Minecraft.getInstance().getCameraEntity();
        if (renderViewEntity == null) {
            return;
        }

        final PlungerLauncherItemRenderer.RenderHandler handler = SimulatedClient.PLUNGER_LAUNCHER_RENDER_HANDLER;
        handler.basicShoot(this.hand);
        handler.playSound(this.hand, context.player().position());
    }
}

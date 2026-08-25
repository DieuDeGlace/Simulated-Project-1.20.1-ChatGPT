package dev.simulated_team.simulated.network.packets.contraption_diagram;

import dev.ryanhcode.sable.api.physics.force.QueuedForceGroup;
import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.content.entities.diagram.screen.DiagramScreen;
import dev.simulated_team.simulated.network.context.ClientPacketContext;
import dev.simulated_team.simulated.util.SimCodecUtil;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import dev.simulated_team.simulated.network.codec.ByteBufCodecs;
import dev.simulated_team.simulated.network.codec.StreamCodec;
import dev.simulated_team.simulated.network.packet.SimPacket;

import java.util.List;
import java.util.Map;

public record DiagramDataPacket(Map<ResourceLocation, List<QueuedForceGroup.PointForce>> forces, double mass) implements SimPacket {
    public static final Type<DiagramDataPacket> TYPE = new Type<>(Simulated.path("diagram_data"), DiagramDataPacket.class);

    public static final StreamCodec<FriendlyByteBuf, DiagramDataPacket> CODEC = StreamCodec.composite(
            ByteBufCodecs.map(Object2ObjectOpenHashMap::new, ByteBufCodecs.RESOURCE_LOCATION, SimCodecUtil.STREAM_POINT_FORCE.apply(ByteBufCodecs.list())), DiagramDataPacket::forces,
            ByteBufCodecs.DOUBLE, DiagramDataPacket::mass,
            DiagramDataPacket::new
    );

    public void handle(final ClientPacketContext context) {
        handle(this);
    }

    private static void handle(final DiagramDataPacket packet) {
        final Minecraft minecraft = Minecraft.getInstance();
        final Screen screen = minecraft.screen;

        if (screen instanceof final DiagramScreen diagramScreen) {
            diagramScreen.updateData(packet);
        }
    }

    @Override
    public Type<? extends SimPacket> type() {
        return TYPE;
    }
}

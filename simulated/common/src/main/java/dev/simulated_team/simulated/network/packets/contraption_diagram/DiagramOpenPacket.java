package dev.simulated_team.simulated.network.packets.contraption_diagram;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.SubLevelHelper;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.content.entities.diagram.DiagramConfig;
import dev.simulated_team.simulated.content.entities.diagram.DiagramEntity;
import dev.simulated_team.simulated.content.entities.diagram.screen.DiagramScreen;
import dev.simulated_team.simulated.network.context.ClientPacketContext;
import net.minecraft.network.FriendlyByteBuf;
import dev.simulated_team.simulated.network.codec.ByteBufCodecs;
import dev.simulated_team.simulated.network.codec.StreamCodec;
import dev.simulated_team.simulated.network.packet.SimPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

public record DiagramOpenPacket(int entityID, DiagramConfig config) implements SimPacket {
    public static final Type<DiagramOpenPacket> TYPE = new Type<>(Simulated.path("open_diagram"), DiagramOpenPacket.class);

    public static final StreamCodec<FriendlyByteBuf, DiagramOpenPacket> CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, DiagramOpenPacket::entityID,
            DiagramConfig.STREAM_CODEC, DiagramOpenPacket::config,
            DiagramOpenPacket::new
    );

    public void handle(final ClientPacketContext context) {
        final Level level = context.level();
        assert level != null;

        final Entity entity = level.getEntity(this.entityID());
        final SubLevel subLevel = entity instanceof final DiagramEntity diagram
                ? Sable.HELPER.getContaining(diagram)
                : null;

        if (entity instanceof final DiagramEntity diagram) {
            if (subLevel == null) return;

            DiagramScreen.open(diagram, this.config, subLevel);
        }
    }

    @Override
    public Type<? extends SimPacket> type() {
        return TYPE;
    }
}

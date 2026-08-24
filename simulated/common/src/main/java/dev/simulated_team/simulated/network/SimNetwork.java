package dev.simulated_team.simulated.network;

import dev.simulated_team.simulated.network.codec.StreamCodec;
import dev.simulated_team.simulated.network.context.ClientPacketContext;
import dev.simulated_team.simulated.network.context.ServerPacketContext;
import dev.simulated_team.simulated.network.packet.SimPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Map;
import java.util.Optional;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/** Forge 47 transport for the packet contracts retained from the 1.21 source. */
public final class SimNetwork {
    private static final Map<SimPacket.Type<?>, SimNetwork> CHANNELS_BY_TYPE = new ConcurrentHashMap<>();

    private final SimpleChannel channel;
    private int nextDiscriminator;

    private SimNetwork(SimpleChannel channel) {
        this.channel = channel;
    }

    public static SimNetwork create(String modId, String protocolVersion) {
        SimpleChannel channel = NetworkRegistry.newSimpleChannel(
                ResourceLocation.fromNamespaceAndPath(modId, "main"),
                () -> protocolVersion,
                protocolVersion::equals,
                protocolVersion::equals);
        return new SimNetwork(channel);
    }

    public <P extends SimPacket> void registerServerbound(
            SimPacket.Type<P> type, StreamCodec<? super FriendlyByteBuf, P> codec,
            BiConsumer<P, ServerPacketContext> handler) {
        this.register(type, codec, handler, NetworkDirection.PLAY_TO_SERVER, true);
    }

    public <P extends SimPacket> void registerClientbound(
            SimPacket.Type<P> type, StreamCodec<? super FriendlyByteBuf, P> codec,
            BiConsumer<P, ClientPacketContext> handler) {
        this.register(type, codec, handler, NetworkDirection.PLAY_TO_CLIENT, false);
    }

    private <P extends SimPacket, C> void register(
            SimPacket.Type<P> type, StreamCodec<? super FriendlyByteBuf, P> codec,
            BiConsumer<P, C> handler, NetworkDirection direction, boolean serverbound) {
        SimNetwork previous = CHANNELS_BY_TYPE.putIfAbsent(type, this);
        if (previous != null) {
            throw new IllegalStateException("Duplicate packet registration: " + type.id());
        }
        int discriminator = this.nextDiscriminator++;
        this.channel.registerMessage(discriminator, type.packetClass(),
                (packet, buffer) -> codec.encode(buffer, packet),
                codec::decode,
                (packet, contextSupplier) -> {
                    NetworkEvent.Context context = contextSupplier.get();
                    context.enqueueWork(() -> {
                        if (serverbound) {
                            ServerPlayer sender = context.getSender();
                            if (sender == null) {
                                throw new IllegalStateException("Serverbound packet without sender: " + type.id());
                            }
                            @SuppressWarnings("unchecked")
                            BiConsumer<P, ServerPacketContext> serverHandler =
                                    (BiConsumer<P, ServerPacketContext>) handler;
                            serverHandler.accept(packet, new ServerPacketContext(sender));
                        } else {
                            @SuppressWarnings("unchecked")
                            BiConsumer<P, ClientPacketContext> clientHandler =
                                    (BiConsumer<P, ClientPacketContext>) handler;
                            clientHandler.accept(packet, ClientPacketContext.INSTANCE);
                        }
                    });
                    context.setPacketHandled(true);
                }, Optional.of(direction));
    }

    public static PacketSink server() {
        return new PacketSink() {
            @Override
            public void sendPacket(Packet<?> packet) {
                throw new IllegalArgumentException("A vanilla packet cannot be sent through a serverbound Simulated sink");
            }

            @Override
            public void sendPacket(SimPacket... packets) {
                for (SimPacket packet : packets) {
                    channelFor(packet).sendToServer(packet);
                }
            }
        };
    }

    public static PacketSink player(ServerPlayer player) {
        return packet -> player.connection.send(packet);
    }

    /** Matches Veil's tracking(entity): tracking players, excluding the entity itself. */
    public static PacketSink tracking(Entity entity) {
        return packet -> PacketDistributor.TRACKING_ENTITY.with(() -> entity).send(packet);
    }

    public static PacketSink trackingAndSelf(Entity entity) {
        return packet -> PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity).send(packet);
    }

    public static PacketSink tracking(BlockEntity blockEntity) {
        if (!(blockEntity.getLevel() instanceof ServerLevel level)) {
            throw new IllegalArgumentException("Only supported on server levels");
        }
        return tracking(level, blockEntity.getBlockPos());
    }

    public static PacketSink tracking(ServerLevel level, BlockPos pos) {
        return packet -> PacketDistributor.TRACKING_CHUNK.with(() -> level.getChunkAt(pos)).send(packet);
    }

    public static PacketSink all(MinecraftServer server) {
        return packet -> server.getPlayerList().getPlayers().forEach(player -> player.connection.send(packet));
    }

    private static SimpleChannel channelFor(SimPacket packet) {
        SimNetwork network = CHANNELS_BY_TYPE.get(packet.type());
        if (network == null) {
            throw new IllegalStateException("Packet type is not registered: " + packet.type().id());
        }
        return network.channel;
    }

    @SuppressWarnings("unchecked")
    private static Packet<ClientGamePacketListener> clientboundPacket(SimPacket packet) {
        return (Packet<ClientGamePacketListener>) channelFor(packet).toVanillaPacket(
                packet, NetworkDirection.PLAY_TO_CLIENT);
    }

    @FunctionalInterface
    public interface PacketSink {
        void sendPacket(Packet<?> packet);

        default void sendPacket(SimPacket... packets) {
            if (packets.length == 0) {
                return;
            }
            if (packets.length == 1) {
                this.sendPacket(clientboundPacket(packets[0]));
                return;
            }

            List<Packet<ClientGamePacketListener>> payloads = new ArrayList<>(packets.length);
            for (SimPacket packet : packets) {
                payloads.add(clientboundPacket(packet));
            }
            this.sendPacket(new ClientboundBundlePacket(payloads));
        }
    }
}

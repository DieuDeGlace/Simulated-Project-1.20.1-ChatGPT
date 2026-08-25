package dev.simulated_team.simulated.neoforge.mixin.self_mixins;

import dev.simulated_team.simulated.content.entities.diagram.DiagramEntity;
import dev.simulated_team.simulated.Simulated;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.level.Level;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.network.NetworkHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.concurrent.atomic.AtomicInteger;

@Mixin(DiagramEntity.class)
public abstract class DiagramEntityMixin extends HangingEntity implements IEntityAdditionalSpawnData {
    @Unique
    private static final AtomicInteger SIMULATED$SPAWN_TRACES = new AtomicInteger();

    protected DiagramEntityMixin(final EntityType<? extends HangingEntity> entityType, final Level level) {
        super(entityType, level);
    }

    @Shadow public abstract void addAdditionalSaveData(CompoundTag tag);

    @Shadow public abstract void readAdditionalSaveData(CompoundTag tag);

    @Override
    public void writeSpawnData(final FriendlyByteBuf registryFriendlyByteBuf) {
        final CompoundTag compound = new CompoundTag();
        this.addAdditionalSaveData(compound);
        registryFriendlyByteBuf.writeNbt(compound);

        if (SIMULATED$SPAWN_TRACES.getAndIncrement() < 16) {
            final Entity entity = (Entity) (Object) this;
            Simulated.LOGGER.info("SIMULATED-DIAGRAM-SPAWN-WRITE: entity={}, side={}, data={}, box={}",
                    entity.getId(), entity.level().isClientSide ? "client" : "server", compound,
                    entity.getBoundingBox());
        }
    }

    @Override
    public void readSpawnData(final FriendlyByteBuf registryFriendlyByteBuf) {
        final CompoundTag compound = registryFriendlyByteBuf.readNbt();
        this.readAdditionalSaveData(compound);

        if (SIMULATED$SPAWN_TRACES.getAndIncrement() < 16) {
            final Entity entity = (Entity) (Object) this;
            Simulated.LOGGER.info("SIMULATED-DIAGRAM-SPAWN-READ: entity={}, side={}, data={}, position={}, box={}",
                    entity.getId(), entity.level().isClientSide ? "client" : "server", compound,
                    entity.position(), entity.getBoundingBox());
        }
    }

    /**
     * Forge only invokes IEntityAdditionalSpawnData when the entity uses its custom spawn packet.
     * The upstream NeoForge interface supplies that path automatically; Forge 1.20.1 does not.
     */
    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket((Entity) (Object) this);
    }
}

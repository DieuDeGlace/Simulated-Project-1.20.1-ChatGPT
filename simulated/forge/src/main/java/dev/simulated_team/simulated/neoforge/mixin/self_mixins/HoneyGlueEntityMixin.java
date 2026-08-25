package dev.simulated_team.simulated.neoforge.mixin.self_mixins;

import dev.simulated_team.simulated.content.entities.honey_glue.HoneyGlueEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.network.NetworkHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(HoneyGlueEntity.class)
public abstract class HoneyGlueEntityMixin extends Entity implements IEntityAdditionalSpawnData {

    protected HoneyGlueEntityMixin(final EntityType<?> entityType, final Level level) {
        super(entityType, level);
    }

    @Shadow
    public abstract void addAdditionalSaveData(CompoundTag tag);

    @Shadow
    public abstract void readAdditionalSaveData(CompoundTag tag);

    @Override
    public void writeSpawnData(final FriendlyByteBuf registryFriendlyByteBuf) {
        final CompoundTag compound = new CompoundTag();
        this.addAdditionalSaveData(compound);
        registryFriendlyByteBuf.writeNbt(compound);
    }

    @Override
    public void readSpawnData(final FriendlyByteBuf registryFriendlyByteBuf) {
        this.readAdditionalSaveData(registryFriendlyByteBuf.readNbt());
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket((Entity) (Object) this);
    }
    
}

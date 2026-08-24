package dev.eriksonn.aeronautics.content.particle;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.foundation.particle.ICustomParticleDataWithSprite;
import dev.eriksonn.aeronautics.index.AeroParticleTypes;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;

public class PropellerAirParticleData implements ParticleOptions, ICustomParticleDataWithSprite<PropellerAirParticleData> {

    private static final MapCodec<PropellerAirParticleData> CODEC = RecordCodecBuilder.mapCodec((i) -> i.group(
                    Codec.BOOL.fieldOf("collision").forGetter((p) -> p.enableCollision),
                    Codec.BOOL.fieldOf("virtual").forGetter(p -> p.isVirtual))
            .apply(i, PropellerAirParticleData::new));

    private static final ParticleOptions.Deserializer<PropellerAirParticleData> DESERIALIZER = new ParticleOptions.Deserializer<>() {
        @Override
        public PropellerAirParticleData fromCommand(final ParticleType<PropellerAirParticleData> type, final StringReader reader) throws CommandSyntaxException {
            reader.expect(' ');
            final boolean collision = reader.readBoolean();
            reader.expect(' ');
            return new PropellerAirParticleData(collision, reader.readBoolean());
        }

        @Override
        public PropellerAirParticleData fromNetwork(final ParticleType<PropellerAirParticleData> type, final FriendlyByteBuf buffer) {
            return new PropellerAirParticleData(buffer.readBoolean(), buffer.readBoolean());
        }
    };


    boolean enableCollision;
    boolean isVirtual;

    public PropellerAirParticleData(boolean enableCollision, boolean isVirtual) {
        this.enableCollision = enableCollision;
        this.isVirtual = isVirtual;
    }

    public PropellerAirParticleData() {
        this(true, false);
    }

    @Override
    public ParticleType<?> getType() {
        return AeroParticleTypes.PROPELLER_AIR_FLOW.get();
    }

    @Override
    public ParticleEngine.SpriteParticleRegistration<PropellerAirParticleData> getMetaFactory() {
        return PropellerAirParticle.Factory::new;
    }

    @Override
    public Codec<PropellerAirParticleData> getCodec(ParticleType<PropellerAirParticleData> particleType) {
        return CODEC.codec();
    }

    @Override
    public ParticleOptions.Deserializer<PropellerAirParticleData> getDeserializer() {
        return DESERIALIZER;
    }

    @Override
    public void writeToNetwork(final FriendlyByteBuf buffer) {
        buffer.writeBoolean(this.enableCollision);
        buffer.writeBoolean(this.isVirtual);
    }

    @Override
    public String writeToString() {
        return String.format(java.util.Locale.ROOT, "%s %s %s", BuiltInRegistries.PARTICLE_TYPE.getKey(this.getType()), this.enableCollision, this.isVirtual);
    }
}

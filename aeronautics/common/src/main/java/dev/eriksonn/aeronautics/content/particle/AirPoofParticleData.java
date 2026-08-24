package dev.eriksonn.aeronautics.content.particle;

import com.mojang.brigadier.StringReader;
import com.mojang.serialization.Codec;
import com.simibubi.create.foundation.particle.ICustomParticleDataWithSprite;
import dev.eriksonn.aeronautics.index.AeroParticleTypes;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;

public class AirPoofParticleData implements ParticleOptions, ICustomParticleDataWithSprite<AirPoofParticleData> {
    public static final AirPoofParticleData INSTANCE = new AirPoofParticleData();
    private static final Codec<AirPoofParticleData> CODEC = Codec.unit(INSTANCE);
    private static final ParticleOptions.Deserializer<AirPoofParticleData> DESERIALIZER = new ParticleOptions.Deserializer<>() {
        @Override
        public AirPoofParticleData fromCommand(final ParticleType<AirPoofParticleData> type, final StringReader reader) {
            return INSTANCE;
        }

        @Override
        public AirPoofParticleData fromNetwork(final ParticleType<AirPoofParticleData> type, final FriendlyByteBuf buffer) {
            return INSTANCE;
        }
    };

    private AirPoofParticleData() {}

    @Override
    public ParticleEngine.SpriteParticleRegistration<AirPoofParticleData> getMetaFactory() {
        return AirPoofParticle.Factory::new;
    }

    @Override
    public Codec<AirPoofParticleData> getCodec(final ParticleType<AirPoofParticleData> type) {
        return CODEC;
    }

    @Override
    public ParticleOptions.Deserializer<AirPoofParticleData> getDeserializer() {
        return DESERIALIZER;
    }

    @Override
    public void writeToNetwork(final FriendlyByteBuf buffer) {
    }

    @Override
    public String writeToString() {
        return BuiltInRegistries.PARTICLE_TYPE.getKey(this.getType()).toString();
    }

    @Override
    public ParticleType<?> getType() {
        return AeroParticleTypes.AIR_POOF.get();
    }
}

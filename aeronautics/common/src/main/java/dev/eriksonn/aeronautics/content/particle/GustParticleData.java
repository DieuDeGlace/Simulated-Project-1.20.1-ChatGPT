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
import net.minecraft.util.ExtraCodecs;
import org.joml.Quaternionf;

public record GustParticleData(
        Quaternionf orientation) implements ParticleOptions, ICustomParticleDataWithSprite<GustParticleData> {

    private static final MapCodec<GustParticleData> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ExtraCodecs.QUATERNIONF.fieldOf("orientation").forGetter(o -> o.orientation)
    ).apply(instance, GustParticleData::new));

    private static final ParticleOptions.Deserializer<GustParticleData> DESERIALIZER = new ParticleOptions.Deserializer<>() {
        @Override
        public GustParticleData fromCommand(final ParticleType<GustParticleData> type, final StringReader reader) throws CommandSyntaxException {
            reader.expect(' ');
            final float x = reader.readFloat();
            reader.expect(' ');
            final float y = reader.readFloat();
            reader.expect(' ');
            final float z = reader.readFloat();
            reader.expect(' ');
            return new GustParticleData(new Quaternionf(x, y, z, reader.readFloat()));
        }

        @Override
        public GustParticleData fromNetwork(final ParticleType<GustParticleData> type, final FriendlyByteBuf buffer) {
            return new GustParticleData(new Quaternionf(buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readFloat()));
        }
    };

    public GustParticleData() {
        this(new Quaternionf());
    }


    @Override
    public ParticleEngine.SpriteParticleRegistration<GustParticleData> getMetaFactory() {
        return GustParticle.Factory::new;
    }

    @Override
    public Codec<GustParticleData> getCodec(final ParticleType<GustParticleData> type) {
        return CODEC.codec();
    }

    @Override
    public ParticleOptions.Deserializer<GustParticleData> getDeserializer() {
        return DESERIALIZER;
    }

    @Override
    public void writeToNetwork(final FriendlyByteBuf buffer) {
        buffer.writeFloat(this.orientation.x());
        buffer.writeFloat(this.orientation.y());
        buffer.writeFloat(this.orientation.z());
        buffer.writeFloat(this.orientation.w());
    }

    @Override
    public String writeToString() {
        return String.format(java.util.Locale.ROOT, "%s %f %f %f %f", BuiltInRegistries.PARTICLE_TYPE.getKey(this.getType()),
                this.orientation.x(), this.orientation.y(), this.orientation.z(), this.orientation.w());
    }

    @Override
    public ParticleType<?> getType() {
        return AeroParticleTypes.GUST.get();
    }
}

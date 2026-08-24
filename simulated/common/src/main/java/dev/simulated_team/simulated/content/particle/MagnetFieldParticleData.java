package dev.simulated_team.simulated.content.particle;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.foundation.particle.ICustomParticleDataWithSprite;
import dev.simulated_team.simulated.index.SimParticleTypes;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;

public class MagnetFieldParticleData implements ParticleOptions, ICustomParticleDataWithSprite<MagnetFieldParticleData> {
    public static final MapCodec<MagnetFieldParticleData> CODEC = RecordCodecBuilder.mapCodec((i) -> {
        return i.group(Codec.BOOL.fieldOf("negative").forGetter((p) -> {
            return p.negative;
        })).apply(i, MagnetFieldParticleData::new);
    });
    public static final ParticleOptions.Deserializer<MagnetFieldParticleData> DESERIALIZER = new ParticleOptions.Deserializer<>() {
        @Override
        public MagnetFieldParticleData fromCommand(final ParticleType<MagnetFieldParticleData> type,
                                                    final StringReader reader) throws CommandSyntaxException {
            reader.expect(' ');
            return new MagnetFieldParticleData(reader.readBoolean());
        }

        @Override
        public MagnetFieldParticleData fromNetwork(final ParticleType<MagnetFieldParticleData> type,
                                                    final FriendlyByteBuf buffer) {
            return new MagnetFieldParticleData(buffer.readBoolean());
        }
    };
    private boolean negative;

    public MagnetFieldParticleData(final boolean negative) {
        this.negative = negative;
    }

    public MagnetFieldParticleData() {
        this.negative = false;
    }

    public ParticleType<?> getType() {
        return SimParticleTypes.MAGNET_FIELD.get();
    }

    public void writeToNetwork(final FriendlyByteBuf buffer) {
        buffer.writeBoolean(this.negative);
    }

    public ParticleEngine.SpriteParticleRegistration<MagnetFieldParticleData> getMetaFactory() {
        return MagnetFieldParticle.Factory::new;
    }

    public String writeToString() {
        return String.format(java.util.Locale.ROOT, "%s %s", BuiltInRegistries.PARTICLE_TYPE.getKey(this.getType()), this.negative);
    }

    public ParticleOptions.Deserializer<MagnetFieldParticleData> getDeserializer() {
        return DESERIALIZER;
    }

    public Codec<MagnetFieldParticleData> getCodec(final ParticleType<MagnetFieldParticleData> type) {
        return CODEC.codec();
    }

    public boolean isNegative() {
        return this.negative;
    }

    public void setNegative(final boolean negative) {
        this.negative = negative;
    }
}

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

public class HotAirEmberParticleData implements ParticleOptions, ICustomParticleDataWithSprite<HotAirEmberParticleData> {

    private static final MapCodec<HotAirEmberParticleData> CODEC = RecordCodecBuilder.mapCodec((i) -> i.group(
                    Codec.BOOL.fieldOf("isSoul").forGetter((p) -> p.isSoul)
    ).apply(i, HotAirEmberParticleData::new));

    private static final ParticleOptions.Deserializer<HotAirEmberParticleData> DESERIALIZER = new ParticleOptions.Deserializer<>() {
        @Override
        public HotAirEmberParticleData fromCommand(final ParticleType<HotAirEmberParticleData> type, final StringReader reader) throws CommandSyntaxException {
            reader.expect(' ');
            return new HotAirEmberParticleData(reader.readBoolean());
        }

        @Override
        public HotAirEmberParticleData fromNetwork(final ParticleType<HotAirEmberParticleData> type, final FriendlyByteBuf buffer) {
            return new HotAirEmberParticleData(buffer.readBoolean());
        }
    };

    protected final boolean isSoul;

    public HotAirEmberParticleData(final boolean isSoul) {
        this.isSoul = isSoul;
    }

    public HotAirEmberParticleData() {
        this.isSoul = false;
    }

    @Override
    public ParticleType<?> getType() {
        return AeroParticleTypes.HOT_AIR_EMBER.get();
    }

    @Override
    public ParticleEngine.SpriteParticleRegistration<HotAirEmberParticleData> getMetaFactory() {
        return HotAirEmberParticle.Factory::new;
    }

    @Override
    public Codec<HotAirEmberParticleData> getCodec(final ParticleType<HotAirEmberParticleData> particleType) {
        return CODEC.codec();
    }

    @Override
    public ParticleOptions.Deserializer<HotAirEmberParticleData> getDeserializer() {
        return DESERIALIZER;
    }

    @Override
    public void writeToNetwork(final FriendlyByteBuf buffer) {
        buffer.writeBoolean(this.isSoul);
    }

    @Override
    public String writeToString() {
        return String.format(java.util.Locale.ROOT, "%s %s", BuiltInRegistries.PARTICLE_TYPE.getKey(this.getType()), this.isSoul);
    }
}

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
import net.minecraft.world.phys.Vec3;

public class MagnetFieldParticleData2 implements ParticleOptions, ICustomParticleDataWithSprite<MagnetFieldParticleData2> {
    //public static final MapCodec<MagnetFieldParticleData2> CODEC = RecordCodecBuilder.mapCodec((i) -> {
    //    return i.group(Codec.BOOL.fieldOf("negative").forGetter((p) -> {
    //        return p.negative;
    //    })).apply(i, MagnetFieldParticleData2::new);
    //});

    public static final MapCodec<MagnetFieldParticleData2> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Vec3.CODEC.fieldOf("previous_offset").forGetter(p -> p.previousOffset),
            Vec3.CODEC.fieldOf("next_offset").forGetter(p -> p.nextOffset),
            Codec.BOOL.fieldOf("negative").forGetter(p -> p.negative),
            Codec.INT.fieldOf("timeUntilEnd").forGetter(p -> p.timeUntilEnd)
    ).apply(instance, MagnetFieldParticleData2::new));

    public static final ParticleOptions.Deserializer<MagnetFieldParticleData2> DESERIALIZER = new ParticleOptions.Deserializer<>() {
        @Override
        public MagnetFieldParticleData2 fromCommand(final ParticleType<MagnetFieldParticleData2> type,
                                                     final StringReader reader) throws CommandSyntaxException {
            reader.expect(' ');
            final Vec3 previousOffset = new Vec3(reader.readDouble(), readSpacedDouble(reader), readSpacedDouble(reader));
            final Vec3 nextOffset = new Vec3(readSpacedDouble(reader), readSpacedDouble(reader), readSpacedDouble(reader));
            reader.expect(' ');
            final boolean negative = reader.readBoolean();
            reader.expect(' ');
            final int timeUntilEnd = reader.readInt();
            return new MagnetFieldParticleData2(previousOffset, nextOffset, negative, timeUntilEnd);
        }

        private double readSpacedDouble(final StringReader reader) throws CommandSyntaxException {
            reader.expect(' ');
            return reader.readDouble();
        }

        @Override
        public MagnetFieldParticleData2 fromNetwork(final ParticleType<MagnetFieldParticleData2> type,
                                                     final FriendlyByteBuf buffer) {
            return new MagnetFieldParticleData2(
                    new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()),
                    new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()),
                    buffer.readBoolean(), buffer.readVarInt());
        }
    };
    Vec3 previousOffset;
    Vec3 nextOffset;
    private boolean negative;
    private int timeUntilEnd;

    public MagnetFieldParticleData2(final Vec3 previousOffset, final Vec3 nextOffset, final boolean negative, final int timeUntilEnd) {
        this.negative = negative;
        this.previousOffset = previousOffset;
        this.nextOffset = nextOffset;
        this.timeUntilEnd = timeUntilEnd;
    }

    public MagnetFieldParticleData2() {
        this.negative = false;
    }

    public ParticleType<?> getType() {
        return SimParticleTypes.MAGNET_FIELD2.get();
    }

    public void writeToNetwork(final FriendlyByteBuf buffer) {
        buffer.writeDouble(this.previousOffset.x);
        buffer.writeDouble(this.previousOffset.y);
        buffer.writeDouble(this.previousOffset.z);
        buffer.writeDouble(this.nextOffset.x);
        buffer.writeDouble(this.nextOffset.y);
        buffer.writeDouble(this.nextOffset.z);
        buffer.writeBoolean(this.negative);
        buffer.writeVarInt(this.timeUntilEnd);
    }

    public ParticleEngine.SpriteParticleRegistration<MagnetFieldParticleData2> getMetaFactory() {
        return MagnetFieldParticle2.Factory::new;
    }

    public String writeToString() {
        return String.format(java.util.Locale.ROOT, "%s %f %f %f %f %f %f %s %d", BuiltInRegistries.PARTICLE_TYPE.getKey(this.getType()),
                this.previousOffset.x, this.previousOffset.y, this.previousOffset.z,
                this.nextOffset.x, this.nextOffset.y, this.nextOffset.z, this.negative, this.timeUntilEnd);
    }

    public ParticleOptions.Deserializer<MagnetFieldParticleData2> getDeserializer() {
        return DESERIALIZER;
    }

    public Codec<MagnetFieldParticleData2> getCodec(final ParticleType<MagnetFieldParticleData2> type) {
        return CODEC.codec();
    }

    public boolean isNegative() {
        return this.negative;
    }

    public int getTimeUntilEnd()
    {
        return this.timeUntilEnd;
    }

    public void setNegative(final boolean negative) {
        this.negative = negative;
    }
}

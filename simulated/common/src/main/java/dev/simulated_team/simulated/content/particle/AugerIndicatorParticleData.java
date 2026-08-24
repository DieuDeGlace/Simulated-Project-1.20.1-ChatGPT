package dev.simulated_team.simulated.content.particle;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.foundation.particle.ICustomParticleDataWithSprite;
import dev.simulated_team.simulated.index.SimParticleTypes;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Stolen shamelessly from {@link com.simibubi.create.content.kinetics.base.RotationIndicatorParticleData}
 */
public class AugerIndicatorParticleData implements ParticleOptions, ICustomParticleDataWithSprite<AugerIndicatorParticleData> {
    public static final MapCodec<AugerIndicatorParticleData> CODEC = RecordCodecBuilder.mapCodec(i -> i
            .group(Codec.INT.fieldOf("color")
                            .forGetter(p -> p.color),
                    Codec.FLOAT.fieldOf("speed")
                            .forGetter(p -> p.speed),
                    Codec.FLOAT.fieldOf("radius1")
                            .forGetter(p -> p.radius1),
                    Codec.FLOAT.fieldOf("radius2")
                            .forGetter(p -> p.radius2),
                    Codec.FLOAT.fieldOf("angle_offset")
                            .forGetter(p -> p.angleOffset),
                    Codec.INT.fieldOf("life_span")
                            .forGetter(p -> p.lifeSpan),
                    Direction.CODEC.fieldOf("direction")
                            .forGetter(p -> p.direction))
            .apply(i, AugerIndicatorParticleData::new));

    public static final ParticleOptions.Deserializer<AugerIndicatorParticleData> DESERIALIZER = new ParticleOptions.Deserializer<>() {
        @Override
        public AugerIndicatorParticleData fromCommand(final ParticleType<AugerIndicatorParticleData> type,
                                                       final StringReader reader) throws CommandSyntaxException {
            reader.expect(' ');
            final int color = reader.readInt();
            reader.expect(' ');
            final float speed = reader.readFloat();
            reader.expect(' ');
            final float radius1 = reader.readFloat();
            reader.expect(' ');
            final float radius2 = reader.readFloat();
            reader.expect(' ');
            final float angleOffset = reader.readFloat();
            reader.expect(' ');
            final int lifeSpan = reader.readInt();
            reader.expect(' ');
            final Direction direction = Direction.from3DDataValue(reader.readInt());
            return new AugerIndicatorParticleData(color, speed, radius1, radius2, angleOffset, lifeSpan, direction);
        }

        @Override
        public AugerIndicatorParticleData fromNetwork(final ParticleType<AugerIndicatorParticleData> type,
                                                       final FriendlyByteBuf buffer) {
            return new AugerIndicatorParticleData(buffer.readInt(), buffer.readFloat(), buffer.readFloat(),
                    buffer.readFloat(), buffer.readFloat(), buffer.readVarInt(), buffer.readEnum(Direction.class));
        }
    };

    public final int color;
    public final float speed;
    public final float radius1;
    public final float radius2;
    public float angleOffset;
    public final int lifeSpan;
    public final Direction direction;

    public AugerIndicatorParticleData(final int color, final float speed,
                                         final float radius1, final float radius2, final float angleOffset,
                                         final int lifeSpan,
                                         final Direction direction) {
        this.color = color;
        this.speed = speed;
        this.radius1 = radius1;
        this.radius2 = radius2;
        this.angleOffset = angleOffset;
        this.lifeSpan = lifeSpan;
        this.direction = direction;
    }

    public AugerIndicatorParticleData() {
        this(0, 0, 0, 0, 0, 0, Direction.NORTH);
    }

    @Override
    public ParticleType<?> getType() {
        return SimParticleTypes.AUGER_INDICATOR.get();
    }

    @Override
    public void writeToNetwork(final FriendlyByteBuf buffer) {
        buffer.writeInt(this.color);
        buffer.writeFloat(this.speed);
        buffer.writeFloat(this.radius1);
        buffer.writeFloat(this.radius2);
        buffer.writeFloat(this.angleOffset);
        buffer.writeVarInt(this.lifeSpan);
        buffer.writeEnum(this.direction);
    }

    @Override
    public String writeToString() {
        return String.format(java.util.Locale.ROOT, "%s %d %f %f %f %f %d %d", BuiltInRegistries.PARTICLE_TYPE.getKey(this.getType()), this.color,
                this.speed, this.radius1, this.radius2, this.angleOffset, this.lifeSpan, this.direction.get3DDataValue());
    }

    @Override
    public ParticleOptions.Deserializer<AugerIndicatorParticleData> getDeserializer() {
        return DESERIALIZER;
    }

    @Override
    public Codec<AugerIndicatorParticleData> getCodec(final ParticleType<AugerIndicatorParticleData> type) {
        return CODEC.codec();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public ParticleEngine.SpriteParticleRegistration<AugerIndicatorParticleData> getMetaFactory() {
        return AugerIndicatorParticle.Factory::new;
    }
}

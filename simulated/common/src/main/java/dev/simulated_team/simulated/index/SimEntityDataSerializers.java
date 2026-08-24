package dev.simulated_team.simulated.index;

import dev.simulated_team.simulated.service.SimEntityDataSerialization;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.world.phys.Vec3;

public class SimEntityDataSerializers {

    public static final EntityDataSerializer<Vec3> VEC3 = new EntityDataSerializer<>() {
        @Override
        public void write(final FriendlyByteBuf buffer, final Vec3 value) {
            buffer.writeDouble(value.x);
            buffer.writeDouble(value.y);
            buffer.writeDouble(value.z);
        }

        @Override
        public Vec3 read(final FriendlyByteBuf buffer) {
            return new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
        }

        @Override
        public Vec3 copy(final Vec3 object) {
            return new Vec3(object.x, object.y, object.z);
        }
    };

    public static void register() {
        SimEntityDataSerialization.INSTANCE.registerDataSerializer("vec3", VEC3);
    }
}

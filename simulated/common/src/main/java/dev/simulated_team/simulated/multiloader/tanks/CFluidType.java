package dev.simulated_team.simulated.multiloader.tanks;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * A loader-independent representation of a fluid
 */
public record CFluidType(Fluid fluid, CompoundTag data) {
    public boolean isBlank() {
        return this.equals(BLANK);
    }

    public static final CFluidType BLANK = new CFluidType(Fluids.EMPTY, new CompoundTag());

    public CompoundTag write() {
        final CompoundTag tag = new CompoundTag();
        tag.putString("Fluid", BuiltInRegistries.FLUID.getKey(this.fluid).toString());

        if (!this.data.isEmpty()) {
            tag.put("data", this.data.copy());
        }

        return tag;
    }

    public static CFluidType read(final CompoundTag tag) {
        final Fluid fluid = BuiltInRegistries.FLUID.get(ResourceLocation.tryParse(tag.getString("Fluid")));
        final CompoundTag data = tag.contains("data", Tag.TAG_COMPOUND)
                ? tag.getCompound("data").copy()
                : new CompoundTag();

        return new CFluidType(fluid, data);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        // Java 17 port: original used a record pattern.
        if (obj instanceof CFluidType other) {
            final Fluid fluid1 = other.fluid();
            final CompoundTag data1 = other.data();
            // both haves tag, or both no haves tag
            return this.fluid.isSame(fluid1) && this.data.equals(data1);
        }
        return false;
    }
}

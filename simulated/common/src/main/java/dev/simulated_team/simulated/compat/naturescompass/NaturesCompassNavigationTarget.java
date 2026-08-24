package dev.simulated_team.simulated.compat.naturescompass;

import dev.simulated_team.simulated.content.blocks.nav_table.NavTableBlockEntity;
import dev.simulated_team.simulated.content.blocks.nav_table.navigation_target.NavigationTarget;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class NaturesCompassNavigationTarget implements NavigationTarget {
	@Override
	public @Nullable Vec3 getTarget(final NavTableBlockEntity navBE, final ItemStack self) {
		final CompoundTag tag = self.getTag();
		if (tag != null && tag.contains("FoundX", Tag.TAG_INT) && tag.contains("FoundZ", Tag.TAG_INT)) {
			final int x = tag.getInt("FoundX");
			final int z = tag.getInt("FoundZ");
			final Vec3 pos = navBE.getProjectedSelfPos();
			return new Vec3(x, pos.y(), z);
		}

		return null;
	}
}

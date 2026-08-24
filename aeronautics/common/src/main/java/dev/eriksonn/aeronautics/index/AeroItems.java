package dev.eriksonn.aeronautics.index;

import com.simibubi.create.AllItems;
import dev.simulated_team.simulated.registrate.SimulatedRegistrate;
import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import com.tterrag.registrate.util.entry.ItemEntry;
import com.tterrag.registrate.util.nullness.NonNullUnaryOperator;
import dev.eriksonn.aeronautics.Aeronautics;
import dev.eriksonn.aeronautics.content.components.Levitating;
import dev.eriksonn.aeronautics.content.items.AviatorsGogglesItem;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.RecordItem;

public class AeroItems {
	private static final SimulatedRegistrate REGISTRATE = Aeronautics.getRegistrate();

	public static final ItemEntry<AviatorsGogglesItem> AVIATORS_GOGGLES = REGISTRATE
					.item("aviators_goggles", AviatorsGogglesItem::new)
					.lang("Aviator's Goggles")
					.recipe((c, p) -> ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, c.get(), 1)
							.requires(AeroTags.ItemTags.LEATHERS)
							.requires(AllItems.GOGGLES)
							.unlockedBy("has_ingredient", RegistrateRecipeProvider.has(AllItems.GOGGLES))
							.save(p))
					.tag(AeroTags.ItemTags.ARMORS)
					.tag(AeroTags.ItemTags.HEAD_ARMOR)
					.tag(ItemTags.FREEZE_IMMUNE_WEARABLES)
					.register();

	public static ItemEntry<RecordItem> MUSIC_DISC_CLOUD_SKIPPER =
			REGISTRATE.item("music_disc_cloud_skipper",
					properties -> new RecordItem(12, AeroSoundEvents.MUSIC_DISC_CLOUD_SKIPPER::event, properties, 225 * 20))
					.properties(p -> p
							.stacksTo(1)
							.rarity(Rarity.RARE)
					)
					.tag(AeroTags.ItemTags.MUSIC_DISCS)
					.lang("Music Disc")
					.register();

	public static ItemEntry<Item> ENDSTONE_POWDER = ingredient("end_stone_powder", p -> p);

	private static ItemEntry<Item> ingredient(final String name, NonNullUnaryOperator<Item.Properties> poperator) {
		return REGISTRATE.item(name, Item::new)
				.properties(poperator)
				.register();
	}

	public static void init() {
		dev.simulated_team.simulated.compat.components.ItemComponentCompat.registerDefault(
				MUSIC_DISC_CLOUD_SKIPPER, AeroDataComponents.LEVITATING, Levitating.DEFAULT);
		dev.simulated_team.simulated.compat.components.ItemComponentCompat.registerDefault(
				ENDSTONE_POWDER, AeroDataComponents.LEVITATING, Levitating.END_STONE);
	}
}

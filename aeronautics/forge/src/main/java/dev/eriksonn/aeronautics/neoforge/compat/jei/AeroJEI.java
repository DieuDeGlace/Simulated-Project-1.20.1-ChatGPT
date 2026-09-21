package dev.eriksonn.aeronautics.neoforge.compat.jei;

import com.simibubi.create.compat.jei.ConversionRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeBuilder;
import dev.eriksonn.aeronautics.Aeronautics;
import dev.eriksonn.aeronautics.index.AeroBlocks;
import dev.eriksonn.aeronautics.index.AeroItems;
import dev.eriksonn.aeronautics.index.AeroTags;
import dev.eriksonn.aeronautics.neoforge.index.AeroFluidsNeoForge;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.List;

@JeiPlugin
public final class AeroJEI implements IModPlugin {
	private static final RecipeType<ConversionRecipe> MYSTERY_CONVERSION =
			RecipeType.create("create", "mystery_conversion", ConversionRecipe.class);

	@Override
	public net.minecraft.resources.ResourceLocation getPluginUid() {
		return Aeronautics.path("jei_plugin");
	}

	@Override
	public void registerRecipes(final IRecipeRegistration registration) {
		final var blend = AeroFluidsNeoForge.LEVITITE_BLEND.getBucket().get().getDefaultInstance();
		final var musicDisc = new ProcessingRecipeBuilder<>(ConversionRecipe::new,
				Aeronautics.path("conversion_music_disc_cloud_skipper"))
				.withItemIngredients(Ingredient.of(AeroTags.ItemTags.CONVERTS_TO_CLOUD_SKIPPER))
				.withSingleItemOutput(AeroItems.MUSIC_DISC_CLOUD_SKIPPER.asStack())
				.build();
		registration.addRecipes(MYSTERY_CONVERSION, List.of(
				ConversionRecipe.create(blend, AeroBlocks.LEVITITE.asStack()),
				ConversionRecipe.create(blend, AeroBlocks.PEARLESCENT_LEVITITE.asStack()),
				musicDisc));
	}
}

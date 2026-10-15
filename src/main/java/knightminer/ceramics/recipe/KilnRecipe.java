package knightminer.ceramics.recipe;

import knightminer.ceramics.Registration;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class KilnRecipe extends AbstractCookingRecipe {
  public KilnRecipe(String group, CookingBookCategory category, Ingredient ingredient, ItemStack result, float experience, int cookTime) {
    super(Registration.KILN_RECIPE.get(), group, category, ingredient, result, experience, cookTime);
  }

  public Ingredient getInput() {
    return ingredient;
  }

  /** Output of the recipe, kiln recipes do not depend on registries */
  public ItemStack getOutput() {
    return result;
  }

  @Override
  public ItemStack getToastSymbol() {
    return new ItemStack(Registration.KILN);
  }

  @Override
  public RecipeSerializer<?> getSerializer() {
    return Registration.KILN_SERIALIZER.get();
  }

  @Override
  public boolean isSpecial() {
    // the kiln has no recipe book, so keep the vanilla book from warning about these recipes
    return true;
  }
}

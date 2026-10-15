package knightminer.ceramics.recipe;

import knightminer.ceramics.Registration;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

public class KilnRecipe extends AbstractCookingRecipe {
  public KilnRecipe(String group, CookingBookCategory category, Ingredient ingredient, ItemStack result, float experience, int cookTime) {
    super(group, category, ingredient, result, experience, cookTime);
  }

  public Ingredient getInput() {
    return input();
  }

  /** Output of the recipe, kiln recipes do not depend on registries */
  public ItemStack getOutput() {
    return result();
  }

  /** Was getToastSymbol, now the icon of the recipe display */
  @Override
  protected Item furnaceIcon() {
    return Registration.KILN.asItem();
  }

  @Override
  public RecipeSerializer<KilnRecipe> getSerializer() {
    return Registration.KILN_SERIALIZER.get();
  }

  @Override
  public RecipeType<KilnRecipe> getType() {
    return Registration.KILN_RECIPE.get();
  }

  @Override
  public RecipeBookCategory recipeBookCategory() {
    return switch (category()) {
      case BLOCKS -> RecipeBookCategories.FURNACE_BLOCKS;
      case FOOD -> RecipeBookCategories.FURNACE_FOOD;
      case MISC -> RecipeBookCategories.FURNACE_MISC;
    };
  }

  @Override
  public boolean isSpecial() {
    // the kiln has no recipe book, so keep the vanilla book from warning about these recipes
    return true;
  }
}

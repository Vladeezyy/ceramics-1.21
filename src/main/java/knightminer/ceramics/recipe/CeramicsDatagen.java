package knightminer.ceramics.recipe;

import knightminer.ceramics.Registration;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

/** Public helpers for performing datagen */
public class CeramicsDatagen {
  private CeramicsDatagen() {}

  /** Creates a kiln recipe builder, the book category is blocks for block items and misc otherwise */
  public static SimpleCookingRecipeBuilder kilnRecipe(Ingredient input, RecipeCategory category, ItemLike output, float experience, int cookTime) {
    return SimpleCookingRecipeBuilder.generic(input, category, bookCategory(output), output, experience, cookTime, KilnRecipe::new);
  }

  /** Book category, blocks for block items and misc otherwise, as vanilla chose it before 26.1 */
  public static CookingBookCategory bookCategory(ItemLike output) {
    return output.asItem() instanceof net.minecraft.world.item.BlockItem ? CookingBookCategory.BLOCKS : CookingBookCategory.MISC;
  }
}

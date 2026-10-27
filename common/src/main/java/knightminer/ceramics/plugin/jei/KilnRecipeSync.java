package knightminer.ceramics.plugin.jei;

import knightminer.ceramics.Registration;
import knightminer.ceramics.recipe.KilnRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeMap;

import java.util.Collection;
import java.util.List;

/**
 * Kiln recipes received from the server. Since 1.21.2 the client has no recipe manager; the server sends the kiln
 * recipes on datapack sync (see Registration) and the client keeps them here for JEI.
 */
public final class KilnRecipeSync {
  private static Collection<RecipeHolder<KilnRecipe>> recipes = List.of();

  private KilnRecipeSync() {}

  /** Stores the recipes received by the client (the loader part calls this before JEI starts) */
  public static void setRecipes(RecipeMap received) {
    Collection<RecipeHolder<KilnRecipe>> list = received.byType(Registration.KILN_RECIPE.get());
    setRecipes(list == null ? List.of() : list);
  }

  /** Stores the kiln recipes received by the client */
  public static void setRecipes(Collection<RecipeHolder<KilnRecipe>> received) {
    recipes = List.copyOf(received);
  }

  /** Kiln recipes of the current server */
  public static Collection<RecipeHolder<KilnRecipe>> getRecipes() {
    return recipes;
  }
}

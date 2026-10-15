package knightminer.ceramics.plugin.jei;

import knightminer.ceramics.Registration;
import knightminer.ceramics.recipe.KilnRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeMap;
import net.neoforged.neoforge.client.event.RecipesReceivedEvent;

import java.util.Collection;
import java.util.List;

/**
 * Kiln recipes received from the server. Since 1.21.2 the client has no recipe manager; the server sends the kiln
 * recipes on datapack sync (see Registration) and the client keeps them here for JEI.
 */
public final class KilnRecipeSync {
  private static RecipeMap recipes = RecipeMap.EMPTY;

  private KilnRecipeSync() {}

  /** Stores the recipes, runs before JEI reads them as the event is sent before JEI starts */
  public static void onRecipesReceived(RecipesReceivedEvent event) {
    recipes = event.getRecipeMap();
  }

  /** Kiln recipes of the current server */
  public static Collection<RecipeHolder<KilnRecipe>> getRecipes() {
    Collection<RecipeHolder<KilnRecipe>> list = recipes.byType(Registration.KILN_RECIPE.get());
    return list == null ? List.of() : list;
  }
}

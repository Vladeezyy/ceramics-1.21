package knightminer.ceramics.plugin.jei;

import knightminer.ceramics.Ceramics;
import knightminer.ceramics.Registration;
import knightminer.ceramics.client.screen.KilnScreen;
import knightminer.ceramics.items.CrackableBlockItem;
import knightminer.ceramics.menu.KilnMenu;
import knightminer.ceramics.recipe.CeramicsTags;
import knightminer.ceramics.recipe.KilnRecipe;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.ItemLike;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

@SuppressWarnings("unused")
@JeiPlugin
public class JEIPlugin implements IModPlugin {
  @Override
  public net.minecraft.resources.ResourceLocation getPluginUid() {
    return Ceramics.getResource("jei_plugin");
  }

  /** Subtype interpreter from a function returning a key, or null for no subtype */
  private record Interpreter(Function<ItemStack, String> key) implements ISubtypeInterpreter<ItemStack> {
    @Nullable
    @Override
    public Object getSubtypeData(ItemStack stack, UidContext context) {
      String value = key.apply(stack);
      return value.isEmpty() ? null : value;
    }

    @Override
    public String getLegacyStringSubtypeInfo(ItemStack stack, UidContext context) {
      return key.apply(stack);
    }
  }

  @Override
  public void registerItemSubtypes(ISubtypeRegistration registration) {
    Interpreter fluidInterpreter = new Interpreter(stack -> BuiltInRegistries.FLUID.getKey(Registration.FLUID_CLAY_BUCKET.get().getFluid(stack)).toString());
    registration.registerSubtypeInterpreter(Registration.FLUID_CLAY_BUCKET.get(), fluidInterpreter);
    registration.registerSubtypeInterpreter(Registration.CRACKED_FLUID_CLAY_BUCKET.get(), fluidInterpreter);
    Interpreter blockInterpreter = new Interpreter(stack -> BuiltInRegistries.BLOCK.getKey(Registration.SOLID_CLAY_BUCKET.get().getBlock(stack)).toString());
    registration.registerSubtypeInterpreter(Registration.SOLID_CLAY_BUCKET.get(), blockInterpreter);
    registration.registerSubtypeInterpreter(Registration.CRACKED_SOLID_CLAY_BUCKET.get(), blockInterpreter);

    // separate different states of crackable clay
    Interpreter crackableClay = new Interpreter(stack -> CrackableBlockItem.getCracks(stack) > 0 ? "cracked" : "");
    registration.registerSubtypeInterpreter(Registration.TERRACOTTA_CISTERN.asItem(), crackableClay);
    Registration.COLORED_CISTERN.forEach(block -> registration.registerSubtypeInterpreter(block.asItem(), crackableClay));
    registration.registerSubtypeInterpreter(Registration.TERRACOTTA_FAUCET.asItem(), crackableClay);
    registration.registerSubtypeInterpreter(Registration.TERRACOTTA_CHANNEL.asItem(), crackableClay);
  }

  @Override
  public void registerCategories(IRecipeCategoryRegistration registration) {
    registration.addRecipeCategories(new KilnCategory(registration.getJeiHelpers().getGuiHelper()));
  }

  @Override
  public void registerRecipes(IRecipeRegistration registration) {
    ClientLevel world = Minecraft.getInstance().level;
    assert world != null;
    RecipeManager recipeManager = world.getRecipeManager();
    List<RecipeHolder<KilnRecipe>> results = new ArrayList<>();
    // kiln recipes are all marked special to keep the vanilla recipe book quiet, so unlike 1.8.0 do not skip special
    // recipes here: that hid every kiln recipe from JEI
    for (RecipeHolder<KilnRecipe> holder : recipeManager.getAllRecipesFor(Registration.KILN_RECIPE.get())) {
      KilnRecipe recipe = holder.value();
      // validate output
      if (recipe.getOutput().isEmpty()) {
        Ceramics.LOG.error("Invalid kiln recipe {}, no output", holder.id());
        continue;
      }
      if (recipe.getIngredients().size() != 1) {
        Ceramics.LOG.error("Invalid kiln recipe {}, wrong number of inputs", holder.id());
        continue;
      }
      // add to list
      results.add(holder);
    }
    registration.addRecipes(KilnCategory.TYPE, results);

    // clay repair info
    List<ItemStack> clayRepair = BuiltInRegistries.ITEM.getTag(CeramicsTags.Items.TERRACOTTA_CRACK_REPAIR).stream().flatMap(set -> set.stream()).map(ItemStack::new).toList();
    registration.addIngredientInfo(clayRepair, VanillaTypes.ITEM_STACK, Ceramics.component("jei", "clay_repair"));
  }

  @Override
  public void registerGuiHandlers(IGuiHandlerRegistration registration) {
    registration.addRecipeClickArea(KilnScreen.class, 78, 32, 28, 23, KilnCategory.TYPE, RecipeTypes.FUELING);
  }

  @Override
  public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
    registration.addRecipeTransferHandler(KilnMenu.class, Registration.KILN_MENU.get(), KilnCategory.TYPE, KilnCategory.INPUT_SLOT, 1, 3, 36);
    registration.addRecipeTransferHandler(KilnMenu.class, Registration.KILN_MENU.get(), RecipeTypes.FUELING, KilnCategory.FUEL_SLOT, 1, 3, 36);
  }

  @Override
  public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
    registration.addRecipeCatalyst(new ItemStack((ItemLike) Registration.KILN), KilnCategory.TYPE, RecipeTypes.FUELING);
  }

  @Override
  public void onRuntimeAvailable(IJeiRuntime runtime) {
    // add buckets to the ingredient list since JEI fills that list too soon
    // TODO: is this still needed?
    List<ItemStack> buckets = new ArrayList<>();
    Consumer<ItemStack> consumer = buckets::add;
    Registration.FLUID_CLAY_BUCKET.get().addVariants(consumer);
    Registration.CRACKED_FLUID_CLAY_BUCKET.get().addVariants(consumer);
    Registration.SOLID_CLAY_BUCKET.get().addVariants(consumer);
    Registration.CRACKED_SOLID_CLAY_BUCKET.get().addVariants(consumer);
    runtime.getIngredientManager().addIngredientsAtRuntime(VanillaTypes.ITEM_STACK, buckets);
  }
}

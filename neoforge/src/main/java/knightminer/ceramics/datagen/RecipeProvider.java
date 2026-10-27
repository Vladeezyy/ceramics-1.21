package knightminer.ceramics.datagen;

import net.minecraft.core.registries.BuiltInRegistries;
import knightminer.ceramics.Ceramics;
import knightminer.ceramics.Registration;
import knightminer.ceramics.blocks.RainbowPorcelain;
import knightminer.ceramics.recipe.CeramicsDatagen;
import knightminer.ceramics.recipe.CeramicsTags;
import knightminer.ceramics.recipe.CrackedClayRepairRecipe;
import knightminer.ceramics.recipe.NoNBTIngredient;
import net.minecraft.advancements.Criterion;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.core.HolderLookup;
import java.util.concurrent.CompletableFuture;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.data.recipes.SingleItemRecipeBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.Tags;
import dev.vladeezyy.kilnwork.registration.BuildingBlocks;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@SuppressWarnings("ConstantConditions")
public class RecipeProvider extends net.minecraft.data.recipes.RecipeProvider {

  /** Vanilla bricks as a building block object */
  private static final BuildingBlocks BRICKS = new BuildingBlocks(() -> Blocks.BRICKS, () -> Blocks.BRICK_SLAB, () -> Blocks.BRICK_STAIRS, () -> Blocks.BRICK_WALL);

  protected RecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
    super(registries, output);
  }

  /** Runs the provider, since 1.21.2 the provider itself is created per run with the registries */
  public static class Runner extends net.minecraft.data.recipes.RecipeProvider.Runner {
    public Runner(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> registries) {
      super(packOutput, registries);
    }

    @Override
    protected net.minecraft.data.recipes.RecipeProvider createRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
      return new RecipeProvider(registries, output);
    }

    @Override
    public String getName() {
      return "Ceramics Recipes";
    }
  }

  @Override
  protected void buildRecipes() {
    RecipeOutput consumer = this.output;
    // recoloring terracotta
    Criterion<?> terracottaCriteria = has(CeramicsTags.Items.COLORED_TERRACOTTA);
    Registration.TERRACOTTA.forEach((color, item) ->
      shaped(RecipeCategory.BUILDING_BLOCKS, item, 8)
                         .define('B', CeramicsTags.Items.COLORED_TERRACOTTA)
                         .define('D', color.getTag())
                         .group("stained_terracotta")
                         .pattern("BBB")
                         .pattern("BDB")
                         .pattern("BBB")
                         .unlockedBy("has_terracotta", terracottaCriteria)
                         .save(consumer, key(location(id(item).getPath() + "_recolor")))
    );

    // crafting porcelain
    shapeless(RecipeCategory.BUILDING_BLOCKS, Registration.UNFIRED_PORCELAIN, 4)
                          .requires(Tags.Items.GEMS_QUARTZ)
                          .requires(Items.CLAY_BALL)
                          .requires(Items.CLAY_BALL)
                          .requires(Items.CLAY_BALL)
                          .unlockedBy("has_quartz", has(Tags.Items.GEMS_QUARTZ))
                          .save(consumer);

    // unfired porcelain
    shaped(RecipeCategory.BUILDING_BLOCKS, Registration.UNFIRED_PORCELAIN_BLOCK)
                       .define('b', Registration.UNFIRED_PORCELAIN)
                       .pattern("bb")
                       .pattern("bb")
                       .unlockedBy("has_item", has(Registration.UNFIRED_PORCELAIN))
                       .save(consumer);
    // smelting porcelain
    kilnFurnaceRecipe(consumer, Registration.UNFIRED_PORCELAIN_BLOCK, RecipeCategory.BUILDING_BLOCKS, Registration.PORCELAIN_BLOCK.get(DyeColor.WHITE), 0.3f);

    // colored porcelain
    Criterion<?> porcelainCriteria = has(Registration.PORCELAIN_BLOCK.get(DyeColor.WHITE));
    Registration.PORCELAIN_BLOCK.forEach((color, item) ->
      shaped(RecipeCategory.BUILDING_BLOCKS, item, 8)
                         .define('B', CeramicsTags.Items.PORCELAIN)
                         .define('D', color.getTag())
                         .group(locationString("dye_porcelain"))
                         .pattern("BBB")
                         .pattern("BDB")
                         .pattern("BBB")
                         .unlockedBy("has_porcelain", porcelainCriteria)
                         .save(consumer)
    );
    // rainbow porcelain
    kilnFurnaceRecipe(consumer, CeramicsTags.Items.COLORED_PORCELAIN, RecipeCategory.BUILDING_BLOCKS, Registration.RAINBOW_PORCELAIN.get(RainbowPorcelain.RED), 0.1f, location("rainbow_porcelain"));

    // smelt for full rainbow
    Criterion<?> hasTheRainbow = has(CeramicsTags.Items.RAINBOW_PORCELAIN);
    Registration.RAINBOW_PORCELAIN.forEach((color, item) ->
      SingleItemRecipeBuilder.stonecutting(tag(CeramicsTags.Items.RAINBOW_PORCELAIN), RecipeCategory.BUILDING_BLOCKS, item, 1)
                             .unlockedBy("has_the_rainbow", hasTheRainbow)
                             .save(consumer, key(id(item)))
    );

    // bricks
    // vanilla brick block shortcuts
    Criterion<?> hasClayBrick = has(Items.BRICK);
    shaped(RecipeCategory.BUILDING_BLOCKS, Items.BRICK_SLAB)
                       .define('b', Items.BRICK)
                       .pattern("bb")
                       .unlockedBy("has_bricks", hasClayBrick)
                       .group(path(Items.BRICK_SLAB))
                       .save(consumer, key(location("brick_slab_from_bricks")));
    // stairs shortcut
    shaped(RecipeCategory.BUILDING_BLOCKS, Items.BRICK_STAIRS)
                       .define('b', Items.BRICK)
                       .pattern("b  ")
                       .pattern("bb ")
                       .pattern("bbb")
                       .unlockedBy("has_bricks", hasClayBrick)
                       .group(path(Items.BRICK_STAIRS))
                       .save(consumer, key(location("brick_stairs_from_bricks")));
    // block from slab
    shaped(RecipeCategory.BUILDING_BLOCKS, Items.BRICKS)
                       .define('B', Items.BRICK_SLAB)
                       .pattern("B")
                       .pattern("B")
                       .unlockedBy("has_item", has(Items.BRICK_SLAB))
                       .group(path(Items.BRICKS))
                       .save(consumer, key(location("bricks_from_slab")));

    // dark bricks from smelting bricks
    eachBuilding(BRICKS, Registration.DARK_BRICKS, (input, output) ->
      kilnFurnaceRecipe(consumer, input, RecipeCategory.BUILDING_BLOCKS, output, 0.1f)
    );
    registerSlabStairWall(consumer, Registration.DARK_BRICKS);

    // magma bricks from lava bucket
    addBrickRecipe(consumer, BRICKS, Items.LAVA_BUCKET, Registration.LAVA_BRICKS, "lava");
    registerSlabStairWall(consumer, Registration.LAVA_BRICKS);

    // dragon bricks from dragon's breath
    addBrickRecipe(consumer, BRICKS, Items.DRAGON_BREATH, Registration.DRAGON_BRICKS, "dragon");
    registerSlabStairWall(consumer, Registration.DRAGON_BRICKS);

    // porcelain bricks
    kilnFurnaceRecipe(consumer, Registration.UNFIRED_PORCELAIN, RecipeCategory.MISC, Registration.PORCELAIN_BRICK, 0.3f);
    // using bricks
    Criterion<?> hasBricks = has(Registration.PORCELAIN_BRICK);
    shaped(RecipeCategory.BUILDING_BLOCKS, Registration.PORCELAIN_BRICKS)
                       .define('b', Registration.PORCELAIN_BRICK)
                       .pattern("bb")
                       .pattern("bb")
                       .unlockedBy("has_bricks", hasBricks)
                       .group(Registration.PORCELAIN_BRICK.getId().toString())
                       .save(consumer);
    // slab shortcut
    ItemLike porcelainSlab = Registration.PORCELAIN_BRICKS.getSlab();
    shaped(RecipeCategory.BUILDING_BLOCKS, porcelainSlab)
                       .define('b', Registration.PORCELAIN_BRICK)
                       .pattern("bb")
                       .unlockedBy("has_bricks", hasBricks)
                       .group(id(porcelainSlab).toString())
                       .save(consumer, key(suffix(porcelainSlab, "_from_bricks")));
    // stairs shortcut
    ItemLike porcelainStairs = Registration.PORCELAIN_BRICKS.getStairs();
    shaped(RecipeCategory.BUILDING_BLOCKS, porcelainStairs)
                       .define('b', Registration.PORCELAIN_BRICK)
                       .pattern("b  ")
                       .pattern("bb ")
                       .pattern("bbb")
                       .unlockedBy("has_bricks", hasBricks)
                       .group(id(porcelainStairs).toString())
                       .save(consumer, key(suffix(porcelainStairs, "_from_bricks")));
    registerSlabStairWall(consumer, Registration.PORCELAIN_BRICKS);

    // golden bricks
    addBrickRecipe(consumer, Registration.PORCELAIN_BRICKS, Items.GOLD_INGOT, Registration.GOLDEN_BRICKS, "gold");
    registerSlabStairWall(consumer, Registration.GOLDEN_BRICKS);

    // marine bricks
    addBrickRecipe(consumer, Registration.PORCELAIN_BRICKS, Items.PRISMARINE_SHARD, Registration.MARINE_BRICKS, "prismarine");
    registerSlabStairWall(consumer, Registration.MARINE_BRICKS);

    // monochrome uses ink, not black dye intentionally
    addBrickRecipe(consumer, Registration.PORCELAIN_BRICKS, Items.INK_SAC, Registration.MONOCHROME_BRICKS, "ink");
    registerSlabStairWall(consumer, Registration.MONOCHROME_BRICKS);

    // rainbow
    eachBuilding(Registration.PORCELAIN_BRICKS, Registration.RAINBOW_BRICKS, (input, output) ->
      kilnFurnaceRecipe(consumer, input, RecipeCategory.BUILDING_BLOCKS, output, 0.1f)
    );
    registerSlabStairWall(consumer, Registration.RAINBOW_BRICKS);

    // buckets
    // unfired
    shaped(RecipeCategory.DECORATIONS, Registration.UNFIRED_CLAY_BUCKET)
                       .define('c', Items.CLAY_BALL)
                       .pattern("c c")
                       .pattern(" c ")
                       .unlockedBy("has_clay", has(Items.CLAY_BALL))
                       .save(consumer);
    // fired
    kilnFurnaceRecipe(consumer, Registration.UNFIRED_CLAY_BUCKET, RecipeCategory.TOOLS, Registration.EMPTY_CLAY_BUCKET, 0.3f);
    // cracked
    kilnFurnaceRecipe(consumer, Registration.EMPTY_CLAY_BUCKET, RecipeCategory.TOOLS, Registration.CRACKED_EMPTY_CLAY_BUCKET, 0.2f);

    // cistern
    shaped(RecipeCategory.DECORATIONS, Registration.CLAY_CISTERN, 2)
                       .define('c', Items.CLAY_BALL)
                       .pattern("c c")
                       .pattern("c c")
                       .pattern("c c")
                       .unlockedBy("has_clay", has(Items.CLAY_BALL))
                       .save(consumer);
    shaped(RecipeCategory.DECORATIONS, Registration.UNFIRED_CISTERN, 2)
                       .define('c', Registration.UNFIRED_PORCELAIN)
                       .pattern("c c")
                       .pattern("c c")
                       .pattern("c c")
                       .unlockedBy("has_clay", has(Registration.UNFIRED_PORCELAIN))
                       .save(consumer);
    // fired
    kilnFurnaceRecipe(consumer, Registration.CLAY_CISTERN, RecipeCategory.REDSTONE, Registration.TERRACOTTA_CISTERN, 0.3f);
    kilnFurnaceRecipe(consumer, Registration.UNFIRED_CISTERN, RecipeCategory.REDSTONE, Registration.PORCELAIN_CISTERN.get(DyeColor.WHITE), 0.3f);
    // repair
    clayRepair(consumer, Registration.TERRACOTTA_CISTERN);
    // colored
    Registration.COLORED_CISTERN.forEach((color, block) -> {
      // craft
      shaped(RecipeCategory.REDSTONE, block, 4)
                         .define('c', NoNBTIngredient.of(tag(CeramicsTags.Items.TERRACOTTA_CISTERNS)))
                         .define('d', color.getTag())
                         .pattern(" c ")
                         .pattern("cdc")
                         .pattern(" c ")
                         .unlockedBy("has_cistern", has(Registration.TERRACOTTA_CISTERN))
                         .group(Ceramics.locationName("colored_cisterns"))
                         .save(consumer);
      // repair
      clayRepair(consumer, block);
    });
    Registration.PORCELAIN_CISTERN.forEach((color, block) ->
      shaped(RecipeCategory.REDSTONE, block, 4)
                         .define('c', CeramicsTags.Items.PORCELAIN_CISTERNS)
                         .define('d', color.getTag())
                         .pattern(" c ")
                         .pattern("cdc")
                         .pattern(" c ")
                         .unlockedBy("has_cistern", has(Registration.PORCELAIN_CISTERN.get(DyeColor.WHITE)))
                         .group(Ceramics.locationName("porcelain_cisterns"))
                         .save(consumer)
    );

    // gauge
    shaped(RecipeCategory.REDSTONE, Registration.TERRACOTTA_GAUGE, 4)
                       .define('b', Items.BRICK)
                       .define('p', Tags.Items.GLASS_PANES_COLORLESS)
                       .pattern(" b ")
                       .pattern("bpb")
                       .pattern(" b ")
                       .unlockedBy("has_cistern", has(Registration.TERRACOTTA_CISTERN))
                       .save(consumer);
    shaped(RecipeCategory.REDSTONE, Registration.PORCELAIN_GAUGE, 4)
                       .define('b', Registration.PORCELAIN_BRICK)
                       .define('p', Tags.Items.GLASS_PANES_COLORLESS)
                       .pattern(" b ")
                       .pattern("bpb")
                       .pattern(" b ")
                       .unlockedBy("has_cistern", has(Registration.PORCELAIN_CISTERN.get(DyeColor.WHITE)))
                       .save(consumer);


    // faucet
    shaped(RecipeCategory.DECORATIONS, Registration.CLAY_FAUCET, 2)
                       .define('c', Items.CLAY_BALL)
                       .pattern("ccc")
                       .pattern(" c ")
                       .unlockedBy("has_cistern", has(Items.CLAY_BALL))
                       .save(consumer);
    shaped(RecipeCategory.DECORATIONS, Registration.UNFIRED_FAUCET, 2)
                       .define('c', Registration.UNFIRED_PORCELAIN)
                       .pattern("ccc")
                       .pattern(" c ")
                       .unlockedBy("has_cistern", has(Registration.UNFIRED_PORCELAIN))
                       .save(consumer);
    kilnFurnaceRecipe(consumer, Registration.CLAY_FAUCET, RecipeCategory.REDSTONE, Registration.TERRACOTTA_FAUCET, 0.3f);
    kilnFurnaceRecipe(consumer, Registration.UNFIRED_FAUCET, RecipeCategory.REDSTONE, Registration.PORCELAIN_FAUCET, 0.3f);
    clayRepair(consumer, Registration.TERRACOTTA_FAUCET);

    // channel
    shaped(RecipeCategory.DECORATIONS, Registration.CLAY_CHANNEL, 3)
                       .define('p', Items.CLAY_BALL)
                       .pattern("ppp")
                       .pattern("ppp")
                       .unlockedBy("has_cistern", has(Items.CLAY_BALL))
                       .save(consumer);
    shaped(RecipeCategory.DECORATIONS, Registration.UNFIRED_CHANNEL, 3)
                       .define('p', Registration.UNFIRED_PORCELAIN)
                       .pattern("ppp")
                       .pattern("ppp")
                       .unlockedBy("has_cistern", has(Registration.UNFIRED_PORCELAIN))
                       .save(consumer);
    kilnFurnaceRecipe(consumer, Registration.CLAY_CHANNEL, RecipeCategory.REDSTONE, Registration.TERRACOTTA_CHANNEL, 0.3f);
    kilnFurnaceRecipe(consumer, Registration.UNFIRED_CHANNEL, RecipeCategory.REDSTONE, Registration.PORCELAIN_CHANNEL, 0.3f);
    clayRepair(consumer, Registration.TERRACOTTA_CHANNEL);

    // armor
    // clay plates
    shaped(RecipeCategory.MISC, Registration.UNFIRED_CLAY_PLATE, 2)
                       .define('c', Items.CLAY_BALL)
                       .pattern("cc")
                       .unlockedBy("has_clay", has(Items.CLAY_BALL))
                       .save(consumer);
    kilnFurnaceRecipe(consumer, Registration.UNFIRED_CLAY_PLATE, RecipeCategory.MISC, Registration.CLAY_PLATE, 0.3f);

    // helmet
    Criterion<?> hasClayPlate = has(CeramicsTags.Items.BRICK_PLATES);
    shaped(RecipeCategory.COMBAT, Registration.CLAY_HELMET)
                       .define('c', CeramicsTags.Items.BRICK_PLATES)
                       .pattern("ccc")
                       .pattern("c c")
                       .unlockedBy("has_plate", hasClayPlate)
                       .save(consumer);
    // chestplate
    shaped(RecipeCategory.COMBAT, Registration.CLAY_CHESTPLATE)
                       .define('c', CeramicsTags.Items.BRICK_PLATES)
                       .pattern("c c")
                       .pattern("ccc")
                       .pattern("ccc")
                       .unlockedBy("has_plate", hasClayPlate)
                       .save(consumer);
    // leggings
    shaped(RecipeCategory.COMBAT, Registration.CLAY_LEGGINGS)
                       .define('c', CeramicsTags.Items.BRICK_PLATES)
                       .pattern("ccc")
                       .pattern("c c")
                       .pattern("c c")
                       .unlockedBy("has_plate", hasClayPlate)
                       .save(consumer);
    // boots
    shaped(RecipeCategory.COMBAT, Registration.CLAY_BOOTS)
                       .define('c', CeramicsTags.Items.BRICK_PLATES)
                       .pattern("c c")
                       .pattern("c c")
                       .unlockedBy("has_plate", hasClayPlate)
                       .save(consumer);

    // clay uncrafting
    shapeless(RecipeCategory.MISC, Items.CLAY_BALL, 4)
                          .requires(Items.CLAY)
                          .unlockedBy("has_unfired", has(Items.CLAY))
                          .group(locationString("clay_uncrafting"))
                          .save(consumer, key(location("clay_uncrafting_4")));
    shapeless(RecipeCategory.MISC, Items.CLAY_BALL, 3)
                          .requires(Ingredient.of(Registration.UNFIRED_CLAY_BUCKET, Registration.CLAY_CISTERN))
                          .unlockedBy("has_unfired", has(Registration.UNFIRED_CLAY_BUCKET))
                          .group(locationString("clay_uncrafting"))
                          .save(consumer, key(location("clay_uncrafting_3")));
    shapeless(RecipeCategory.MISC, Items.CLAY_BALL, 2)
                          .requires(Ingredient.of(Registration.CLAY_FAUCET, Registration.CLAY_CHANNEL))
                          .unlockedBy("has_unfired", has(Registration.CLAY_FAUCET))
                          .group(locationString("clay_uncrafting"))
                          .save(consumer, key(location("clay_uncrafting_2")));
    shapeless(RecipeCategory.MISC, Items.CLAY_BALL, 1)
                          .requires(Ingredient.of(Registration.UNFIRED_CLAY_PLATE))
                          .unlockedBy("has_unfired", has(Registration.UNFIRED_CLAY_PLATE))
                          .group(locationString("clay_uncrafting"))
                          .save(consumer, key(location("clay_uncrafting")));
    // porcelain uncrafting
    shapeless(RecipeCategory.MISC, Registration.UNFIRED_PORCELAIN, 4)
                          .requires(Registration.UNFIRED_PORCELAIN_BLOCK)
                          .unlockedBy("has_unfired", has(Registration.UNFIRED_PORCELAIN_BLOCK))
                          .group(locationString("porcelain_uncrafting"))
                          .save(consumer, key(location("porcelain_uncrafting_4")));
    shapeless(RecipeCategory.MISC, Registration.UNFIRED_PORCELAIN, 2)
                          .requires(Ingredient.of(Registration.UNFIRED_FAUCET, Registration.UNFIRED_CHANNEL))
                          .unlockedBy("has_unfired", has(Registration.UNFIRED_FAUCET))
                          .group(locationString("porcelain_uncrafting"))
                          .save(consumer, key(location("porcelain_uncrafting_2")));
    shapeless(RecipeCategory.MISC, Registration.UNFIRED_PORCELAIN, 3)
                          .requires(Registration.UNFIRED_CISTERN)
                          .unlockedBy("has_unfired", has(Registration.UNFIRED_CISTERN))
                          .group(locationString("porcelain_uncrafting"))
                          .save(consumer, key(location("porcelain_uncrafting_3")));
    // compat, wish there was a better way to do this
    shaped(RecipeCategory.FOOD, Blocks.CAKE)
                       .define('M', CeramicsTags.Items.MILK_BUCKETS)
                       .define('S', Items.SUGAR)
                       .define('W', Items.WHEAT)
                       .define('E', Items.EGG)
                       .pattern("MMM")
                       .pattern("SES")
                       .pattern("WWW")
                       .unlockedBy("has_egg", has(Items.EGG))
                       .save(consumer, key(location("cake")));

    // kiln - crafting
    shaped(RecipeCategory.DECORATIONS, Registration.KILN)
                       .define('B', Items.BRICK)
                       .define('F', Items.FURNACE)
                       .define('H', Items.TERRACOTTA)
                       .pattern("BBB")
                       .pattern("BFB")
                       .pattern("HHH")
                       .unlockedBy("has_brick", has(Items.BRICK))
                       .save(consumer);

    // add vanilla furnace recipes to the kiln
    // clay
    kilnRecipe(consumer, Items.CLAY_BALL, RecipeCategory.MISC, Items.BRICK, 0.3f);
    kilnRecipe(consumer, Blocks.CLAY, RecipeCategory.BUILDING_BLOCKS, Blocks.TERRACOTTA, 0.3f);
    Registration.TERRACOTTA.forEach((color, input) ->
      kilnRecipe(consumer, input, RecipeCategory.BUILDING_BLOCKS, GLAZED_TERRACOTTA.get(color), 0.1f)
    );
    // sand and glass
    kilnRecipe(consumer, ItemTags.SAND, RecipeCategory.BUILDING_BLOCKS, Blocks.GLASS, 0.1f);
    kilnRecipe(consumer, Blocks.SANDSTONE, RecipeCategory.BUILDING_BLOCKS, Blocks.SMOOTH_SANDSTONE, 0.1f);
    kilnRecipe(consumer, Blocks.RED_SANDSTONE, RecipeCategory.BUILDING_BLOCKS, Blocks.SMOOTH_RED_SANDSTONE, 0.1f);
    // rock
    kilnRecipe(consumer, Blocks.COBBLESTONE, RecipeCategory.BUILDING_BLOCKS, Blocks.STONE, 0.1f);
    kilnRecipe(consumer, Blocks.STONE, RecipeCategory.BUILDING_BLOCKS, Blocks.SMOOTH_STONE, 0.1f);
    kilnRecipe(consumer, Blocks.STONE_BRICKS, RecipeCategory.BUILDING_BLOCKS, Blocks.CRACKED_STONE_BRICKS, 0.1f);
    kilnRecipe(consumer, Blocks.NETHERRACK, RecipeCategory.BUILDING_BLOCKS, Items.NETHER_BRICK, 0.1f);
    kilnRecipe(consumer, Blocks.QUARTZ_BLOCK, RecipeCategory.BUILDING_BLOCKS, Blocks.SMOOTH_QUARTZ, 0.1f);

    // add recipes to smelt slabs and stairs directly for relevant blocks
    // sand
    kilnFurnaceRecipe(consumer, Blocks.SANDSTONE_SLAB, RecipeCategory.BUILDING_BLOCKS, Blocks.SMOOTH_SANDSTONE_SLAB, 0.1f);
    kilnFurnaceRecipe(consumer, Blocks.SANDSTONE_STAIRS, RecipeCategory.BUILDING_BLOCKS, Blocks.SMOOTH_SANDSTONE_STAIRS, 0.1f);
    kilnFurnaceRecipe(consumer, Blocks.RED_SANDSTONE_SLAB, RecipeCategory.BUILDING_BLOCKS, Blocks.SMOOTH_RED_SANDSTONE_SLAB, 0.1f);
    kilnFurnaceRecipe(consumer, Blocks.RED_SANDSTONE_STAIRS, RecipeCategory.BUILDING_BLOCKS, Blocks.SMOOTH_RED_SANDSTONE_STAIRS, 0.1f);
    // rock
    kilnFurnaceRecipe(consumer, Blocks.COBBLESTONE_SLAB, RecipeCategory.BUILDING_BLOCKS, Blocks.STONE_SLAB, 0.1f);
    kilnFurnaceRecipe(consumer, Blocks.COBBLESTONE_STAIRS, RecipeCategory.BUILDING_BLOCKS, Blocks.STONE_STAIRS, 0.1f);
    kilnFurnaceRecipe(consumer, Blocks.STONE_SLAB, RecipeCategory.BUILDING_BLOCKS, Blocks.SMOOTH_STONE_SLAB, 0.1f);
    kilnFurnaceRecipe(consumer, Blocks.QUARTZ_STAIRS, RecipeCategory.BUILDING_BLOCKS, Blocks.SMOOTH_QUARTZ_STAIRS, 0.1f);
    kilnFurnaceRecipe(consumer, Blocks.BASALT, RecipeCategory.BUILDING_BLOCKS, Blocks.SMOOTH_BASALT, 0.1f);
  }


  /* Location helpers */

  /**
   * Gets a resource location for Ceramics
   * @param id  Location path
   * @return  Location for Ceramics
   */
  /** Recipe key at the given location */
  private static ResourceKey<Recipe<?>> key(Identifier location) {
    return ResourceKey.create(Registries.RECIPE, location);
  }

  private static Identifier location(String id) {
    return Ceramics.getResource(id);
  }

  /** Gets the ID of an item */
  private static Identifier id(ItemLike item) {
    return BuiltInRegistries.ITEM.getKey(item.asItem());
  }

  /** Gets the ID of an item as a Ceramics location */
  private static Identifier location(ItemLike item) {
    return location(path(item));
  }

  /** Gets the ID of an item */
  private static String path(ItemLike item) {
    return id(item).getPath();
  }

  /**
   * Gets a resource location as a string for Ceramics
   * @param id  Location path
   * @return  String location for Ceramics
   */
  private static String locationString(String id) {
    return Ceramics.MOD_ID + ":" + id;
  }

  /**
   * Suffixes the resource location path with the given value
   * @param loc     Location to suffix
   * @param suffix  Suffix value
   * @return  Resource location path
   */
  private static Identifier suffix(Identifier loc, String suffix) {
    return location(loc.getPath() + suffix);
  }

  /**
   * Suffixes the item's resource location with the given value
   * @param item    Item to suffix
   * @param suffix  Suffix value
   * @return  Resource location path
   */
  private static Identifier suffix(ItemLike item, String suffix) {
    return suffix(id(item.asItem()), suffix);
  }


  /* Enum blocks */
  /** Map of color to glazed terracotta types */
  private static final Map<DyeColor,Block> GLAZED_TERRACOTTA;
  static {
    EnumMap<DyeColor,Block> map = new EnumMap<>(DyeColor.class);
    map.put(DyeColor.WHITE,      Blocks.WHITE_GLAZED_TERRACOTTA);
    map.put(DyeColor.ORANGE,     Blocks.ORANGE_GLAZED_TERRACOTTA);
    map.put(DyeColor.MAGENTA,    Blocks.MAGENTA_GLAZED_TERRACOTTA);
    map.put(DyeColor.LIGHT_BLUE, Blocks.LIGHT_BLUE_GLAZED_TERRACOTTA);
    map.put(DyeColor.YELLOW,     Blocks.YELLOW_GLAZED_TERRACOTTA);
    map.put(DyeColor.LIME,       Blocks.LIME_GLAZED_TERRACOTTA);
    map.put(DyeColor.PINK,       Blocks.PINK_GLAZED_TERRACOTTA);
    map.put(DyeColor.GRAY,       Blocks.GRAY_GLAZED_TERRACOTTA);
    map.put(DyeColor.LIGHT_GRAY, Blocks.LIGHT_GRAY_GLAZED_TERRACOTTA);
    map.put(DyeColor.CYAN,       Blocks.CYAN_GLAZED_TERRACOTTA);
    map.put(DyeColor.PURPLE,     Blocks.PURPLE_GLAZED_TERRACOTTA);
    map.put(DyeColor.BLUE,       Blocks.BLUE_GLAZED_TERRACOTTA);
    map.put(DyeColor.BROWN,      Blocks.BROWN_GLAZED_TERRACOTTA);
    map.put(DyeColor.GREEN,      Blocks.GREEN_GLAZED_TERRACOTTA);
    map.put(DyeColor.RED,        Blocks.RED_GLAZED_TERRACOTTA);
    map.put(DyeColor.BLACK,      Blocks.BLACK_GLAZED_TERRACOTTA);
    GLAZED_TERRACOTTA = map;
  }


  /* Iteration helpers */

  /**
   * Runs the consumer once for each of block, slab, stairs, and wall
   * @param input      Recipe input for consumer
   * @param output     Recipe output for consumer
   * @param consumer   Consumer to create recipes
   */
  private void eachBuilding(BuildingBlocks input, BuildingBlocks output, BiConsumer<ItemLike,ItemLike> consumer) {
    consumer.accept(input.asItem(), output.asItem());
    consumer.accept(input.getSlab(), output.getSlab());
    consumer.accept(input.getStairs(), output.getStairs());
    consumer.accept(input.getWall(), output.getWall());
  }

  /**
   * Registers generic building block recipes
   * @param consumer  Recipe consumer
   * @param building  Building object instance
   */
  private void registerSlabStairWall(RecipeOutput consumer, BuildingBlocks building) {
    Item item = building.asItem();
    Identifier location = id(item);
    Criterion<?> hasBuilding = has(item);
    Ingredient ingredient = Ingredient.of(item);

    // slab
    ItemLike slab = building.getSlab();
    shaped(RecipeCategory.BUILDING_BLOCKS, slab, 6)
        .define('B', item)
        .pattern("BBB")
        .unlockedBy("has_item", hasBuilding)
        .group(id(slab.asItem()).toString())
        .save(consumer, key(suffix(location, "_slab_crafting")));
    SingleItemRecipeBuilder.stonecutting(ingredient, RecipeCategory.BUILDING_BLOCKS, slab, 2)
                           .unlockedBy("has_item", hasBuilding)
                           .save(consumer, key(suffix(location, "_slab_stonecutter")));

    // stairs
    ItemLike stairs = building.getStairs();
    shaped(RecipeCategory.BUILDING_BLOCKS, stairs, 4)
        .define('B', item)
        .pattern("B  ")
        .pattern("BB ")
        .pattern("BBB")
        .unlockedBy("has_item", hasBuilding)
        .group(id(stairs).toString())
        .save(consumer, key(suffix(location, "_stairs_crafting")));
    SingleItemRecipeBuilder.stonecutting(ingredient, RecipeCategory.BUILDING_BLOCKS, stairs, 1)
                           .unlockedBy("has_item", hasBuilding)
                           .save(consumer, key(suffix(location, "_stairs_stonecutter")));

    // wall
    ItemLike wall = building.getWall();
    shaped(RecipeCategory.BUILDING_BLOCKS, wall, 6)
        .define('B', item)
        .pattern("BBB")
        .pattern("BBB")
        .unlockedBy("has_item", hasBuilding)
        .save(consumer, key(suffix(location, "_wall_crafting")));
    SingleItemRecipeBuilder.stonecutting(ingredient, RecipeCategory.BUILDING_BLOCKS, wall, 1)
                           .unlockedBy("has_item", hasBuilding)
                           .save(consumer, key(suffix(location, "_wall_stonecutter")));

    // block from slab, its bricks so its easy
    shaped(RecipeCategory.BUILDING_BLOCKS, item)
                       .define('B', slab)
                       .pattern("B")
                       .pattern("B")
                       .unlockedBy("has_item", has(slab))
                       .group(location.toString())
                       .save(consumer, key(suffix(location, "_from_slab")));
  }

  /**
   * Add recipes for surrouding an ingredient with a brick to get another brick
   * @param consumer    Recipe consumer
   * @param from        Input bricks
   * @param ingredient  Ingredient to transform
   * @param to          Output bricks
   * @param name        Recipe name
   */
  private void addBrickRecipe(RecipeOutput consumer, BuildingBlocks from, Item ingredient, BuildingBlocks to, String name) {
    Criterion<?> criteria = has(ingredient);
    eachBuilding(from, to, (input, output) ->
      shaped(RecipeCategory.BUILDING_BLOCKS, output, 8)
                         .define('B', input)
                         .define('i', ingredient)
                         .pattern("BBB")
                         .pattern("BiB")
                         .pattern("BBB")
                         .unlockedBy("has_" + name, criteria)
                         .group(id(output).toString())
                         .save(consumer, key(suffix(output, "_" + name)))
    );
  }

  /* Kiln recipes */

  /**
   * Shortcut to add a kiln recipe
   * @param consumer    Recipe consumer
   * @param input       Recipe input
   * @param category    Recipe category
   * @param criteria    Criteria to unlock the recipe
   * @param output      Recipe output
   * @param experience  Experience earned
   * @param name        Recipe name
   */
  private void kilnRecipe(RecipeOutput consumer, Ingredient input, Criterion<?> criteria, RecipeCategory category, ItemLike output, float experience, Identifier name) {
    CeramicsDatagen.kilnRecipe(input, category, output, experience, 100)
        .unlockedBy("has_item", criteria)
        .save(consumer, key(name));
  }

  /**
   * Adds a kiln recipe for vanilla item
   * @param consumer    Recipe consumer
   * @param input       Recipe input item
   * @param category    Recipe category
   * @param output      Recipe output
   * @param experience  Experience earned
   */
  private void kilnRecipe(RecipeOutput consumer, ItemLike input, RecipeCategory category, ItemLike output, float experience) {
    kilnRecipe(consumer, Ingredient.of(input), has(input), category, output, experience, suffix(output, "_kiln"));
  }

  /**
   * Adds a kiln recipe for vanilla item
   * @param consumer    Recipe consumer
   * @param input       Recipe input tag
   * @param category    Recipe category
   * @param output      Recipe output
   * @param experience  Experience earned
   */
  private void kilnRecipe(RecipeOutput consumer, TagKey<Item> input, RecipeCategory category, ItemLike output, float experience) {
    kilnRecipe(consumer, tag(input), has(input), category, output, experience, suffix(output, "_kiln"));
  }

  /**
   * Adds a new recipe to both the kiln and the furnace
   * @param consumer    Recipe consumer
   * @param input       Recipe input
   * @param category    Recipe category
   * @param criteria    Criteria to unlock the recipe
   * @param output      Recipe output
   * @param experience  Experience earned
   * @param name        Recipe name
   */
  private void kilnFurnaceRecipe(RecipeOutput consumer, Ingredient input, Criterion<?> criteria, RecipeCategory category, ItemLike output, float experience, Identifier name) {
    SimpleCookingRecipeBuilder.smelting(input, category, CeramicsDatagen.bookCategory(output), output, experience, 200)
                        .unlockedBy("has_item", criteria)
                        .save(consumer, key(suffix(name, "_smelting")));
    kilnRecipe(consumer, input, criteria, category, output, experience, suffix(name, "_kiln"));
  }

  /**
   * Adds a new recipe to both the kiln and the furnace using the output's registry name to name the recipe
   * @param consumer    Recipe consumer
   * @param input       Recipe item input
   * @param category    Recipe category
   * @param output      Recipe output
   * @param experience  Experience earned
   * @param name        Recipe name
   */
  private void kilnFurnaceRecipe(RecipeOutput consumer, ItemLike input, RecipeCategory category, ItemLike output, float experience, Identifier name) {
    kilnFurnaceRecipe(consumer, Ingredient.of(input), has(input), category, output, experience, name);
  }

  /**
   * Adds a new recipe to both the kiln and the furnace using the output's registry name to name the recipe
   * @param consumer    Recipe consumer
   * @param input       Recipe item input
   * @param category    Recipe category
   * @param output      Recipe output
   * @param experience  Experience earned
   */
  private void kilnFurnaceRecipe(RecipeOutput consumer, ItemLike input, RecipeCategory category, ItemLike output, float experience) {
    kilnFurnaceRecipe(consumer, input, category, output, experience, location(output));
  }

  /**
   * Adds a new recipe to both the kiln and the furnace using the output's registry name to name the recipe
   * @param consumer    Recipe consumer
   * @param input       Recipe tag input
   * @param category    Recipe category
   * @param output      Recipe output
   * @param experience  Experience earned
   * @param name        Recipe name
   */
  private void kilnFurnaceRecipe(RecipeOutput consumer, TagKey<Item> input, RecipeCategory category, ItemLike output, float experience, Identifier name) {
    kilnFurnaceRecipe(consumer, tag(input), has(input), category, output, experience, name);
  }

  /**
   * Adds a recipe to repair a terracotta item's cracks
   * @param consumer    Recipe consumer
   * @param repairable  Repairable item
   */
  private void clayRepair(RecipeOutput consumer, ItemLike repairable) {
    CrackedClayRepairRecipe.save(consumer, suffix(repairable, "_repair"), repairable, tag(CeramicsTags.Items.TERRACOTTA_CRACK_REPAIR), has(repairable));
  }
}

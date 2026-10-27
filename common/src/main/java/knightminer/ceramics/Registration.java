package knightminer.ceramics;

import knightminer.ceramics.recipe.CeramicsTags;
import dev.vladeezyy.kilnwork.platform.KilnworkPlatform;
import dev.vladeezyy.kilnwork.platform.LoaderHooks;
import dev.vladeezyy.kilnwork.tag.TagPreference;
import knightminer.ceramics.platform.CeramicsPlatform;
import knightminer.ceramics.util.ConstantFluidContainerWrapper;
import knightminer.ceramics.util.FluidClayBucketWrapper;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import com.mojang.serialization.Codec;
import knightminer.ceramics.blocks.ChannelBlock;
import knightminer.ceramics.blocks.CisternBlock;
import knightminer.ceramics.blocks.FaucetBlock;
import knightminer.ceramics.blocks.FlowingChannelBlock;
import knightminer.ceramics.blocks.FluidCisternBlock;
import knightminer.ceramics.blocks.KilnBlock;
import knightminer.ceramics.blocks.PouringFaucetBlock;
import knightminer.ceramics.blocks.RainbowPorcelain;
import knightminer.ceramics.blocks.entity.ChannelBlockEntity;
import knightminer.ceramics.blocks.entity.CisternBlockEntity;
import knightminer.ceramics.blocks.entity.FaucetBlockEntity;
import knightminer.ceramics.blocks.entity.KilnBlockEntity;
import knightminer.ceramics.items.ArmorMaterials;
import knightminer.ceramics.items.BaseClayBucketItem;
import knightminer.ceramics.items.CrackableBlockItem;
import knightminer.ceramics.items.EmptyClayBucketItem;
import knightminer.ceramics.items.FixedTooltipBlockItem;
import knightminer.ceramics.items.FluidClayBucketItem;
import knightminer.ceramics.items.HeatResistantBlockItem;
import knightminer.ceramics.items.MilkClayBucketItem;
import knightminer.ceramics.items.SolidClayBucketItem;
import knightminer.ceramics.menu.KilnMenu;
import knightminer.ceramics.recipe.CrackedClayRepairRecipe;
import knightminer.ceramics.recipe.KilnRecipe;
import knightminer.ceramics.util.EmptyFluidBucketCauldronInteraction;
import knightminer.ceramics.util.EmptySolidBucketCauldronInteraction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.core.cauldron.CauldronInteractions;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTab.ItemDisplayParameters;
import net.minecraft.world.item.CreativeModeTab.Output;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import dev.vladeezyy.kilnwork.platform.Registrar;
import dev.vladeezyy.kilnwork.block.GaugeBlock;
import dev.vladeezyy.kilnwork.item.BlockTooltipItem;
import dev.vladeezyy.kilnwork.registration.BlockRegister;
import java.util.Set;
import java.util.function.Supplier;
import dev.vladeezyy.kilnwork.registration.EnumObject;
import dev.vladeezyy.kilnwork.registration.BuildingBlocks;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class Registration {
  private static final BlockRegister BLOCKS = new BlockRegister(Ceramics.MOD_ID);
  private static final Registrar<Item> ITEMS = BLOCKS.items();
  private static final Registrar<MenuType<?>> MENUS = Registrar.create(Registries.MENU, Ceramics.MOD_ID);
  private static final Registrar<BlockEntityType<?>> BLOCK_ENTITIES = Registrar.create(Registries.BLOCK_ENTITY_TYPE, Ceramics.MOD_ID);
  private static final Registrar<RecipeSerializer<?>> RECIPE_SERIALIZERS = Registrar.create(Registries.RECIPE_SERIALIZER, Ceramics.MOD_ID);
  private static final Registrar<RecipeType<?>> RECIPE_TYPES = Registrar.create(Registries.RECIPE_TYPE, Ceramics.MOD_ID);
  private static final Registrar<CreativeModeTab> CREATIVE_TABS = Registrar.create(Registries.CREATIVE_MODE_TAB, Ceramics.MOD_ID);
  private static final Registrar<DataComponentType<?>> DATA_COMPONENTS = Registrar.create(Registries.DATA_COMPONENT_TYPE, Ceramics.MOD_ID);

  /**
   * Initializes the registries (the loader registers the entries) and the loader hooks
   */
  public static void init() {
    registerFluidHandlers();
    // since 1.21.2 clients only get the recipes the server sends, JEI shows the kiln recipes from these
    CeramicsPlatform.get().syncRecipes(KILN_RECIPE, KILN_SERIALIZER);
  }

  /* Creative tab */
  static {
    CREATIVE_TABS.register(Ceramics.MOD_ID, () ->
      KilnworkPlatform.get().creativeTab()
        .title(Component.translatable("creative_tab.ceramics"))
        .icon(() -> new ItemStack(Registration.KILN))
        .displayItems(Registration::addTabItems)
        .build());
  }

  /* Data components, replacing the NBT of 1.20 */
  /** Fluid inside a fluid clay bucket, was the "fluid" tag */
  public static final Registrar.Entry<DataComponentType<?>,DataComponentType<Fluid>> BUCKET_FLUID = DATA_COMPONENTS.registerComponentType("fluid", builder -> builder.persistent(BuiltInRegistries.FLUID.byNameCodec()).networkSynchronized(ByteBufCodecs.registry(Registries.FLUID)));
  /** Block inside a solid clay bucket, was the "block" tag */
  public static final Registrar.Entry<DataComponentType<?>,DataComponentType<Block>> BUCKET_BLOCK = DATA_COMPONENTS.registerComponentType("block", builder -> builder.persistent(BuiltInRegistries.BLOCK.byNameCodec()).networkSynchronized(ByteBufCodecs.registry(Registries.BLOCK)));
  /** Cracks on a crackable block item, was the "cracks" tag */
  public static final Registrar.Entry<DataComponentType<?>,DataComponentType<Integer>> CRACKS = DATA_COMPONENTS.registerComponentType("cracks", builder -> builder.persistent(Codec.intRange(1, 5)).networkSynchronized(ByteBufCodecs.VAR_INT));

  /**
   * Item block function that sets the group props
   */
  private static final BlockRegister.BlockItemFactory<Block> DEFAULT_BLOCK_ITEM = BlockRegister::blockItem;
  /**
   * Item block function using {@link FixedTooltipBlockItem}
   */
  private static final Function<String, BlockRegister.BlockItemFactory<Block>> HEAT_RESISTANT = name -> (block, props) -> new HeatResistantBlockItem(block, props, name);

  /**
   * Mapping for terracotta to make registration easier
   */
  public static final EnumObject<DyeColor, Block> TERRACOTTA = EnumObject.<DyeColor, Block>builder(DyeColor.class)
    .put(DyeColor.WHITE, () -> Blocks.WHITE_TERRACOTTA)
    .put(DyeColor.ORANGE, () -> Blocks.ORANGE_TERRACOTTA)
    .put(DyeColor.MAGENTA, () -> Blocks.MAGENTA_TERRACOTTA)
    .put(DyeColor.LIGHT_BLUE, () -> Blocks.LIGHT_BLUE_TERRACOTTA)
    .put(DyeColor.YELLOW, () -> Blocks.YELLOW_TERRACOTTA)
    .put(DyeColor.LIME, () -> Blocks.LIME_TERRACOTTA)
    .put(DyeColor.PINK, () -> Blocks.PINK_TERRACOTTA)
    .put(DyeColor.GRAY, () -> Blocks.GRAY_TERRACOTTA)
    .put(DyeColor.LIGHT_GRAY, () -> Blocks.LIGHT_GRAY_TERRACOTTA)
    .put(DyeColor.CYAN, () -> Blocks.CYAN_TERRACOTTA)
    .put(DyeColor.PURPLE, () -> Blocks.PURPLE_TERRACOTTA)
    .put(DyeColor.BLUE, () -> Blocks.BLUE_TERRACOTTA)
    .put(DyeColor.BROWN, () -> Blocks.BROWN_TERRACOTTA)
    .put(DyeColor.GREEN, () -> Blocks.GREEN_TERRACOTTA)
    .put(DyeColor.RED, () -> Blocks.RED_TERRACOTTA)
    .put(DyeColor.BLACK, () -> Blocks.BLACK_TERRACOTTA)
    .build();

  /* Building blocks */
  public static final Registrar.Entry<Block, Block> UNFIRED_PORCELAIN_BLOCK = BLOCKS.register("unfired_porcelain_block", Block::new, () -> Block.Properties.of().mapColor(MapColor.CLAY).strength(0.6F).sound(SoundType.GRAVEL), DEFAULT_BLOCK_ITEM);

  // porcelain
  public static final EnumObject<DyeColor, Block> PORCELAIN_BLOCK = registerEnum(DyeColor.values(), "porcelain", (color, props) -> new Block(props), color -> Block.Properties.ofFullCopy(TERRACOTTA.get(color)), DEFAULT_BLOCK_ITEM);
  public static final EnumObject<RainbowPorcelain, Block> RAINBOW_PORCELAIN = registerEnum(RainbowPorcelain.values(), "rainbow_porcelain",
    (color, props) -> new Block(props), color -> Block.Properties.of().mapColor(color.getColor()).instrument(NoteBlockInstrument.BASEDRUM).strength(2.0F, 6.0F).requiresCorrectToolForDrops(), BlockTooltipItem::new);

  // clay bricks
  public static final BuildingBlocks DARK_BRICKS = registerWallBuilding("dark_bricks", () -> Block.Properties.of().mapColor(MapColor.COLOR_RED).instrument(NoteBlockInstrument.BASEDRUM).strength(2.0F, 6.0F).requiresCorrectToolForDrops());
  public static final BuildingBlocks LAVA_BRICKS = registerWallBuilding("lava_bricks", () -> Block.Properties.of().mapColor(MapColor.COLOR_ORANGE).instrument(NoteBlockInstrument.BASEDRUM).strength(2.0F, 6.0F).requiresCorrectToolForDrops());
  public static final BuildingBlocks DRAGON_BRICKS = registerWallBuilding("dragon_bricks", () -> Block.Properties.of().mapColor(MapColor.PODZOL).instrument(NoteBlockInstrument.BASEDRUM).strength(2.0F, 6.0F).requiresCorrectToolForDrops());

  // porcelain bricks
  public static final BuildingBlocks PORCELAIN_BRICKS = registerWallBuilding("porcelain_bricks", () -> Block.Properties.of().mapColor(MapColor.SNOW).instrument(NoteBlockInstrument.BASEDRUM).strength(2.0F, 6.0F).requiresCorrectToolForDrops());
  public static final BuildingBlocks MONOCHROME_BRICKS = registerWallBuilding("monochrome_bricks", () -> Block.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).strength(2.0F, 6.0F).requiresCorrectToolForDrops());
  public static final BuildingBlocks GOLDEN_BRICKS = registerWallBuilding("golden_bricks", () -> Block.Properties.of().mapColor(MapColor.COLOR_YELLOW).instrument(NoteBlockInstrument.BASEDRUM).strength(2.0F, 6.0F).requiresCorrectToolForDrops());
  public static final BuildingBlocks MARINE_BRICKS = registerWallBuilding("marine_bricks", () -> Block.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).instrument(NoteBlockInstrument.BASEDRUM).strength(2.0F, 6.0F).requiresCorrectToolForDrops());
  public static final BuildingBlocks RAINBOW_BRICKS = registerWallBuilding("rainbow_bricks", () -> Block.Properties.of().mapColor(MapColor.COLOR_GREEN).instrument(NoteBlockInstrument.BASEDRUM).strength(2.0F, 6.0F).requiresCorrectToolForDrops());


  /* items */
  public static final Registrar.Entry<Item, Item> UNFIRED_PORCELAIN = ITEMS.registerSimpleItem("unfired_porcelain");
  public static final Registrar.Entry<Item, Item> PORCELAIN_BRICK = ITEMS.registerSimpleItem("porcelain_brick");

  // tools
  public static final Registrar.Entry<Item, Item> UNFIRED_CLAY_BUCKET;
  public static final Registrar.Entry<Item, EmptyClayBucketItem> EMPTY_CLAY_BUCKET, CRACKED_EMPTY_CLAY_BUCKET;
  public static final Registrar.Entry<Item, FluidClayBucketItem> FLUID_CLAY_BUCKET, CRACKED_FLUID_CLAY_BUCKET;
  public static final Registrar.Entry<Item, MilkClayBucketItem> MILK_CLAY_BUCKET, CRACKED_MILK_CLAY_BUCKET;
  public static final Registrar.Entry<Item, SolidClayBucketItem> SOLID_CLAY_BUCKET, CRACKED_SOLID_CLAY_BUCKET;
  static {
    // empty
    UNFIRED_CLAY_BUCKET = ITEMS.registerSimpleItem("unfired_clay_bucket", () -> new Item.Properties().stacksTo(16));
    EMPTY_CLAY_BUCKET = ITEMS.registerItem("empty_clay_bucket", props -> new EmptyClayBucketItem(false, props), () -> new Item.Properties().stacksTo(16));
    CRACKED_EMPTY_CLAY_BUCKET = ITEMS.registerItem("cracked_empty_clay_bucket", props -> new EmptyClayBucketItem(true, props), () -> new Item.Properties().stacksTo(16));
    // uncracked filled
    FLUID_CLAY_BUCKET = ITEMS.registerItem("fluid_clay_bucket", props -> new FluidClayBucketItem(false, props), () -> new Item.Properties().stacksTo(1).craftRemainder(EMPTY_CLAY_BUCKET.get()));
    MILK_CLAY_BUCKET = ITEMS.registerItem("milk_clay_bucket", props -> new MilkClayBucketItem(false, props), () -> new Item.Properties().stacksTo(1).craftRemainder(EMPTY_CLAY_BUCKET.get()).component(DataComponents.CONSUMABLE, Consumables.DEFAULT_DRINK));
    SOLID_CLAY_BUCKET = ITEMS.registerItem("solid_clay_bucket", props -> new SolidClayBucketItem(false, props), () -> new Item.Properties().stacksTo(1).craftRemainder(EMPTY_CLAY_BUCKET.get()));
    // cracked filled
    CRACKED_FLUID_CLAY_BUCKET = ITEMS.registerItem("cracked_fluid_clay_bucket", props -> new FluidClayBucketItem(true, props), () -> new Item.Properties().stacksTo(1));
    CRACKED_MILK_CLAY_BUCKET = ITEMS.registerItem("cracked_milk_clay_bucket", props -> new MilkClayBucketItem(true, props), () -> new Item.Properties().stacksTo(1).component(DataComponents.CONSUMABLE, Consumables.DEFAULT_DRINK));
    CRACKED_SOLID_CLAY_BUCKET = ITEMS.registerItem("cracked_solid_clay_bucket", props -> new SolidClayBucketItem(true, props), () -> new Item.Properties().stacksTo(1));
  }

  // armor
  public static final Registrar.Entry<Item, Item> UNFIRED_CLAY_PLATE = ITEMS.registerSimpleItem("unfired_clay_plate");
  public static final Registrar.Entry<Item, Item> CLAY_PLATE = ITEMS.registerSimpleItem("clay_plate");
  // armor items are plain items since 1.21.2, the equippable component does the rest
  public static final Registrar.Entry<Item, Item> CLAY_HELMET = armor("clay_helmet", ArmorType.HELMET);
  public static final Registrar.Entry<Item, Item> CLAY_CHESTPLATE = armor("clay_chestplate", ArmorType.CHESTPLATE);
  public static final Registrar.Entry<Item, Item> CLAY_LEGGINGS = armor("clay_leggings", ArmorType.LEGGINGS);
  public static final Registrar.Entry<Item, Item> CLAY_BOOTS = armor("clay_boots", ArmorType.BOOTS);

  // kiln block
  public static final Registrar.Entry<Block, KilnBlock> KILN = BLOCKS.register("kiln", KilnBlock::new, () -> terracottaProps(MapColor.COLOR_ORANGE).lightLevel(s -> s.getValue(KilnBlock.LIT) ? 13 : 0), DEFAULT_BLOCK_ITEM);
  public static final Registrar.Entry<MenuType<?>,MenuType<KilnMenu>> KILN_MENU = MENUS.register("kiln", () -> KilnworkPlatform.get().menuType(KilnMenu::new));
  public static final Registrar.Entry<BlockEntityType<?>,BlockEntityType<KilnBlockEntity>> KILN_BLOCK_ENTITY = blockEntity("kiln", KilnBlockEntity::new, () -> Set.of(KILN.get()));
  // kiln recipes
  public static final Registrar.Entry<RecipeType<?>,RecipeType<KilnRecipe>> KILN_RECIPE = RECIPE_TYPES.register("kiln", () -> new RecipeType<KilnRecipe>() { @Override public String toString() { return Ceramics.locationName("kiln"); } });
  public static final Registrar.Entry<RecipeSerializer<?>,RecipeSerializer<KilnRecipe>> KILN_SERIALIZER = RECIPE_SERIALIZERS.register("kiln", () -> new RecipeSerializer<>(KilnRecipe.MAP_CODEC, KilnRecipe.STREAM_CODEC));

  /*
   * fluid handling
   */
  // gauge
  public static final Registrar.Entry<Block, GaugeBlock> TERRACOTTA_GAUGE, PORCELAIN_GAUGE;

  static {
    BlockRegister.BlockItemFactory<Block> GAUGE_BLOCK_ITEM = (block, props) -> new FixedTooltipBlockItem(block, props, "gauge.tooltip");
    Supplier<BlockBehaviour.Properties> GAUGE_PROPERTIES = () -> BlockBehaviour.Properties.of().mapColor(MapColor.NONE).pushReaction(PushReaction.DESTROY).noCollision().strength(0.5F).noOcclusion().requiresCorrectToolForDrops();
    TERRACOTTA_GAUGE = BLOCKS.register("terracotta_gauge", GaugeBlock::new, GAUGE_PROPERTIES, GAUGE_BLOCK_ITEM);
    PORCELAIN_GAUGE = BLOCKS.register("porcelain_gauge", GaugeBlock::new, GAUGE_PROPERTIES, GAUGE_BLOCK_ITEM);
    // the 1.16 gauge was renamed to the terracotta gauge, keep old worlds working
    BLOCKS.addAlias("gauge", "terracotta_gauge");
  }

  // raw clay blocks
  public static final Registrar.Entry<Block, CisternBlock> CLAY_CISTERN, UNFIRED_CISTERN;
  public static final Registrar.Entry<Block, FaucetBlock> CLAY_FAUCET, UNFIRED_FAUCET;
  public static final Registrar.Entry<Block, ChannelBlock> CLAY_CHANNEL, UNFIRED_CHANNEL;
  static {
    Supplier<BlockBehaviour.Properties> CLAY_PROPERTIES = () -> BlockBehaviour.Properties.of().mapColor(MapColor.CLAY).strength(0.6F).sound(SoundType.GRAVEL).noOcclusion();
    CLAY_CISTERN = BLOCKS.register("clay_cistern", CisternBlock::new, CLAY_PROPERTIES, DEFAULT_BLOCK_ITEM);
    UNFIRED_CISTERN = BLOCKS.register("unfired_cistern", CisternBlock::new, CLAY_PROPERTIES, DEFAULT_BLOCK_ITEM);
    CLAY_FAUCET = BLOCKS.register("clay_faucet", FaucetBlock::new, CLAY_PROPERTIES, DEFAULT_BLOCK_ITEM);
    UNFIRED_FAUCET = BLOCKS.register("unfired_faucet", FaucetBlock::new, CLAY_PROPERTIES, DEFAULT_BLOCK_ITEM);
    CLAY_CHANNEL = BLOCKS.register("clay_channel", ChannelBlock::new, CLAY_PROPERTIES, DEFAULT_BLOCK_ITEM);
    UNFIRED_CHANNEL = BLOCKS.register("unfired_channel", ChannelBlock::new, CLAY_PROPERTIES, DEFAULT_BLOCK_ITEM);
  }

  // cistern
  private static final Function<String,BlockRegister.BlockItemFactory<Block>> CRACKABLE_BLOCK_ITEM = tooltip -> (block, props) -> new CrackableBlockItem(block, props, tooltip);
  private static final BlockRegister.BlockItemFactory<Block> TERRACOTTA_CISTERN_BLOCK_ITEM = CRACKABLE_BLOCK_ITEM.apply("cistern.tooltip");
  private static final BlockRegister.BlockItemFactory<Block> PORCELAIN_CISTERN_BLOCK_ITEM = HEAT_RESISTANT.apply("cistern.tooltip");
  public static final Registrar.Entry<Block, FluidCisternBlock> TERRACOTTA_CISTERN = BLOCKS.register("terracotta_cistern", props -> new FluidCisternBlock(props, true), () -> terracottaProps(MapColor.COLOR_ORANGE).noOcclusion().randomTicks(), TERRACOTTA_CISTERN_BLOCK_ITEM);
  public static final EnumObject<DyeColor, FluidCisternBlock> COLORED_CISTERN = registerEnum(DyeColor.values(), "terracotta_cistern", (color, props) -> new FluidCisternBlock(props, true), color -> terracottaProps(getTerracottaColor(color)).noOcclusion().randomTicks(), TERRACOTTA_CISTERN_BLOCK_ITEM);
  public static final EnumObject<DyeColor, FluidCisternBlock> PORCELAIN_CISTERN = registerEnum(DyeColor.values(), "porcelain_cistern", (color, props) -> new FluidCisternBlock(props, false), color -> terracottaProps(getTerracottaColor(color)).noOcclusion(), PORCELAIN_CISTERN_BLOCK_ITEM);
  public static final Registrar.Entry<BlockEntityType<?>,BlockEntityType<CisternBlockEntity>> CISTERN_BLOCK_ENTITY = blockEntity("cistern", CisternBlockEntity::new, () -> {
    Set<Block> blocks = new java.util.HashSet<>();
    blocks.add(TERRACOTTA_CISTERN.get());
    blocks.addAll(COLORED_CISTERN.values());
    blocks.addAll(PORCELAIN_CISTERN.values());
    return blocks;
  });

  // faucet
  public static final Registrar.Entry<Block, PouringFaucetBlock> TERRACOTTA_FAUCET = BLOCKS.register("terracotta_faucet", props -> new PouringFaucetBlock(props, true), () -> terracottaProps(MapColor.COLOR_ORANGE).noOcclusion().randomTicks(), CRACKABLE_BLOCK_ITEM.apply("faucet.tooltip"));
  public static final Registrar.Entry<Block, PouringFaucetBlock> PORCELAIN_FAUCET = BLOCKS.register("porcelain_faucet", props -> new PouringFaucetBlock(props, false), () -> terracottaProps(MapColor.TERRACOTTA_WHITE).noOcclusion(), HEAT_RESISTANT.apply("faucet.tooltip"));
  public static final Registrar.Entry<BlockEntityType<?>,BlockEntityType<FaucetBlockEntity>> FAUCET_BLOCK_ENTITY = blockEntity("faucet", FaucetBlockEntity::new, () -> Set.of(TERRACOTTA_FAUCET.get(), PORCELAIN_FAUCET.get()));

  // channel
  public static final Registrar.Entry<Block, FlowingChannelBlock> TERRACOTTA_CHANNEL = BLOCKS.register("terracotta_channel", props -> new FlowingChannelBlock(props, true), () -> terracottaProps(MapColor.COLOR_ORANGE).noOcclusion().randomTicks(), CRACKABLE_BLOCK_ITEM.apply("channel.tooltip"));
  public static final Registrar.Entry<Block, FlowingChannelBlock> PORCELAIN_CHANNEL = BLOCKS.register("porcelain_channel", props -> new FlowingChannelBlock(props, false), () -> terracottaProps(MapColor.TERRACOTTA_WHITE).noOcclusion(), HEAT_RESISTANT.apply("channel.tooltip"));
  public static final Registrar.Entry<BlockEntityType<?>,BlockEntityType<ChannelBlockEntity>> CHANNEL_BLOCK_ENTITY = blockEntity("channel", ChannelBlockEntity::new, () -> Set.of(TERRACOTTA_CHANNEL.get(), PORCELAIN_CHANNEL.get()));

  // clay repair
  public static final Registrar.Entry<RecipeSerializer<?>,RecipeSerializer<CrackedClayRepairRecipe>> CLAY_REPAIR_RECIPE_SERIALIZER = RECIPE_SERIALIZERS.register("cracked_clay_repair", () -> CrackedClayRepairRecipe.SERIALIZER);

  private static void registerFluidHandlers() {
    // blocks, in 1.20 these were getCapability overrides on the block entities
    LoaderHooks.blockFluidHandler(CISTERN_BLOCK_ENTITY, (cistern, side) -> cistern.getPublicHandler());
    LoaderHooks.blockFluidHandler(CHANNEL_BLOCK_ENTITY, ChannelBlockEntity::getFluidHandler);
    // clay buckets, milk buckets were missing a fluid handler in 1.20 (issue #88 of the original)
    for (Supplier<? extends Item> bucket : List.of(EMPTY_CLAY_BUCKET, CRACKED_EMPTY_CLAY_BUCKET, FLUID_CLAY_BUCKET, CRACKED_FLUID_CLAY_BUCKET, MILK_CLAY_BUCKET, CRACKED_MILK_CLAY_BUCKET)) {
      LoaderHooks.itemFluidHandler(bucket, FluidClayBucketWrapper::new);
    }
    // solid buckets: powder snow is also a fluid if a mod adds one
    Function<ItemStack, dev.vladeezyy.kilnwork.fluid.api.IFluidHandlerItem> solid = stack -> {
      if (stack.getItem() instanceof SolidClayBucketItem bucket && bucket.getBlock(stack) == Blocks.POWDER_SNOW) {
        Optional<Fluid> optional = TagPreference.getPreference(CeramicsTags.Fluids.POWDERED_SNOW);
        if (optional.isPresent()) {
          return new ConstantFluidContainerWrapper(new dev.vladeezyy.kilnwork.fluid.api.FluidStack(optional.get(), dev.vladeezyy.kilnwork.fluid.api.FluidStack.BUCKET_VOLUME), stack);
        }
      }
      return null;
    };
    LoaderHooks.itemFluidHandler(SOLID_CLAY_BUCKET, solid);
    LoaderHooks.itemFluidHandler(CRACKED_SOLID_CLAY_BUCKET, solid);
  }

  /** Setup after registration, on the main thread: bucketable blocks and cauldron interactions */
  public static void setup() {
    SolidClayBucketItem.loadBucketableBlocks();
    {
      record FillClayBucketCauldronInteraction(Predicate<BlockState> cauldron, java.util.function.Supplier<ItemStack> filledBucket, SoundEvent sound) implements CauldronInteraction {
        @Override
        public InteractionResult interact(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, ItemStack stack) {
          return CauldronInteractions.fillBucket(state, level, pos, player, hand, stack, filledBucket.get(), cauldron, sound);
        }
      }

      // filling buckets - not needed on empty cauldron. The stacks are made on use: on Fabric this runs before the
      // tags are bound, and since 1.21.11 an unbound tag check throws
      // TODO: this is pretty hardcoded, can this be generalized in any way?
      Predicate<BlockState> full = state -> state.getValue(LayeredCauldronBlock.LEVEL) == 3;
      CauldronInteractions.WATER.put(EMPTY_CLAY_BUCKET.get(),         new FillClayBucketCauldronInteraction(full, () -> BaseClayBucketItem.withFluid(Fluids.WATER, false), SoundEvents.BUCKET_FILL));
      CauldronInteractions.WATER.put(CRACKED_EMPTY_CLAY_BUCKET.get(), new FillClayBucketCauldronInteraction(full, () -> BaseClayBucketItem.withFluid(Fluids.WATER, true),  SoundEvents.BUCKET_FILL));
      CauldronInteractions.POWDER_SNOW.put(EMPTY_CLAY_BUCKET.get(),         new FillClayBucketCauldronInteraction(full, () -> BaseClayBucketItem.withBlock(Blocks.POWDER_SNOW, false), SoundEvents.BUCKET_FILL_POWDER_SNOW));
      CauldronInteractions.POWDER_SNOW.put(CRACKED_EMPTY_CLAY_BUCKET.get(), new FillClayBucketCauldronInteraction(full, () -> BaseClayBucketItem.withBlock(Blocks.POWDER_SNOW, true),  SoundEvents.BUCKET_FILL_POWDER_SNOW));
      CauldronInteraction fillLava = new FillClayBucketCauldronInteraction(state -> true, () -> BaseClayBucketItem.withFluid(Fluids.LAVA, true), SoundEvents.BUCKET_FILL_LAVA);
      CauldronInteractions.LAVA.put(EMPTY_CLAY_BUCKET.get(),         fillLava);
      CauldronInteractions.LAVA.put(CRACKED_EMPTY_CLAY_BUCKET.get(), fillLava);

      // emptying buckets - needed on all as fluid can replace existing
      BiConsumer<ItemLike, CauldronInteraction> addAll = (item, interaction) -> {
        CauldronInteractions.EMPTY.put(item.asItem(), interaction);
        CauldronInteractions.WATER.put(item.asItem(), interaction);
        CauldronInteractions.LAVA.put(item.asItem(), interaction);
        CauldronInteractions.POWDER_SNOW.put(item.asItem(), interaction);
      };
      addAll.accept(FLUID_CLAY_BUCKET,         new EmptyFluidBucketCauldronInteraction(FLUID_CLAY_BUCKET.get()));
      addAll.accept(CRACKED_FLUID_CLAY_BUCKET, new EmptyFluidBucketCauldronInteraction(CRACKED_FLUID_CLAY_BUCKET.get()));
      addAll.accept(SOLID_CLAY_BUCKET,         new EmptySolidBucketCauldronInteraction(SOLID_CLAY_BUCKET.get()));
      addAll.accept(CRACKED_SOLID_CLAY_BUCKET, new EmptySolidBucketCauldronInteraction(CRACKED_SOLID_CLAY_BUCKET.get()));
    }
  }

  /** Adds relevant items to the creative tab */
  private static void addTabItems(ItemDisplayParameters parameters, Output output) {
    output.accept(KILN);

    // clay bricks
    accept(output, DARK_BRICKS);
    accept(output, LAVA_BRICKS);
    accept(output, DRAGON_BRICKS);

    // basic porcelain
    output.accept(UNFIRED_PORCELAIN);
    output.accept(UNFIRED_PORCELAIN_BLOCK);
    accept(output, PORCELAIN_BLOCK);
    accept(output, RAINBOW_PORCELAIN);

    // porcelain bricks
    output.accept(PORCELAIN_BRICK);
    accept(output, PORCELAIN_BRICKS);
    accept(output, MONOCHROME_BRICKS);
    accept(output, GOLDEN_BRICKS);
    accept(output, MARINE_BRICKS);
    accept(output, RAINBOW_BRICKS);

    // fluid handling - terracotta
    output.accept(TERRACOTTA_GAUGE);
    output.accept(TERRACOTTA_FAUCET);
    output.accept(TERRACOTTA_CHANNEL);
    output.accept(TERRACOTTA_CISTERN);
    accept(output, COLORED_CISTERN);
    // fluid handling - porcelain
    output.accept(PORCELAIN_GAUGE);
    output.accept(PORCELAIN_FAUCET);
    output.accept(PORCELAIN_CHANNEL);
    accept(output, PORCELAIN_CISTERN);
    // fluid handling - unfired
    output.accept(CLAY_FAUCET);
    output.accept(CLAY_CHANNEL);
    output.accept(CLAY_CISTERN);
    output.accept(UNFIRED_FAUCET);
    output.accept(UNFIRED_CHANNEL);
    output.accept(UNFIRED_CISTERN);

    // armor
    output.accept(UNFIRED_CLAY_PLATE);
    output.accept(CLAY_PLATE);
    output.accept(CLAY_HELMET);
    output.accept(CLAY_CHESTPLATE);
    output.accept(CLAY_LEGGINGS);
    output.accept(CLAY_BOOTS);

    // buckets
    // the original added the empty bucket twice and left out the solid buckets (powder snow), fixed here
    output.accept(UNFIRED_CLAY_BUCKET);
    Consumer<ItemStack> consumer = output::accept;
    EMPTY_CLAY_BUCKET.get().addVariants(consumer);
    CRACKED_EMPTY_CLAY_BUCKET.get().addVariants(consumer);
    FLUID_CLAY_BUCKET.get().addVariants(consumer);
    CRACKED_FLUID_CLAY_BUCKET.get().addVariants(consumer);
    MILK_CLAY_BUCKET.get().addVariants(consumer);
    CRACKED_MILK_CLAY_BUCKET.get().addVariants(consumer);
    SOLID_CLAY_BUCKET.get().addVariants(consumer);
    CRACKED_SOLID_CLAY_BUCKET.get().addVariants(consumer);
  }

  /** Adds all members of an enum object to the given creative tab */
  private static void accept(Output output, EnumObject<?, ? extends ItemLike> object) {
    object.forEachItem(output::accept);
  }

  /** Adds a block with its slab, stairs and wall to the creative tab */
  private static void accept(Output output, BuildingBlocks object) {
    object.values().forEach(output::accept);
  }

  /** Registers a block entity type for the given blocks, resolved once the blocks exist */
  private static <T extends net.minecraft.world.level.block.entity.BlockEntity> Registrar.Entry<BlockEntityType<?>, BlockEntityType<T>> blockEntity(String name, BlockEntityType.BlockEntitySupplier<T> factory, Supplier<Set<Block>> blocks) {
    return BLOCK_ENTITIES.register(name, () -> new BlockEntityType<>(factory, blocks.get()));
  }

  /** Registers a block with slab, stairs and wall, all using the default block item */
  private static BuildingBlocks registerWallBuilding(String name, Supplier<BlockBehaviour.Properties> props) {
    return BLOCKS.registerBuilding(name, props, DEFAULT_BLOCK_ITEM);
  }

  /** Registers one block per enum value */
  private static <E extends Enum<E>, B extends Block> EnumObject<E, B> registerEnum(E[] values, String name, java.util.function.BiFunction<E, BlockBehaviour.Properties, B> mapper, Function<E, BlockBehaviour.Properties> props, BlockRegister.BlockItemFactory<? super B> item) {
    return BLOCKS.registerEnum(values, name, mapper, props, item);
  }

  /** Registers a clay armor piece */
  private static Registrar.Entry<Item, Item> armor(String name, ArmorType type) {
    return ITEMS.registerItem(name, Item::new, () -> new Item.Properties().humanoidArmor(ArmorMaterials.CLAY, type));
  }

  /**
   * Standard hardened clay properties
   * @param color  Map color of block
   * @return  Block properties
   */
  private static BlockBehaviour.Properties terracottaProps(MapColor color) {
    return BlockBehaviour.Properties.of().mapColor(color).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(1.25F, 4.2F);
  }

  /**
   * Gets the terracotta map color for the given dye color
   * @param color  Dye color
   * @return  Material color
   */
  private static MapColor getTerracottaColor(DyeColor color) {
    return switch (color) {
      case WHITE      -> MapColor.TERRACOTTA_WHITE;
      case ORANGE     -> MapColor.TERRACOTTA_ORANGE;
      case MAGENTA    -> MapColor.TERRACOTTA_MAGENTA;
      case LIGHT_BLUE -> MapColor.TERRACOTTA_LIGHT_BLUE;
      case YELLOW     -> MapColor.TERRACOTTA_YELLOW;
      case LIME       -> MapColor.TERRACOTTA_LIGHT_GREEN;
      case PINK       -> MapColor.TERRACOTTA_PINK;
      case GRAY       -> MapColor.TERRACOTTA_GRAY;
      case LIGHT_GRAY -> MapColor.TERRACOTTA_LIGHT_GRAY;
      case CYAN       -> MapColor.TERRACOTTA_CYAN;
      case PURPLE     -> MapColor.TERRACOTTA_PURPLE;
      case BLUE       -> MapColor.TERRACOTTA_BLUE;
      case BROWN      -> MapColor.TERRACOTTA_BROWN;
      case GREEN      -> MapColor.TERRACOTTA_GREEN;
      case RED        -> MapColor.TERRACOTTA_RED;
      case BLACK      -> MapColor.TERRACOTTA_BLACK;
    };
  }
}

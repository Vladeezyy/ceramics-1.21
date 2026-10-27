package knightminer.ceramics.selftest;

import dev.vladeezyy.kilnwork.fluid.FluidTransferHelper;
import dev.vladeezyy.kilnwork.fluid.api.FluidTank;
import dev.vladeezyy.kilnwork.fluid.api.IFluidHandler;
import dev.vladeezyy.kilnwork.fluid.api.IFluidHandler.FluidAction;
import dev.vladeezyy.kilnwork.fluid.api.IFluidHandlerItem;
import dev.vladeezyy.kilnwork.platform.KilnworkPlatform;
import knightminer.ceramics.Ceramics;
import knightminer.ceramics.platform.CeramicsPlatform;
import knightminer.ceramics.Registration;
import knightminer.ceramics.blocks.ChannelBlock;
import knightminer.ceramics.blocks.ChannelBlock.ChannelConnection;
import knightminer.ceramics.blocks.CisternBlock;
import knightminer.ceramics.blocks.FaucetBlock;
import knightminer.ceramics.blocks.entity.CisternBlockEntity;
import knightminer.ceramics.blocks.entity.FaucetBlockEntity;
import knightminer.ceramics.items.BaseClayBucketItem;
import knightminer.ceramics.items.CrackableBlockItem;
import knightminer.ceramics.recipe.KilnRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import knightminer.ceramics.blocks.entity.CrackableBlockEntityHandler.ICrackableBlock;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.util.ProblemReporter;
import dev.vladeezyy.kilnwork.fluid.api.FluidStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * Functional checks on a dev server, only in dev runs with -Dceramics.selftest=true (the source set is not in the jar).
 * Prints "[Ceramics selftest] PASS n/n" or the failed checks, then stops the server. The loader parts call
 * {@link #serverStarted} and {@link #serverTick}.
 */
public class SelfTest {
  private static final String PREFIX = "[Ceramics selftest] ";
  private static final boolean ENABLED = Boolean.getBoolean("ceramics.selftest");
  private static final List<String> failures = new ArrayList<>();
  private static int checks = 0;
  /** Tick counter after server start, -1 when not running */
  private static int tick = -1;
  private static BlockPos origin;

  private static void check(String name, boolean result) {
    checks++;
    if (result) {
      Ceramics.LOG.info(PREFIX + "ok   {}", name);
    } else {
      failures.add(name);
      Ceramics.LOG.error(PREFIX + "FAIL {}", name);
    }
  }

  private static void check(String name, BooleanSupplier test) {
    boolean result;
    try {
      result = test.getAsBoolean();
    } catch (RuntimeException e) {
      Ceramics.LOG.error(PREFIX + "exception in {}", name, e);
      result = false;
    }
    check(name, result);
  }

  public static void serverStarted(MinecraftServer server) {
    if (ENABLED) {
      ServerLevel level = server.overworld();
      BlockPos spawn = level.getRespawnData().pos();
      origin = new BlockPos(spawn.getX(), 200, spawn.getZ());
      // since 1.21.9 no chunk ticks without a player or a ticket, keep the test area ticking
      for (int cx = (origin.getX() - 2) >> 4; cx <= (origin.getX() + 12) >> 4; cx++) {
        for (int cz = (origin.getZ() - 2) >> 4; cz <= (origin.getZ() + 6) >> 4; cz++) {
          level.setChunkForced(cx, cz, true);
        }
      }
      // clear a box of air
      for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-2, -2, -2), origin.offset(12, 8, 6))) {
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
      }
      instantChecks(server, level);
      worldSetup(level);
      tick = 0;
    }
  }

  public static void serverTick(MinecraftServer server) {
    if (tick < 0) {
      return;
    }
    tick++;
    ServerLevel level = server.overworld();
    if (tick == 120) {
      worldChecks(level);
      int passed = checks - failures.size();
      if (failures.isEmpty()) {
        Ceramics.LOG.info(PREFIX + "PASS {}/{}", passed, checks);
      } else {
        Ceramics.LOG.error(PREFIX + "FAILED {}/{}: {}", passed, checks, failures);
      }
      tick = -1;
      server.halt(false);
    }
  }

  /* Checks not needing ticks */

  private static void instantChecks(MinecraftServer server, ServerLevel level) {
    // registries
    long blocks = BuiltInRegistries.BLOCK.keySet().stream().filter(id -> id.getNamespace().equals(Ceramics.MOD_ID)).count();
    check("103 blocks registered (" + blocks + ")", blocks == 103);
    check("gauge alias from 1.16 resolves", () -> BuiltInRegistries.BLOCK.getValue(Ceramics.getResource("gauge")) == Registration.TERRACOTTA_GAUGE.get());

    // recipes
    long recipes = server.getRecipeManager().getRecipes().stream().filter(r -> r.id().identifier().getNamespace().equals(Ceramics.MOD_ID)).count();
    check("282 recipes loaded (" + recipes + ")", recipes == 282);
    var kiln = server.getRecipeManager().getRecipes().stream().filter(r -> r.value().getType() == Registration.KILN_RECIPE.get()).toList();
    check("kiln recipes loaded (" + kiln.size() + ")", kiln.size() > 50);
    check("kiln fires unfired porcelain block into porcelain", () -> server.getRecipeManager()
      .getRecipeFor(Registration.KILN_RECIPE.get(), new SingleRecipeInput(new ItemStack(Registration.UNFIRED_PORCELAIN_BLOCK)), level)
      .map(r -> r.value().getOutput().is(Registration.PORCELAIN_BLOCK.get(net.minecraft.world.item.DyeColor.WHITE).asItem())).orElse(false));
    check("kiln also smelts terracotta in the furnace", () -> server.getRecipeManager()
      .getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(new ItemStack(Registration.UNFIRED_PORCELAIN_BLOCK)), level).isPresent());

    // clay repair recipe
    check("clay repair recipe removes 3 cracks", () -> {
      ItemStack cistern = CrackableBlockItem.setCracks(new ItemStack(Registration.TERRACOTTA_CISTERN), 4);
      CraftingInput input = CraftingInput.of(2, 1, List.of(cistern, new ItemStack(Items.CLAY_BALL)));
      return server.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level)
        .map(r -> CrackableBlockItem.getCracks(r.value().assemble(input, level.registryAccess())) == 1).orElse(false);
    });
    check("clay repair does not match an uncracked cistern", () -> {
      CraftingInput input = CraftingInput.of(2, 1, List.of(new ItemStack(Registration.TERRACOTTA_CISTERN), new ItemStack(Items.CLAY_BALL)));
      return server.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level)
        .filter(r -> r.id().identifier().getPath().endsWith("_repair")).isEmpty();
    });
    check("dyeing ignores cracked cisterns (no_nbt ingredient)", () -> {
      ItemStack cracked = CrackableBlockItem.setCracks(new ItemStack(Registration.TERRACOTTA_CISTERN), 1);
      ItemStack plain = new ItemStack(Registration.TERRACOTTA_CISTERN);
      ItemStack dye = new ItemStack(Items.RED_DYE);
      CraftingInput good = CraftingInput.of(3, 3, List.of(ItemStack.EMPTY, plain, ItemStack.EMPTY, plain, dye, plain, ItemStack.EMPTY, plain, ItemStack.EMPTY));
      CraftingInput bad = CraftingInput.of(3, 3, List.of(ItemStack.EMPTY, plain, ItemStack.EMPTY, plain, dye, cracked, ItemStack.EMPTY, plain, ItemStack.EMPTY));
      return server.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, good, level).isPresent()
        && server.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, bad, level).isEmpty();
    });

    // buckets
    check("water clay bucket holds water", () -> Registration.FLUID_CLAY_BUCKET.get().getFluid(BaseClayBucketItem.withFluid(Fluids.WATER, false)) == Fluids.WATER);
    check("lava gives a cracked bucket", () -> BaseClayBucketItem.withFluid(Fluids.LAVA, false).is(Registration.CRACKED_FLUID_CLAY_BUCKET.get()));
    check("empty clay bucket fills from a fluid handler", () -> {
      IFluidHandler source = new FluidTank(4000);
      source.fill(new FluidStack(Fluids.WATER, 4000), FluidAction.EXECUTE);
      IFluidHandlerItem bucket = KilnworkPlatform.get().fluidHandler(new ItemStack(Registration.EMPTY_CLAY_BUCKET.get()));
      FluidStack moved = bucket == null ? FluidStack.EMPTY : FluidTransferHelper.tryTransfer(source, bucket, 1000);
      ItemStack result = bucket == null ? ItemStack.EMPTY : bucket.getContainer();
      return moved.getAmount() == 1000 && result.is(Registration.FLUID_CLAY_BUCKET.get())
        && Registration.FLUID_CLAY_BUCKET.get().getFluid(result) == Fluids.WATER && amount(source) == 3000;
    });
    check("filled clay bucket empties into a fluid handler", () -> {
      IFluidHandler target = new FluidTank(4000);
      IFluidHandlerItem bucket = KilnworkPlatform.get().fluidHandler(BaseClayBucketItem.withFluid(Fluids.WATER, false));
      FluidStack moved = bucket == null ? FluidStack.EMPTY : FluidTransferHelper.tryTransfer(bucket, target, 1000);
      return moved.getAmount() == 1000 && bucket.getContainer().is(Registration.EMPTY_CLAY_BUCKET.get()) && amount(target) == 1000;
    });
    check("simulated drain leaves the bucket as it was", () -> {
      IFluidHandlerItem bucket = KilnworkPlatform.get().fluidHandler(BaseClayBucketItem.withFluid(Fluids.WATER, false));
      return bucket != null && bucket.drain(1000, FluidAction.SIMULATE).getAmount() == 1000
        && bucket.getContainer().is(Registration.FLUID_CLAY_BUCKET.get()) && bucket.getFluidInTank(0).getAmount() == 1000;
    });
    check("milk clay bucket has a fluid handler (issue #88)", () -> KilnworkPlatform.get().fluidHandler(new ItemStack(Registration.MILK_CLAY_BUCKET.get())) != null);
    check("powder snow is a solid bucket", () -> Registration.SOLID_CLAY_BUCKET.get().getBlock(BaseClayBucketItem.withBlock(Blocks.POWDER_SNOW, false)) == Blocks.POWDER_SNOW);
    check("cauldron interactions registered", () -> CauldronInteraction.WATER.map().containsKey(Registration.EMPTY_CLAY_BUCKET.get())
      && CauldronInteraction.EMPTY.map().containsKey(Registration.FLUID_CLAY_BUCKET.get()) && CauldronInteraction.LAVA.map().containsKey(Registration.CRACKED_EMPTY_CLAY_BUCKET.get()));
    check("lava clay bucket burns 20000 ticks", () -> CeramicsPlatform.get().burnTime(BaseClayBucketItem.withFluid(Fluids.LAVA, false)) == 20000);

    // data saved by 1.20: the world upgrade keeps unknown item NBT as custom data
    check("1.20 fluid bucket is read and converted", () -> {
      ItemStack stack = new ItemStack(Registration.CRACKED_FLUID_CLAY_BUCKET.get());
      CompoundTag tag = new CompoundTag();
      tag.putString("fluid", "minecraft:lava");
      stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
      boolean read = Registration.CRACKED_FLUID_CLAY_BUCKET.get().getFluid(stack) == Fluids.LAVA;
      stack.getItem().inventoryTick(stack, level, null, null);
      return read && stack.get(Registration.BUCKET_FLUID.get()) == Fluids.LAVA && !stack.has(DataComponents.CUSTOM_DATA);
    });
    check("1.20 solid bucket is read", () -> {
      ItemStack stack = new ItemStack(Registration.SOLID_CLAY_BUCKET.get());
      CompoundTag tag = new CompoundTag();
      tag.putString("block", "minecraft:powder_snow");
      stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
      return Registration.SOLID_CLAY_BUCKET.get().getBlock(stack) == Blocks.POWDER_SNOW;
    });
    check("1.20 cracked block item is read", () -> {
      ItemStack stack = new ItemStack(Registration.TERRACOTTA_FAUCET);
      CompoundTag tag = new CompoundTag();
      tag.putInt("cracks", 3);
      stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
      return CrackableBlockItem.getCracks(stack) == 3;
    });

    // armor
    check("clay helmet: 1 armor, 44 durability", () -> armor(new ItemStack(Registration.CLAY_HELMET.get())) == 1 && new ItemStack(Registration.CLAY_HELMET.get()).getMaxDamage() == 44);
    check("clay chestplate: 3 armor, 64 durability", () -> armor(new ItemStack(Registration.CLAY_CHESTPLATE.get())) == 3 && new ItemStack(Registration.CLAY_CHESTPLATE.get()).getMaxDamage() == 64);
    check("clay armor repairs with clay plates", () -> new ItemStack(Registration.CLAY_HELMET.get()).isValidRepairItem(new ItemStack(Registration.CLAY_PLATE.get())));
    check("clay armor uses the clay equipment asset", () -> new ItemStack(Registration.CLAY_BOOTS.get()).get(DataComponents.EQUIPPABLE).assetId().map(key -> key.identifier().equals(Ceramics.getResource("clay"))).orElse(false));

    // tags
    check("golden bricks need a stone tool or better", () -> Registration.GOLDEN_BRICKS.get().defaultBlockState().is(BlockTags.INCORRECT_FOR_WOODEN_TOOL));
    check("terracotta cistern is mineable with a pickaxe", () -> Registration.TERRACOTTA_CISTERN.get().defaultBlockState().is(BlockTags.MINEABLE_WITH_PICKAXE));
    check("faucet hides the gauge amount", () -> Registration.TERRACOTTA_FAUCET.get().defaultBlockState().is(dev.vladeezyy.kilnwork.tag.KilnworkTags.Blocks.HIDES_GAUGE_AMOUNT));

    // creative tab (the original listed empty buckets twice and no solid buckets)
    check("creative tab has solid buckets and one empty bucket", () -> {
      CreativeModeTab tab = BuiltInRegistries.CREATIVE_MODE_TAB.getValue(Ceramics.getResource(Ceramics.MOD_ID));
      tab.buildContents(new CreativeModeTab.ItemDisplayParameters(level.enabledFeatures(), true, level.registryAccess()));
      long empty = tab.getDisplayItems().stream().filter(s -> s.is(Registration.EMPTY_CLAY_BUCKET.get())).count();
      long solid = tab.getDisplayItems().stream().filter(s -> s.is(Registration.SOLID_CLAY_BUCKET.get())).count();
      return empty == 1 && solid >= 1;
    });
  }

  /* World */

  private static BlockPos at(int x, int y, int z) {
    return origin.offset(x, y, z);
  }

  private static void worldSetup(ServerLevel level) {
    // 1. cistern tower: base + 2 extensions, filled with water through the capability
    placeCistern(level, at(0, 0, 0));
    placeCistern(level, at(0, 1, 0));
    placeCistern(level, at(0, 2, 0));
    IFluidHandler tower = handler(level, at(0, 2, 0), Direction.UP);
    check("tower capability from the top extension", tower != null);
    if (tower != null) {
      int filled = fill(tower, new FluidStack(Fluids.WATER, 20000));
      check("tower of 3 terracotta cisterns holds 12000 (" + filled + ")", filled == 12000);
    }

    // 2. faucet: source cistern with lava -> faucet -> cistern below
    placeCistern(level, at(4, 1, 0));
    fill(level, at(4, 1, 0), new FluidStack(Fluids.WATER, 2000));
    level.setBlock(at(5, 1, 0), Registration.PORCELAIN_FAUCET.get().defaultBlockState().setValue(FaucetBlock.FACING, Direction.EAST), Block.UPDATE_ALL);
    placeCistern(level, at(5, 0, 0));
    if (level.getBlockEntity(at(5, 1, 0)) instanceof FaucetBlockEntity faucet) {
      faucet.activate();
    }

    // 3. channel: filled from the top, flows down into a cistern
    placeCistern(level, at(8, 0, 0));
    level.setBlock(at(8, 1, 0), Registration.PORCELAIN_CHANNEL.get().defaultBlockState().setValue(ChannelBlock.DOWN, true), Block.UPDATE_ALL);
    // side channel: flows out east into another channel, which flows down
    level.setBlock(at(9, 1, 0), Registration.PORCELAIN_CHANNEL.get().defaultBlockState().setValue(ChannelBlock.WEST, ChannelConnection.IN).setValue(ChannelBlock.DOWN, true), Block.UPDATE_ALL);
    placeCistern(level, at(9, 0, 0));
    IFluidHandler channelTop = handler(level, at(8, 1, 0), Direction.UP);
    check("channel has a top fluid handler", channelTop != null);
    if (channelTop != null) {
      int accepted = fill(channelTop, new FluidStack(Fluids.WATER, 100));
      check("channel top accepts 100 (" + accepted + ")", accepted == 100);
    }
    check("neighbor channel turned its east side to out", level.getBlockState(at(8, 1, 0)).getValue(ChannelBlock.EAST) == ChannelConnection.OUT);

    // 4. cracking: lava in a terracotta cistern cracks it, porcelain does not
    placeCistern(level, at(0, 0, 4));
    fill(level, at(0, 0, 4), new FluidStack(Fluids.LAVA, 1000));
    check("lava cracks a terracotta cistern", level.getBlockEntity(at(0, 0, 4)) instanceof CisternBlockEntity c && c.getCracksHandler().getCracks() == 1);
    check("cracks are the block state since 1.21.11", level.getBlockState(at(0, 0, 4)).getValue(ICrackableBlock.CRACKS) == 1);
    check("placing a cracked item keeps the cracks", () -> {
      BlockState placed = Registration.TERRACOTTA_FAUCET.get().withItemCracks(Registration.TERRACOTTA_FAUCET.get().defaultBlockState(), new net.minecraft.world.item.context.BlockPlaceContext(level, null, net.minecraft.world.InteractionHand.MAIN_HAND,
        CrackableBlockItem.setCracks(new ItemStack(Registration.TERRACOTTA_FAUCET), 4), new net.minecraft.world.phys.BlockHitResult(at(0, 0, 0).getCenter(), Direction.UP, at(0, 0, 0), false)) {});
      return placed != null && placed.getValue(ICrackableBlock.CRACKS) == 4;
    });
    level.setBlock(at(2, 0, 4), Registration.PORCELAIN_CISTERN.get(net.minecraft.world.item.DyeColor.WHITE).defaultBlockState(), Block.UPDATE_ALL);
    fill(level, at(2, 0, 4), new FluidStack(Fluids.LAVA, 1000));
    check("lava does not crack a porcelain cistern", level.getBlockEntity(at(2, 0, 4)) instanceof CisternBlockEntity c && c.getCracksHandler().getCracks() == 0);
    check("porcelain cistern stays uncracked in the state", level.getBlockState(at(2, 0, 4)).getValue(ICrackableBlock.CRACKS) == 0);
    // drops keep the cracks
    check("cracked cistern drops keep the cracks", () -> {
      BlockState state = level.getBlockState(at(0, 0, 4));
      BlockEntity be = level.getBlockEntity(at(0, 0, 4));
      List<ItemStack> drops = Block.getDrops(state, level, at(0, 0, 4), be);
      return drops.size() == 1 && CrackableBlockItem.getCracks(drops.get(0)) == 1;
    });
    // save and load keeps the fluid and cracks
    check("cistern save/load keeps fluid and cracks", () -> {
      BlockEntity be = level.getBlockEntity(at(0, 0, 4));
      CompoundTag nbt = be.saveWithFullMetadata(level.registryAccess());
      BlockEntity loaded = BlockEntity.loadStatic(at(0, 0, 4), be.getBlockState(), nbt, level.registryAccess());
      return loaded instanceof CisternBlockEntity c && c.getFluid().getAmount() == 1000 && c.getCracksHandler().getCracks() == 1;
    });
    // block entities saved by 1.21.1 keep the cracks in the tag, read once into the state
    check("1.21.1 cistern data is read (fluid, cracks)", () -> {
      CompoundTag nbt = new CompoundTag();
      CompoundTag fluid = new CompoundTag();
      fluid.putString("id", "minecraft:water");
      fluid.putInt("amount", 3000);
      nbt.put("fluid", fluid);
      nbt.putInt("cracks", 2);
      CisternBlockEntity cistern = new CisternBlockEntity(at(0, 0, 4), Registration.TERRACOTTA_CISTERN.get().defaultBlockState(), true);
      try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(com.mojang.logging.LogUtils.getLogger())) {
        cistern.loadWithComponents(TagValueInput.create(reporter, level.registryAccess(), nbt));
      }
      return cistern.getFluid().getAmount() == 3000;
    });
  }

  private static void worldChecks(ServerLevel level) {
    // tower
    IFluidHandler base = handler(level, at(0, 0, 0), Direction.UP);
    check("tower base holds the fluid", base != null && amount(base) == 12000);
    check("tower extension render index", level.getBlockEntity(at(0, 2, 0)) instanceof CisternBlockEntity c && c.getRenderIndex() == 2);
    // break the middle: the top becomes a new base with the fluid above the break
    level.destroyBlock(at(0, 1, 0), false);
    check("breaking the middle makes the top a base", level.getBlockState(at(0, 2, 0)).hasProperty(CisternBlock.EXTENSION) && !level.getBlockState(at(0, 2, 0)).getValue(CisternBlock.EXTENSION));
    IFluidHandler top = handler(level, at(0, 2, 0), Direction.UP);
    check("new base keeps the fluid above the break", top != null && amount(top) == 4000);
    check("old base shrinks to one layer", base != null && amount(base) == 4000);
    // placing a cistern back merges
    placeCistern(level, at(0, 1, 0));
    IFluidHandler merged = handler(level, at(0, 2, 0), Direction.UP);
    check("placing the middle back merges the tower", merged != null && amount(merged) == 8000 && merged.getTankCapacity(0) == 12000);

    // faucet moved fluid down
    IFluidHandler poured = handler(level, at(5, 0, 0), Direction.UP);
    check("faucet poured water into the cistern below (" + (poured == null ? -1 : amount(poured)) + ")", poured != null && amount(poured) == 2000);

    // channel moved its 100 down
    IFluidHandler below = handler(level, at(8, 0, 0), Direction.UP);
    IFluidHandler side = handler(level, at(9, 0, 0), Direction.UP);
    String state = " channel=" + (level.getBlockEntity(at(8, 1, 0)) instanceof knightminer.ceramics.blocks.entity.ChannelBlockEntity ch ? ch.getFluid().getAmount() : -1)
      + " side cistern=" + (side == null ? -1 : amount(side)) + " state=" + level.getBlockState(at(8, 1, 0));
    check("channel flowed down into the cistern (" + (below == null ? -1 : amount(below)) + state + ")", below != null && amount(below) == 100);
    check("channel side capability follows the connection", handler(level, at(9, 1, 0), Direction.WEST) != null
      && handler(level, at(9, 1, 0), Direction.NORTH) == null);
  }

  private static void placeCistern(ServerLevel level, BlockPos pos) {
    BlockState state = Registration.TERRACOTTA_CISTERN.get().defaultBlockState();
    boolean extension = level.getBlockState(pos.below()).is(state.getBlock());
    level.setBlock(pos, state.setValue(CisternBlock.EXTENSION, extension), Block.UPDATE_ALL);
    // the structure is linked in setPlacedBy, as when a player places it
    state.getBlock().setPlacedBy(level, pos, level.getBlockState(pos), null, new ItemStack(Registration.TERRACOTTA_CISTERN));
  }

  private static void fill(ServerLevel level, BlockPos pos, FluidStack fluid) {
    IFluidHandler handler = handler(level, pos, Direction.UP);
    if (handler != null) {
      fill(handler, fluid);
    }
  }

  private static IFluidHandler handler(ServerLevel level, BlockPos pos, Direction side) {
    return KilnworkPlatform.get().fluidHandler(level, pos, side);
  }

  /** Fills the handler, returns the amount filled */
  private static int fill(IFluidHandler handler, FluidStack fluid) {
    return handler.fill(fluid, FluidAction.EXECUTE);
  }

  private static long amount(IFluidHandler handler) {
    return handler.getFluidInTank(0).getAmount();
  }

  /** Armor value of an armor item, from its attribute modifiers component */
  private static double armor(ItemStack stack) {
    return stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, net.minecraft.world.item.component.ItemAttributeModifiers.EMPTY).modifiers().stream()
      .filter(entry -> entry.attribute().is(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR))
      .mapToDouble(entry -> entry.modifier().amount()).sum();
  }
}

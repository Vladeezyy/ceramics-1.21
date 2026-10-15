package knightminer.ceramics.selftest;

import com.mojang.datafixers.util.Pair;
import knightminer.ceramics.Ceramics;
import knightminer.ceramics.Registration;
import knightminer.ceramics.blocks.ChannelBlock;
import knightminer.ceramics.blocks.ChannelBlock.ChannelConnection;
import knightminer.ceramics.blocks.CisternBlock;
import knightminer.ceramics.blocks.FaucetBlock;
import knightminer.ceramics.blocks.KilnBlock;
import knightminer.ceramics.blocks.RainbowPorcelain;
import knightminer.ceramics.blocks.entity.CrackableBlockEntityHandler.ICrackableBlockEntity;
import knightminer.ceramics.blocks.entity.FaucetBlockEntity;
import knightminer.ceramics.items.BaseClayBucketItem;
import knightminer.ceramics.items.CrackableBlockItem;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Client scene, only with -Dceramics.scene=true (./gradlew runScene, tools/scenetest.sh): builds a stage with the blocks,
 * fluids and buckets of the mod, takes run/screenshots/scene_*.png, logs "[scene] CHECK" lines and quits.
 */
@EventBusSubscriber(modid = Ceramics.MOD_ID, value = Dist.CLIENT)
public final class ClientScene {
  private static final boolean ENABLED = Boolean.getBoolean("ceramics.scene");
  private static final String WORLD = "ceramics_scene";
  private static final List<Pair<Integer, Consumer<Minecraft>>> STEPS = new ArrayList<>();
  private static boolean opened;
  private static int tick;
  private static int failures;
  private static BlockPos stage;

  static {
    at(40, mc -> server(mc, ClientScene::setup));
    at(45, mc -> mc.options.hideGui = true);
    at(60, mc -> server(mc, ClientScene::buildStage));
    at(80, mc -> server(mc, ClientScene::startFlow));
    at(150, ClientScene::flowChecks);
    at(200, mc -> shot(mc, "stage"));
    at(205, mc -> server(mc, p -> look(p, -0.5, 1.0, -1.5, 180, 89)));
    at(240, mc -> shot(mc, "cisterns"));
    at(245, mc -> server(mc, p -> lookAt(p, 5.5, 1.3, 0.3, 5.0, 0.6, -2.0)));
    at(280, mc -> shot(mc, "faucet"));
    at(285, mc -> server(mc, p -> lookAt(p, 9.0, 1.2, 2.3, 9.0, 0.0, 0.0)));
    at(320, mc -> shot(mc, "channels"));
    at(325, mc -> server(mc, p -> lookAt(p, -6.5, 0.9, 2.8, -6.5, 0.4, 0.0)));
    at(360, mc -> shot(mc, "cracks"));
    at(365, mc -> server(mc, p -> lookAt(p, -4.0, 2.2, -1.5, -4.0, 1.0, -6.0)));
    at(400, mc -> shot(mc, "bricks"));
    // hotbar with buckets
    at(405, mc -> server(mc, ClientScene::hotbar));
    at(410, mc -> mc.options.hideGui = false);
    at(440, mc -> { mc.getToasts().clear(); shot(mc, "buckets"); });
    at(445, ClientScene::clientChecks);
    // creative tab
    at(450, mc -> mc.setScreen(new CreativeModeInventoryScreen(mc.player, mc.player.connection.enabledFeatures(), true)));
    at(455, mc -> {
      if (mc.screen instanceof CreativeModeInventoryScreen screen) {
        try {
          var select = CreativeModeInventoryScreen.class.getDeclaredMethod("selectTab", net.minecraft.world.item.CreativeModeTab.class);
          select.setAccessible(true);
          select.invoke(screen, net.minecraft.core.registries.BuiltInRegistries.CREATIVE_MODE_TAB.get(Ceramics.getResource(Ceramics.MOD_ID)));
        } catch (ReflectiveOperationException e) {
          Ceramics.LOG.error("[scene] cannot select the creative tab", e);
        }
      }
    });
    at(480, mc -> shot(mc, "creative_tab"));
    at(485, mc -> mc.setScreen(null));
    // JEI: kiln recipes (hidden by 1.8.0 of the original)
    at(490, ClientScene::showJei);
    at(520, mc -> shot(mc, "jei_kiln"));
    at(525, mc -> mc.setScreen(null));
    at(530, mc -> {
      Ceramics.LOG.info("[scene] DONE, failed checks: " + failures);
      mc.stop();
    });
  }

  private static void at(int t, Consumer<Minecraft> action) {
    STEPS.add(Pair.of(t, action));
  }

  private static void check(String name, boolean ok) {
    if (!ok) {
      failures++;
    }
    Ceramics.LOG.info("[scene] CHECK " + (ok ? "ok   " : "FAIL ") + name);
  }

  @SubscribeEvent
  static void onTick(ClientTickEvent.Post event) {
    if (!ENABLED) {
      return;
    }
    Minecraft mc = Minecraft.getInstance();
    // a fresh game directory starts with the accessibility onboarding screen instead of the title screen
    if (!opened && mc.screen instanceof net.minecraft.client.gui.screens.AccessibilityOnboardingScreen) {
      mc.options.onboardAccessibility = false;
      mc.options.save();
      mc.setScreen(new TitleScreen());
      return;
    }
    if (!opened && mc.screen instanceof TitleScreen) {
      opened = true;
      if (mc.getLevelSource().levelExists(WORLD)) {
        mc.createWorldOpenFlows().openWorld(WORLD, () -> mc.setScreen(new TitleScreen()));
      } else {
        LevelSettings settings = new LevelSettings(WORLD, GameType.CREATIVE, false, Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT);
        mc.createWorldOpenFlows().createFreshLevel(WORLD, settings, new WorldOptions(20261015L, false, false), WorldPresets::createNormalWorldDimensions, new TitleScreen());
      }
      return;
    }
    if (mc.player == null || mc.getSingleplayerServer() == null) {
      return;
    }
    mc.options.pauseOnLostFocus = false;
    mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
    if (mc.screen instanceof PauseScreen) {
      mc.setScreen(null);
    }
    tick++;
    for (Pair<Integer, Consumer<Minecraft>> step : STEPS) {
      if (step.getFirst() == tick) {
        Ceramics.LOG.info("[scene] tick " + tick);
        step.getSecond().accept(mc);
      }
    }
  }

  private static void server(Minecraft mc, Consumer<ServerPlayer> action) {
    MinecraftServer server = mc.getSingleplayerServer();
    server.execute(() -> action.accept(server.getPlayerList().getPlayer(mc.player.getUUID())));
  }

  private static void shot(Minecraft mc, String name) {
    Screenshot.grab(mc.gameDirectory, "scene_" + name + ".png", mc.getMainRenderTarget(), c -> Ceramics.LOG.info("[scene] " + c.getString()));
  }

  private static void setup(ServerPlayer player) {
    MinecraftServer server = player.getServer();
    player.setGameMode(GameType.CREATIVE);
    server.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
    server.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
    server.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false, server);
    server.getGameRules().getRule(GameRules.RULE_RANDOMTICKING).set(0, server);
    player.serverLevel().setDayTime(6000);
    player.serverLevel().setWeatherParameters(6000, 0, false, false);
  }

  private static BlockPos at(int x, int y, int z) {
    return stage.offset(x, y, z);
  }

  private static void set(ServerLevel level, BlockPos pos, BlockState state) {
    level.setBlock(pos, state, Block.UPDATE_ALL);
  }

  private static void cistern(ServerLevel level, BlockPos pos, Block block) {
    boolean extension = level.getBlockState(pos.below()).is(block);
    set(level, pos, block.defaultBlockState().setValue(CisternBlock.EXTENSION, extension));
    block.setPlacedBy(level, pos, level.getBlockState(pos), null, new ItemStack(block));
  }

  private static void fill(ServerLevel level, BlockPos pos, FluidStack fluid) {
    IFluidHandler handler = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, Direction.UP);
    if (handler != null) {
      handler.fill(fluid, FluidAction.EXECUTE);
    }
  }

  /** Flat stage at spawn, the camera looks north */
  private static void buildStage(ServerPlayer player) {
    ServerLevel level = player.serverLevel();
    BlockPos spawn = level.getSharedSpawnPos();
    int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, spawn.getX(), spawn.getZ());
    stage = new BlockPos(spawn.getX(), y, spawn.getZ());
    for (int dx = -10; dx <= 12; dx++) {
      for (int dz = -8; dz <= 8; dz++) {
        set(level, at(dx, -1, dz), Blocks.SMOOTH_STONE.defaultBlockState());
        for (int dy = 0; dy <= 8; dy++) {
          set(level, at(dx, dy, dz), Blocks.AIR.defaultBlockState());
        }
      }
    }

    // back row: building blocks
    int x = -9;
    for (Block block : List.of(Registration.DARK_BRICKS.get(), Registration.LAVA_BRICKS.get(), Registration.DRAGON_BRICKS.get(),
                               Registration.PORCELAIN_BRICKS.get(), Registration.MONOCHROME_BRICKS.get(), Registration.GOLDEN_BRICKS.get(),
                               Registration.MARINE_BRICKS.get(), Registration.RAINBOW_BRICKS.get())) {
      set(level, at(x, 0, -6), block.defaultBlockState());
      set(level, at(x, 1, -6), block.defaultBlockState());
      x++;
    }
    for (DyeColor color : DyeColor.values()) {
      set(level, at(x, 0, -6), Registration.PORCELAIN_BLOCK.get(color).defaultBlockState());
      x++;
    }
    x = -9;
    for (RainbowPorcelain color : RainbowPorcelain.values()) {
      set(level, at(x, 2, -6), Registration.RAINBOW_PORCELAIN.get(color).defaultBlockState());
      x++;
    }
    set(level, at(1, 0, -5), Registration.DARK_BRICKS.getStairs().defaultBlockState());
    set(level, at(2, 0, -5), Registration.PORCELAIN_BRICKS.getSlab().defaultBlockState());
    set(level, at(3, 0, -5), Registration.MARINE_BRICKS.getWall().defaultBlockState());
    // lit kiln
    set(level, at(5, 0, -4), Registration.KILN.get().defaultBlockState().setValue(KilnBlock.LIT, true));

    // tower of three terracotta cisterns, filled to the middle of the top layer
    for (int i = 0; i < 3; i++) {
      cistern(level, at(-3, i, -2), Registration.TERRACOTTA_CISTERN.get());
    }
    fill(level, at(-3, 0, -2), new FluidStack(Fluids.WATER, 10000));
    // colored and porcelain cisterns with lava
    cistern(level, at(-1, 0, -2), Registration.COLORED_CISTERN.get(DyeColor.BLUE));
    fill(level, at(-1, 0, -2), new FluidStack(Fluids.WATER, 3000));
    cistern(level, at(1, 0, -2), Registration.PORCELAIN_CISTERN.get(DyeColor.WHITE));
    cistern(level, at(1, 1, -2), Registration.PORCELAIN_CISTERN.get(DyeColor.WHITE));
    fill(level, at(1, 0, -2), new FluidStack(Fluids.LAVA, 9000));
    // gauge on the tower
    set(level, at(-3, 1, -1), Registration.TERRACOTTA_GAUGE.get().defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING, Direction.SOUTH));

    // faucet: lava cistern (porcelain) -> porcelain faucet -> porcelain cistern below
    cistern(level, at(4, 1, -2), Registration.PORCELAIN_CISTERN.get(DyeColor.ORANGE));
    set(level, at(4, 0, -2), Blocks.SMOOTH_STONE.defaultBlockState());
    fill(level, at(4, 1, -2), new FluidStack(Fluids.LAVA, 6000));
    set(level, at(5, 1, -2), Registration.PORCELAIN_FAUCET.get().defaultBlockState().setValue(FaucetBlock.FACING, Direction.EAST));
    cistern(level, at(5, 0, -2), Registration.PORCELAIN_CISTERN.get(DyeColor.BLACK));

    // channels: water cistern -> terracotta faucet -> channel -> channel -> down into a cistern
    cistern(level, at(7, 1, 0), Registration.TERRACOTTA_CISTERN.get());
    set(level, at(7, 0, 0), Blocks.SMOOTH_STONE.defaultBlockState());
    fill(level, at(7, 1, 0), new FluidStack(Fluids.WATER, 4000));
    set(level, at(8, 1, 0), Registration.TERRACOTTA_FAUCET.get().defaultBlockState().setValue(FaucetBlock.FACING, Direction.EAST));
    set(level, at(8, 0, 0), Registration.TERRACOTTA_CHANNEL.get().defaultBlockState().setValue(ChannelBlock.EAST, ChannelConnection.OUT));
    set(level, at(9, 0, 0), Registration.TERRACOTTA_CHANNEL.get().defaultBlockState().setValue(ChannelBlock.WEST, ChannelConnection.IN).setValue(ChannelBlock.EAST, ChannelConnection.OUT));
    set(level, at(10, 0, 0), Registration.PORCELAIN_CHANNEL.get().defaultBlockState().setValue(ChannelBlock.WEST, ChannelConnection.IN).setValue(ChannelBlock.DOWN, true));
    set(level, at(10, -1, 0), Blocks.AIR.defaultBlockState());
    cistern(level, at(10, -1, 0), Registration.PORCELAIN_CISTERN.get(DyeColor.LIGHT_BLUE));

    // cracks: terracotta cisterns, faucet and channel at each stage
    for (int i = 0; i <= 5; i++) {
      BlockPos pos = at(-9 + i, 0, 0);
      cistern(level, pos, Registration.TERRACOTTA_CISTERN.get());
      if (level.getBlockEntity(pos) instanceof ICrackableBlockEntity crackable) {
        crackable.getCracksHandler().setCracks(i);
      }
    }
    set(level, at(-9, 0, 2), Registration.TERRACOTTA_CHANNEL.get().defaultBlockState().setValue(ChannelBlock.EAST, ChannelConnection.OUT));
    set(level, at(-8, 0, 2), Registration.TERRACOTTA_CHANNEL.get().defaultBlockState().setValue(ChannelBlock.WEST, ChannelConnection.IN));
    for (BlockPos pos : List.of(at(-9, 0, 2), at(-8, 0, 2))) {
      if (level.getBlockEntity(pos) instanceof ICrackableBlockEntity crackable) {
        crackable.getCracksHandler().setCracks(4);
      }
    }
    // unfired versions
    set(level, at(-6, 0, 2), Registration.CLAY_CISTERN.get().defaultBlockState());
    set(level, at(-5, 0, 2), Registration.UNFIRED_CISTERN.get().defaultBlockState());
    set(level, at(-4, 0, 2), Registration.UNFIRED_PORCELAIN_BLOCK.get().defaultBlockState());

    look(player, 0.5, 2.4, 7.5, 180, 18);
  }

  /** Starts the faucets once the structure exists */
  private static void startFlow(ServerPlayer player) {
    ServerLevel level = player.serverLevel();
    for (BlockPos pos : List.of(at(5, 1, -2), at(8, 1, 0))) {
      if (level.getBlockEntity(pos) instanceof FaucetBlockEntity faucet) {
        faucet.activate();
      }
    }
  }

  /** Checks while the faucets are still pouring */
  private static void flowChecks(Minecraft mc) {
    for (BlockPos pos : List.of(at(-1, 0, -2), at(5, 0, -2), at(-3, 0, -2), at(-3, 2, -2), at(1, 1, -2))) {
      if (mc.level.getBlockEntity(pos) instanceof knightminer.ceramics.blocks.entity.CisternBlockEntity cistern) {
        Ceramics.LOG.info("[scene] client cistern {} fluid {} x{} index {} state {}", pos.subtract(stage), cistern.getPublicHandler().getFluidInTank(0).getFluid(),
          cistern.getPublicHandler().getFluidInTank(0).getAmount(), cistern.getRenderIndex(), mc.level.getBlockState(pos));
      }
    }
    check("faucet is pouring on the client", mc.level.getBlockEntity(at(5, 1, -2)) instanceof FaucetBlockEntity faucet && faucet.isPouring() && !faucet.getRenderFluid().isEmpty());
    check("channel has fluid on the client", mc.level.getBlockEntity(at(9, 0, 0)) instanceof knightminer.ceramics.blocks.entity.ChannelBlockEntity channel && !channel.getFluid().isEmpty());
  }

  /** Teleports the player to the camera position (relative to the stage) looking at the target */
  private static void lookAt(ServerPlayer player, double x, double y, double z, double tx, double ty, double tz) {
    double dx = tx - x, dy = ty - (y + player.getEyeHeight()), dz = tz - z;
    float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
    float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
    look(player, x, y, z, yaw, pitch);
  }

  private static void look(ServerPlayer player, double x, double y, double z, float yaw, float pitch) {
    player.teleportTo(player.serverLevel(), stage.getX() + x, stage.getY() + y, stage.getZ() + z, yaw, pitch);
  }

  private static void hotbar(ServerPlayer player) {
    look(player, 0.5, 2.4, 7.5, 180, 18);
    List<ItemStack> items = List.of(
      new ItemStack(Registration.EMPTY_CLAY_BUCKET.get()),
      BaseClayBucketItem.withFluid(Fluids.WATER, false),
      BaseClayBucketItem.withFluid(Fluids.LAVA, false),
      new ItemStack(Registration.MILK_CLAY_BUCKET.get()),
      BaseClayBucketItem.withBlock(Blocks.POWDER_SNOW, false),
      new ItemStack(Registration.CRACKED_EMPTY_CLAY_BUCKET.get()),
      CrackableBlockItem.setCracks(new ItemStack(Registration.TERRACOTTA_CISTERN), 3),
      new ItemStack(Registration.CLAY_CHESTPLATE.get()),
      new ItemStack(Registration.KILN));
    for (int i = 0; i < items.size(); i++) {
      player.getInventory().setItem(i, items.get(i));
    }
    player.getInventory().selected = 1;
  }

  private static void clientChecks(Minecraft mc) {
    var renderer = mc.getItemRenderer();
    BakedModel empty = renderer.getModel(new ItemStack(Registration.FLUID_CLAY_BUCKET.get()), mc.level, mc.player, 0);
    BakedModel water = renderer.getModel(BaseClayBucketItem.withFluid(Fluids.WATER, false), mc.level, mc.player, 0);
    BakedModel snow = renderer.getModel(BaseClayBucketItem.withBlock(Blocks.POWDER_SNOW, false), mc.level, mc.player, 0);
    check("water clay bucket has its own model", water != empty && water != mc.getModelManager().getMissingModel());
    check("powder snow bucket has its own model", snow != mc.getModelManager().getMissingModel());
    BakedModel whole = renderer.getModel(new ItemStack(Registration.TERRACOTTA_CISTERN), mc.level, mc.player, 0);
    BakedModel cracked = renderer.getModel(CrackableBlockItem.setCracks(new ItemStack(Registration.TERRACOTTA_CISTERN), 3), mc.level, mc.player, 0);
    check("cracked cistern item uses the cracked model", whole != cracked);
    check("bucket names resolve", !BaseClayBucketItem.withFluid(Fluids.WATER, false).getHoverName().getString().contains("ceramics")
      && !new ItemStack(Registration.CRACKED_EMPTY_CLAY_BUCKET.get()).getHoverName().getString().contains("ceramics"));
  }

  private static void showJei(Minecraft mc) {
    if (SceneJeiPlugin.runtime == null) {
      check("JEI runtime available", false);
      return;
    }
    RecipeType<RecipeHolder<knightminer.ceramics.recipe.KilnRecipe>> type = RecipeType.createRecipeHolderType(Ceramics.getResource("kiln"));
    long count = SceneJeiPlugin.runtime.getRecipeManager().createRecipeLookup(type).get().count();
    check("JEI shows kiln recipes (" + count + ")", count > 50);
    SceneJeiPlugin.runtime.getRecipesGui().showTypes(List.of(type));
  }
}

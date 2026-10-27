package knightminer.ceramics.datagen;

import knightminer.ceramics.Ceramics;
import knightminer.ceramics.Registration;
import knightminer.ceramics.recipe.CeramicsTags;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import dev.vladeezyy.kilnwork.tag.KilnworkTags;
import dev.vladeezyy.kilnwork.registration.EnumObject;
import dev.vladeezyy.kilnwork.platform.Registrar;
import dev.vladeezyy.kilnwork.registration.BuildingBlocks;

import java.util.concurrent.CompletableFuture;

public class BlockTagProvider extends BlockTagsProvider {
  public BlockTagProvider(PackOutput packOutput, CompletableFuture<Provider> lookupProvider) {
    super(packOutput, lookupProvider, Ceramics.MOD_ID);
  }

  @Override
  public String getName() {
    return "Ceramics Block Tags";
  }

  @Override
  protected void addTags(Provider provider) {
    // vanilla colored terracotta
    ObjectTagAppender<Block> coloredTerracotta = this.objects(CeramicsTags.Blocks.COLORED_TERRACOTTA);
    Registration.TERRACOTTA.values().forEach(coloredTerracotta::add);

    this.objects(CeramicsTags.Blocks.BUCKET_CRACKING_BLOCKS);

    // porcelain
    this.objects(BlockTags.ENDERMAN_HOLDABLE).add(Registration.UNFIRED_PORCELAIN_BLOCK.get());
    ObjectTagAppender<Block> coloredPorcelain = this.objects(CeramicsTags.Blocks.COLORED_PORCELAIN);
    Registration.PORCELAIN_BLOCK.forEach((color, block) -> {
      if (color != DyeColor.WHITE) {
        coloredPorcelain.add(block);
      }
    });
    this.objects(CeramicsTags.Blocks.PORCELAIN)
        .add(Registration.PORCELAIN_BLOCK.get(DyeColor.WHITE))
        .addTag(CeramicsTags.Blocks.COLORED_PORCELAIN);

    // rainbow porcelain
    ObjectTagAppender<Block> rainbow = this.objects(CeramicsTags.Blocks.RAINBOW_PORCELAIN);
    Registration.RAINBOW_PORCELAIN.values().forEach(rainbow::add);

    // bricks
    this.objects(CeramicsTags.Blocks.BRICKS).add(
        // clay
        Blocks.BRICKS,
        Registration.DARK_BRICKS.get(),
        Registration.DRAGON_BRICKS.get(),
        Registration.LAVA_BRICKS.get(),
        // porcelain
        Registration.PORCELAIN_BRICKS.get(),
        Registration.GOLDEN_BRICKS.get(),
        Registration.MARINE_BRICKS.get(),
        Registration.MONOCHROME_BRICKS.get(),
        Registration.RAINBOW_BRICKS.get()
    );
    this.objects(BlockTags.WALLS).add(
        // clay
        Registration.DARK_BRICKS.getWall(),
        Registration.DRAGON_BRICKS.getWall(),
        Registration.LAVA_BRICKS.getWall(),
        // porcelain
        Registration.PORCELAIN_BRICKS.getWall(),
        Registration.GOLDEN_BRICKS.getWall(),
        Registration.MARINE_BRICKS.getWall(),
        Registration.MONOCHROME_BRICKS.getWall(),
        Registration.RAINBOW_BRICKS.getWall()
    );
    // blocks that cisterns connect to
    this.objects(CeramicsTags.Blocks.CISTERN_CONNECTIONS)
        .add(Registration.CLAY_FAUCET.get(), Registration.UNFIRED_FAUCET.get(), Registration.TERRACOTTA_FAUCET.get(), Registration.PORCELAIN_FAUCET.get(),
             Registration.CLAY_CHANNEL.get(), Registration.UNFIRED_CHANNEL.get(), Registration.TERRACOTTA_CHANNEL.get(), Registration.PORCELAIN_CHANNEL.get())
      .addTag(KilnworkTags.Blocks.ATTACHED_GAUGES);
    // list of all terracotta cisterns
    ObjectTagAppender<Block> terracottaCisterns = this.objects(CeramicsTags.Blocks.TERRACOTTA_CISTERNS)
                                                         .add(Registration.TERRACOTTA_CISTERN.get());
    Registration.COLORED_CISTERN.forEach(block -> terracottaCisterns.add(block));
    ObjectTagAppender<Block> porcelainCisterns = this.objects(CeramicsTags.Blocks.PORCELAIN_CISTERNS);
    Registration.PORCELAIN_CISTERN.forEach(block -> porcelainCisterns.add(block));

    this.objects(KilnworkTags.Blocks.ATTACHED_GAUGES).add(Registration.TERRACOTTA_GAUGE.get(), Registration.PORCELAIN_GAUGE.get());
    // channels and faucets do not sync the full fluid amount, in 1.20 this was a block entity tag
    this.objects(KilnworkTags.Blocks.HIDES_GAUGE_AMOUNT).add(Registration.TERRACOTTA_FAUCET.get(), Registration.PORCELAIN_FAUCET.get(), Registration.TERRACOTTA_CHANNEL.get(), Registration.PORCELAIN_CHANNEL.get());

    // pickaxe
    addTag(BlockTags.MINEABLE_WITH_PICKAXE, Registration.PORCELAIN_BLOCK, Registration.RAINBOW_PORCELAIN);
    addTag(BlockTags.MINEABLE_WITH_PICKAXE, Registration.KILN);
    addTag(BlockTags.MINEABLE_WITH_PICKAXE, Registration.TERRACOTTA_GAUGE, Registration.PORCELAIN_GAUGE);
    addTag(BlockTags.MINEABLE_WITH_PICKAXE, Registration.TERRACOTTA_CISTERN);
    addTag(BlockTags.MINEABLE_WITH_PICKAXE, Registration.PORCELAIN_CISTERN, Registration.COLORED_CISTERN);
    addTag(BlockTags.MINEABLE_WITH_PICKAXE, Registration.TERRACOTTA_FAUCET, Registration.PORCELAIN_FAUCET);
    addTag(BlockTags.MINEABLE_WITH_PICKAXE, Registration.TERRACOTTA_CHANNEL, Registration.PORCELAIN_CHANNEL);
    addTag(BlockTags.MINEABLE_WITH_PICKAXE,
           Registration.DARK_BRICKS, Registration.LAVA_BRICKS, Registration.DRAGON_BRICKS,
           Registration.PORCELAIN_BRICKS, Registration.MONOCHROME_BRICKS, Registration.GOLDEN_BRICKS,
           Registration.MARINE_BRICKS, Registration.RAINBOW_BRICKS);
    addTag(Tags.Blocks.NEEDS_GOLD_TOOL, Registration.GOLDEN_BRICKS);
    // in 1.20 Forge sorted gold between wood and stone, so this tag kept wooden tools from mining golden bricks.
    // NeoForge 1.21 does not wire the tag into the vanilla tiers, so say it directly
    addTag(BlockTags.INCORRECT_FOR_WOODEN_TOOL, Registration.GOLDEN_BRICKS);
    // shovel
    addTag(BlockTags.MINEABLE_WITH_SHOVEL, Registration.UNFIRED_PORCELAIN_BLOCK);
    addTag(BlockTags.MINEABLE_WITH_SHOVEL, Registration.CLAY_CISTERN, Registration.UNFIRED_CISTERN);
    addTag(BlockTags.MINEABLE_WITH_SHOVEL, Registration.CLAY_FAUCET, Registration.UNFIRED_FAUCET);
    addTag(BlockTags.MINEABLE_WITH_SHOVEL, Registration.CLAY_CHANNEL, Registration.UNFIRED_CHANNEL);
  }

  @SafeVarargs
  private <T extends Block> void addTag(TagKey<Block> tag, Registrar.Entry<Block, T>... blocks) {
    ObjectTagAppender<Block> appender = this.objects(tag);
    for (Registrar.Entry<Block, T> block : blocks) {
      appender.add(block.get());
    }
  }

  @SafeVarargs
  private<T extends Block> void addTag(TagKey<Block> tag, EnumObject<?, T>... blocks) {
    ObjectTagAppender<Block> appender = this.objects(tag);
    for (EnumObject<?, T> object : blocks) {
      object.forEach(block -> appender.add(block));
    }
  }

  private void addTag(TagKey<Block> tag, BuildingBlocks... blocks) {
    ObjectTagAppender<Block> appender = this.objects(tag);
    for (BuildingBlocks object : blocks) {
      object.values().forEach(appender::add);
    }
  }

  /** Appender taking the blocks themselves */
  private ObjectTagAppender<Block> objects(TagKey<Block> tag) {
    return new ObjectTagAppender<>(this.tag(tag), value -> value.builtInRegistryHolder().key());
  }
}

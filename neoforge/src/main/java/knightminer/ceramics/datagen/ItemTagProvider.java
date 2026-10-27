package knightminer.ceramics.datagen;

import net.minecraft.tags.TagKey;
import knightminer.ceramics.Ceramics;
import knightminer.ceramics.Registration;
import knightminer.ceramics.recipe.CeramicsTags;
import knightminer.ceramics.recipe.CeramicsTags.Blocks;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.BlockTagCopyingItemTagProvider;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

import java.util.concurrent.CompletableFuture;

public class ItemTagProvider extends BlockTagCopyingItemTagProvider {
  public ItemTagProvider(PackOutput packOutput, CompletableFuture<Provider> lookupProvider, CompletableFuture<TagsProvider.TagLookup<Block>> blockTags) {
    super(packOutput, lookupProvider, blockTags, Ceramics.MOD_ID);
  }

  @Override
  public String getName() {
    return "Ceramics Item Tags";
  }

  @Override
  protected void addTags(Provider provider) {
    this.copy(Blocks.COLORED_TERRACOTTA, CeramicsTags.Items.COLORED_TERRACOTTA);
    // porcelain
    this.copy(Blocks.PORCELAIN, CeramicsTags.Items.PORCELAIN);
    this.copy(Blocks.COLORED_PORCELAIN, CeramicsTags.Items.COLORED_PORCELAIN);
    this.copy(Blocks.RAINBOW_PORCELAIN, CeramicsTags.Items.RAINBOW_PORCELAIN);
    // bricks
    this.copy(Blocks.BRICKS, CeramicsTags.Items.BRICKS);
    // cisterns
    this.copy(Blocks.TERRACOTTA_CISTERNS, CeramicsTags.Items.TERRACOTTA_CISTERNS);
    this.copy(Blocks.PORCELAIN_CISTERNS, CeramicsTags.Items.PORCELAIN_CISTERNS);

    // item unique tags
    this.objects(CeramicsTags.Items.MILK_BUCKETS).add(Items.MILK_BUCKET, Registration.MILK_CLAY_BUCKET.get(), Registration.CRACKED_MILK_CLAY_BUCKET.get());
    this.objects(CeramicsTags.Items.TERRACOTTA_CRACK_REPAIR).add(Items.CLAY_BALL);
    this.objects(CeramicsTags.Items.BRICK_PLATES).add(Registration.CLAY_PLATE.get());
    this.objects(CeramicsTags.Items.PLATES).addTag(CeramicsTags.Items.BRICK_PLATES);
    // armor repair, a tag on the material since 1.21.2
    this.objects(CeramicsTags.Items.REPAIRS_CLAY_ARMOR).add(Registration.CLAY_PLATE.get());

    // buckets
    this.objects(CeramicsTags.Items.EMPTY_CLAY_BUCKETS).add(Registration.EMPTY_CLAY_BUCKET.asItem(), Registration.CRACKED_EMPTY_CLAY_BUCKET.asItem());
    this.objects(CeramicsTags.Items.CLAY_BUCKETS).addTag(CeramicsTags.Items.EMPTY_CLAY_BUCKETS).add(
        Registration.FLUID_CLAY_BUCKET.asItem(), Registration.CRACKED_FLUID_CLAY_BUCKET.asItem(),
        Registration.SOLID_CLAY_BUCKET.asItem(), Registration.CRACKED_SOLID_CLAY_BUCKET.asItem(),
        Registration.MILK_CLAY_BUCKET.asItem(), Registration.CRACKED_MILK_CLAY_BUCKET.asItem());
  }

  /** Appender taking the items themselves */
  private ObjectTagAppender<Item> objects(TagKey<Item> tag) {
    return new ObjectTagAppender<>(this.tag(tag), value -> value.builtInRegistryHolder().key());
  }
}

package knightminer.ceramics.neoforge;

import knightminer.ceramics.Ceramics;
import knightminer.ceramics.client.CeramicsClient;
import knightminer.ceramics.client.model.ClayBucketItemModel;
import knightminer.ceramics.plugin.jei.KilnRecipeSync;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLConstructModEvent;
import net.neoforged.neoforge.client.event.RecipesReceivedEvent;
import net.neoforged.neoforge.client.event.RegisterItemModelsEvent;

/**
 * Client side of Ceramics on NeoForge. Since 1.21.4 the clay buckets use the item model type ceramics:clay_bucket, and
 * since 1.21.11 the cracks are a block state property drawn by the block models. Cutout render types are set in the
 * block model JSON.
 */
@SuppressWarnings("unused")
@EventBusSubscriber(modid = Ceramics.MOD_ID, value = Dist.CLIENT)
public class CeramicsNeoForgeClient {
  @SubscribeEvent
  static void construct(FMLConstructModEvent event) {
    CeramicsClient.init();
  }

  /** Keeps the kiln recipes sent by the server for JEI, before JEI reads the recipes */
  @SubscribeEvent(priority = EventPriority.HIGH)
  static void onRecipesReceived(RecipesReceivedEvent event) {
    KilnRecipeSync.setRecipes(event.getRecipeMap());
  }

  @SubscribeEvent
  static void registerItemModels(RegisterItemModelsEvent event) {
    event.register(ClayBucketItemModel.ID, ClayBucketItemModel.Unbaked.MAP_CODEC);
  }
}

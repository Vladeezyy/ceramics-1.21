package knightminer.ceramics.fabric.client;

import dev.vladeezyy.kilnwork.fabric.client.KilnworkFabricClient;
import knightminer.ceramics.Ceramics;
import knightminer.ceramics.Registration;
import knightminer.ceramics.client.CeramicsClient;
import knightminer.ceramics.plugin.jei.KilnRecipeSync;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.recipe.v1.sync.ClientRecipeSynchronizedEvent;
import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.item.ItemModels;

/**
 * Client entry of Ceramics on Fabric. Since 1.21.11 the cracks are a block state drawn by the block models; the clay
 * buckets use the item model type ceramics:clay_bucket.
 */
public class CeramicsFabricClient implements ClientModInitializer {
  @Override
  public void onInitializeClient() {
    CeramicsClient.init();
    // NeoForge reads the cutout layer from the model JSON
    CeramicsClient.cutoutBlocks().forEach(block -> BlockRenderLayerMap.putBlock(block, ChunkSectionLayer.CUTOUT));
    ItemModels.ID_MAPPER.put(ClayBucketItemModelFabric.ID, ClayBucketItemModelFabric.Unbaked.MAP_CODEC);
    // the kiln recipes the server sends, for JEI
    ClientRecipeSynchronizedEvent.EVENT.register((client, recipes) -> KilnRecipeSync.setRecipes(recipes.getAllOfType(Registration.KILN_RECIPE.get())));
    KilnworkFabricClient.register();
    if (Boolean.getBoolean("ceramics.scene")) {
      try {
        Class.forName("knightminer.ceramics.selftest.fabric.SelfTestFabric").getMethod("initClient").invoke(null);
      } catch (ReflectiveOperationException e) {
        Ceramics.LOG.error("Could not start the client scene", e);
      }
    }
  }
}

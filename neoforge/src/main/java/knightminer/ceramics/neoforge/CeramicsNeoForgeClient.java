package knightminer.ceramics.neoforge;

import knightminer.ceramics.Ceramics;
import knightminer.ceramics.client.CeramicsClient;
import knightminer.ceramics.client.model.ClayBucketModel;
import knightminer.ceramics.client.model.CrackedModel;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLConstructModEvent;
import net.neoforged.neoforge.client.event.ModelEvent.RegisterGeometryLoaders;

/** Client side of Ceramics on NeoForge: the common client registrations, render layers and the model loaders */
@SuppressWarnings("unused")
@EventBusSubscriber(modid = Ceramics.MOD_ID, bus = Bus.MOD, value = Dist.CLIENT)
public class CeramicsNeoForgeClient {
  @SubscribeEvent
  static void construct(FMLConstructModEvent event) {
    CeramicsClient.init();
  }

  @SuppressWarnings("deprecation")
  @SubscribeEvent
  static void setupClient(FMLClientSetupEvent event) {
    RenderType cutout = RenderType.cutout();
    CeramicsClient.cutoutBlocks().forEach(block -> ItemBlockRenderTypes.setRenderLayer(block, cutout));
  }

  @SubscribeEvent
  static void registerModels(RegisterGeometryLoaders event) {
    event.register(Ceramics.getResource("fluid_bucket"), ClayBucketModel.FLUID_LOADER);
    event.register(Ceramics.getResource("solid_bucket"), ClayBucketModel.SOLID_LOADER);
    event.register(Ceramics.getResource("cracked"), CrackedModel.LOADER);
  }
}

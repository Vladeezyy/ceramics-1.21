package knightminer.ceramics;

import knightminer.ceramics.client.model.ClayBucketItemModel;
import knightminer.ceramics.client.renderer.ChannelBlockEntityRenderer;
import knightminer.ceramics.client.renderer.CisternBlockEntityRenderer;
import knightminer.ceramics.client.renderer.CisternFluids;
import knightminer.ceramics.client.renderer.FaucetBlockEntityRenderer;
import knightminer.ceramics.client.screen.KilnScreen;
import knightminer.ceramics.plugin.jei.KilnRecipeSync;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RecipesReceivedEvent;
import net.neoforged.neoforge.client.event.RegisterItemModelsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/**
 * Client registration. Since 1.21.4 the clay buckets use the item model type ceramics:clay_bucket, and since 1.21.11
 * the cracks are a block state property drawn by the block models, so the geometry loaders of 1.20 are gone. Cutout
 * render types are set in the block model JSON.
 */
@SuppressWarnings("unused")
@EventBusSubscriber(modid=Ceramics.MOD_ID, value=Dist.CLIENT)
public class ClientEvents {
  @SubscribeEvent
  static void registerReloadListeners(AddClientReloadListenersEvent event) {
    // faucet and channel fluid shapes are loaded by Kilnwork
    event.addListener(Ceramics.getResource("cistern_fluids"), CisternFluids.REGISTRY);
  }

  /** Keeps the kiln recipes sent by the server for JEI, before JEI reads the recipes */
  @SubscribeEvent(priority = EventPriority.HIGH)
  static void onRecipesReceived(RecipesReceivedEvent event) {
    KilnRecipeSync.onRecipesReceived(event);
  }

  @SubscribeEvent
  static void registerItemModels(RegisterItemModelsEvent event) {
    event.register(ClayBucketItemModel.ID, ClayBucketItemModel.Unbaked.MAP_CODEC);
  }

  @SubscribeEvent
  static void registerScreens(RegisterMenuScreensEvent event) {
    event.register(Registration.KILN_MENU.get(), KilnScreen::new);
  }

  @SubscribeEvent
  static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
    event.registerBlockEntityRenderer(Registration.CISTERN_BLOCK_ENTITY.get(), CisternBlockEntityRenderer::new);
    event.registerBlockEntityRenderer(Registration.FAUCET_BLOCK_ENTITY.get(), FaucetBlockEntityRenderer::new);
    event.registerBlockEntityRenderer(Registration.CHANNEL_BLOCK_ENTITY.get(), ChannelBlockEntityRenderer::new);
  }
}

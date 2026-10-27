package knightminer.ceramics.client;

import dev.vladeezyy.kilnwork.platform.ClientLoaderHooks;
import knightminer.ceramics.Ceramics;
import knightminer.ceramics.Registration;
import knightminer.ceramics.client.renderer.ChannelBlockEntityRenderer;
import knightminer.ceramics.client.renderer.CisternBlockEntityRenderer;
import knightminer.ceramics.client.renderer.CisternFluids;
import knightminer.ceramics.client.renderer.FaucetBlockEntityRenderer;
import knightminer.ceramics.client.screen.KilnScreen;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

/** Client registrations of Ceramics, the same on every loader; the loader parts call {@link #init()} */
public final class CeramicsClient {
  private CeramicsClient() {}

  public static void init() {
    // faucet and channel fluid shapes are loaded by Kilnwork
    ClientLoaderHooks.reloadListener(Ceramics.getResource("cistern_fluids"), CisternFluids.REGISTRY);
    ClientLoaderHooks.menuScreen(Registration.KILN_MENU, KilnScreen::new);
    ClientLoaderHooks.blockEntityRenderer(Registration.CISTERN_BLOCK_ENTITY, CisternBlockEntityRenderer::new);
    ClientLoaderHooks.blockEntityRenderer(Registration.FAUCET_BLOCK_ENTITY, FaucetBlockEntityRenderer::new);
    ClientLoaderHooks.blockEntityRenderer(Registration.CHANNEL_BLOCK_ENTITY, ChannelBlockEntityRenderer::new);
  }

  /** Blocks drawn in the cutout layer; NeoForge reads it from the model JSON, Fabric needs the list */
  public static List<Block> cutoutBlocks() {
    List<Block> blocks = new ArrayList<>();
    blocks.add(Registration.TERRACOTTA_CISTERN.get());
    Registration.COLORED_CISTERN.forEach((java.util.function.Consumer<Block>) blocks::add);
    blocks.add(Registration.TERRACOTTA_FAUCET.get());
    blocks.add(Registration.TERRACOTTA_CHANNEL.get());
    return blocks;
  }
}

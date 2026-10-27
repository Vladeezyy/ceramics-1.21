package knightminer.ceramics.selftest.neoforge;

import knightminer.ceramics.Ceramics;
import knightminer.ceramics.selftest.ClientScene;
import knightminer.ceramics.selftest.SelfTest;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Starts the common tests on NeoForge (dev runs only, not in the jar) */
@EventBusSubscriber(modid = Ceramics.MOD_ID)
public final class SelfTestNeoForge {
  private SelfTestNeoForge() {}

  @SubscribeEvent
  static void serverStarted(ServerStartedEvent event) {
    SelfTest.serverStarted(event.getServer());
  }

  @SubscribeEvent
  static void serverTick(ServerTickEvent.Post event) {
    SelfTest.serverTick(event.getServer());
  }

  @EventBusSubscriber(modid = Ceramics.MOD_ID, value = Dist.CLIENT)
  public static final class Client {
    private Client() {}

    @SubscribeEvent
    static void clientTick(ClientTickEvent.Post event) {
      ClientScene.onTick();
    }
  }
}

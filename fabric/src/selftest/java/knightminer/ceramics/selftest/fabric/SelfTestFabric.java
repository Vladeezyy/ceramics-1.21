package knightminer.ceramics.selftest.fabric;

import knightminer.ceramics.selftest.ClientScene;
import knightminer.ceramics.selftest.SelfTest;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

/** Starts the common tests on Fabric (dev runs only, not in the jar; CeramicsFabric finds it by name) */
public final class SelfTestFabric {
  private SelfTestFabric() {}

  public static void init() {
    ServerLifecycleEvents.SERVER_STARTED.register(SelfTest::serverStarted);
    ServerTickEvents.END_SERVER_TICK.register(SelfTest::serverTick);
  }

  public static void initClient() {
    ClientTickEvents.END_CLIENT_TICK.register(client -> ClientScene.onTick());
  }
}

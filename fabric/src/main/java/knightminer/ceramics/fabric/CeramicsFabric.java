package knightminer.ceramics.fabric;

import dev.vladeezyy.kilnwork.fabric.KilnworkFabric;
import knightminer.ceramics.Ceramics;
import knightminer.ceramics.Registration;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredientSerializer;

/** Fabric entry of Ceramics */
public class CeramicsFabric implements ModInitializer {
  @Override
  public void onInitialize() {
    Ceramics.init();
    CustomIngredientSerializer.register(FabricNoNBTIngredient.SERIALIZER);
    KilnworkFabric.register(Ceramics.MOD_ID);
    Registration.setup();
    // dev test runs only: the test classes are not in the jar
    if (Boolean.getBoolean("ceramics.selftest") || Boolean.getBoolean("ceramics.scene")) {
      try {
        Class.forName("knightminer.ceramics.selftest.fabric.SelfTestFabric").getMethod("init").invoke(null);
      } catch (ReflectiveOperationException e) {
        Ceramics.LOG.error("Could not start the self test", e);
      }
    }
  }
}

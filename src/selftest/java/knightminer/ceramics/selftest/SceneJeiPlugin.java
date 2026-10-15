package knightminer.ceramics.selftest;

import knightminer.ceramics.Ceramics;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.Identifier;

import javax.annotation.Nullable;

/** Keeps the JEI runtime for the client scene, dev runs only */
@JeiPlugin
public class SceneJeiPlugin implements IModPlugin {
  @Nullable
  static IJeiRuntime runtime;

  @Override
  public Identifier getPluginUid() {
    return Ceramics.getResource("scene");
  }

  @Override
  public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
    runtime = jeiRuntime;
  }
}

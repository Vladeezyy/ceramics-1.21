package knightminer.ceramics.platform;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.material.Fluid;

import java.util.ServiceLoader;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/** What Ceramics needs from the mod loader beyond Kilnwork, implemented once per loader (found with {@link ServiceLoader}) */
public interface CeramicsPlatform {
  CeramicsPlatform INSTANCE = ServiceLoader.load(CeramicsPlatform.class, CeramicsPlatform.class.getClassLoader()).findFirst()
    .orElseThrow(() -> new IllegalStateException("No Ceramics platform for this mod loader"));

  static CeramicsPlatform get() {
    return INSTANCE;
  }

  /** The milk fluid, if the loader has one enabled (NeoForge's when a mod enables it), else empty */
  Fluid milkFluid();

  /** Removes the effects milk cures */
  void cureAsMilk(LivingEntity entity);

  /**
   * Sends the recipes of the type to clients when the data is synced (since 1.21.2 clients only get the recipes
   * the server sends; JEI shows them)
   */
  void syncRecipes(Supplier<? extends RecipeType<?>> type, Supplier<? extends RecipeSerializer<?>> serializer);


  /** Calls the listener when the given item is used up by a player (NeoForge's PlayerDestroyItemEvent) */
  void onItemDestroyed(Item item, BiConsumer<Player, ItemStack> listener);
}

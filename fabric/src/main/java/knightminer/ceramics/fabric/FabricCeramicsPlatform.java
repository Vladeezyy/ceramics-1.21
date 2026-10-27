package knightminer.ceramics.fabric;

import knightminer.ceramics.items.FluidClayBucketItem;
import knightminer.ceramics.platform.CeramicsPlatform;
import net.fabricmc.fabric.api.recipe.v1.sync.RecipeSynchronization;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/** {@link CeramicsPlatform} on Fabric */
public class FabricCeramicsPlatform implements CeramicsPlatform {
  record DestroyListener(Item item, BiConsumer<Player, ItemStack> listener) {}
  static final List<DestroyListener> DESTROY_LISTENERS = new ArrayList<>();
  private static final List<Supplier<? extends RecipeSerializer<?>>> SYNCED_SERIALIZERS = new ArrayList<>();

  @Override
  public Fluid milkFluid() {
    // Fabric has no milk fluid of its own
    return Fluids.EMPTY;
  }

  @Override
  public void cureAsMilk(LivingEntity entity) {
    entity.removeAllEffects();
  }

  @Override
  public void syncRecipes(Supplier<? extends RecipeType<?>> type, Supplier<? extends RecipeSerializer<?>> serializer) {
    // Fabric syncs by serializer, once registered (registerRecipeSync); the client side hands them to KilnRecipeSync
    synchronized (SYNCED_SERIALIZERS) {
      SYNCED_SERIALIZERS.add(serializer);
    }
  }

  /** Called by CeramicsFabric after the registration: the serializers asked by {@link #syncRecipes} */
  public static void registerRecipeSync() {
    synchronized (SYNCED_SERIALIZERS) {
      SYNCED_SERIALIZERS.forEach(serializer -> RecipeSynchronization.synchronizeRecipeSerializer(serializer.get()));
      SYNCED_SERIALIZERS.clear();
    }
  }

  @Override
  public int burnTime(ItemStack stack) {
    // the fuel values of the server ask the bucket too (FuelValuesMixin)
    return stack.getItem() instanceof FluidClayBucketItem bucket ? bucket.getBurnTime(stack, null, null) : 0;
  }

  @Override
  public void onItemDestroyed(Item item, BiConsumer<Player, ItemStack> listener) {
    synchronized (DESTROY_LISTENERS) {
      DESTROY_LISTENERS.add(new DestroyListener(item, listener));
    }
  }

  /** Called by the mixins where NeoForge fires PlayerDestroyItemEvent */
  public static void fireItemDestroyed(Player player, ItemStack original) {
    List<DestroyListener> listeners;
    synchronized (DESTROY_LISTENERS) {
      listeners = new ArrayList<>(DESTROY_LISTENERS);
    }
    for (DestroyListener listener : listeners) {
      if (original.is(listener.item())) {
        listener.listener().accept(player, original);
      }
    }
  }
}

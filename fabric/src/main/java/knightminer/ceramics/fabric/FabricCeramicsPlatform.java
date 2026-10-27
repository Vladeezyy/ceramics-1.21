package knightminer.ceramics.fabric;

import knightminer.ceramics.items.FluidClayBucketItem;
import knightminer.ceramics.platform.CeramicsPlatform;
import net.fabricmc.fabric.api.registry.FuelRegistry;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/** {@link CeramicsPlatform} on Fabric */
public class FabricCeramicsPlatform implements CeramicsPlatform {
  record DestroyListener(Item item, BiConsumer<Player, ItemStack> listener) {}
  static final List<DestroyListener> DESTROY_LISTENERS = new ArrayList<>();

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
  public int burnTime(ItemStack stack) {
    if (stack.getItem() instanceof FluidClayBucketItem bucket) {
      return bucket.getBurnTime(stack, null);
    }
    Integer time = FuelRegistry.INSTANCE.get(stack.getItem());
    return time == null ? 0 : time;
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

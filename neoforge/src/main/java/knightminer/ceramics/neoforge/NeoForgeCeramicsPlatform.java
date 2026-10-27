package knightminer.ceramics.neoforge;

import knightminer.ceramics.platform.CeramicsPlatform;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.entity.player.PlayerDestroyItemEvent;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

/** {@link CeramicsPlatform} on NeoForge */
public class NeoForgeCeramicsPlatform implements CeramicsPlatform {
  @Override
  public Fluid milkFluid() {
    return NeoForgeMod.MILK.isBound() ? NeoForgeMod.MILK.get() : Fluids.EMPTY;
  }

  @Override
  public void cureAsMilk(LivingEntity entity) {
    entity.removeAllEffects();
  }

  @Override
  public void syncRecipes(Supplier<? extends RecipeType<?>> type, Supplier<? extends RecipeSerializer<?>> serializer) {
    NeoForge.EVENT_BUS.addListener(OnDatapackSyncEvent.class, event -> event.sendRecipes(type.get()));
  }

  @Override
  public int burnTime(ItemStack stack) {
    return stack.getBurnTime(null, null);
  }

  @Override
  public void onItemDestroyed(Item item, BiConsumer<Player, ItemStack> listener) {
    NeoForge.EVENT_BUS.addListener(PlayerDestroyItemEvent.class, event -> {
      if (event.getOriginal().getItem() == item) {
        listener.accept(event.getEntity(), event.getOriginal());
      }
    });
  }
}

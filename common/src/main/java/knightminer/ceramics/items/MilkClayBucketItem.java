package knightminer.ceramics.items;

import knightminer.ceramics.platform.CeramicsPlatform;
import net.minecraft.world.level.Level;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * Clay bucket holding milk
 */
public class MilkClayBucketItem extends BaseClayBucketItem {
  public MilkClayBucketItem(boolean isCracked, Properties props) {
    super(isCracked, props);
  }

  // drinking (animation, duration and sound) comes from the consumable component since 1.21.2, see Registration

  @Override
  public ItemStack finishUsingItem(ItemStack stack, Level worldIn, LivingEntity entity) {
    if (!worldIn.isClientSide()) {
      entity.removeAllEffects();
    }
    // update stats
    if (entity instanceof ServerPlayer serverPlayer) {
      CriteriaTriggers.CONSUME_ITEM.trigger(serverPlayer, stack);
      serverPlayer.awardStat(Stats.ITEM_USED.get(this));
    }
    // if a player, empty a bucket
    if (entity instanceof Player player) {
      if (isCracked) {
        renderBrokenItem(player, stack);
      }
      return updateBucket(stack, player, remainder(stack));
    }
    return stack;
  }

  /* Fluids */

  @Override
  public Fluid getFluid(ItemStack stack) {
    return CeramicsPlatform.get().milkFluid();
  }
}

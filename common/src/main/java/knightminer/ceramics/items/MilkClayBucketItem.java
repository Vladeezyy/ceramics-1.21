package knightminer.ceramics.items;

import knightminer.ceramics.platform.CeramicsPlatform;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * Clay bucket holding milk
 */
public class MilkClayBucketItem extends BaseClayBucketItem {
  public MilkClayBucketItem(boolean isCracked, Properties props) {
    super(isCracked, props);
  }

  @Override
  public InteractionResultHolder<ItemStack> use(Level worldIn, Player player, InteractionHand hand) {
    player.startUsingItem(hand);
    return new InteractionResultHolder<>(InteractionResult.SUCCESS, player.getItemInHand(hand));
  }

  @Override
  public UseAnim getUseAnimation(ItemStack stack) {
    return UseAnim.DRINK;
  }

  @Override
  public int getUseDuration(ItemStack stack, LivingEntity entity) {
    return 32;
  }

  @Override
  public ItemStack finishUsingItem(ItemStack stack, Level worldIn, LivingEntity entity) {
    if (!worldIn.isClientSide()) {
      CeramicsPlatform.get().cureAsMilk(entity);
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
      return updateBucket(stack, player, BaseClayBucketItem.remainder(stack));
    }
    return stack;
  }

  /* Fluids */

  @Override
  public Fluid getFluid(ItemStack stack) {
    return CeramicsPlatform.get().milkFluid();
  }
}

package knightminer.ceramics.items;

import knightminer.ceramics.Ceramics;
import knightminer.ceramics.Registration;
import knightminer.ceramics.recipe.CeramicsTags;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.cow.AbstractCow;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.SolidBucketItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;

import java.util.List;
import java.util.function.Consumer;
import java.util.Map;
import java.util.Optional;

public class EmptyClayBucketItem extends BaseClayBucketItem {
	public EmptyClayBucketItem(boolean isCracked, Properties props) {
		super(isCracked, props);
	}


	/* Bucket behavior */

	@Override
	public InteractionResult use(Level world, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		BlockHitResult trace = getPlayerPOVHitResult(world, player, ClipContext.Fluid.SOURCE_ONLY);

		// if we missed, do nothing
		if (trace.getType() != Type.BLOCK) {
			return InteractionResult.PASS;
		}

		// normal fluid logic
		BlockPos pos = trace.getBlockPos();
		Direction direction = trace.getDirection();
		BlockPos offset = pos.relative(direction);

		// ensure we can place a fluid there
		if (world.mayInteract(player, pos) && player.mayUseItemAt(offset, direction, stack)) {
			BlockState state = world.getBlockState(pos);
			Block block = state.getBlock();
			// if blacklisted, do nothing
			if (block instanceof LiquidBlock liquidBlock && liquidBlock.fluid.is(CeramicsTags.Fluids.BUCKET_BLACKLIST)) {
				return InteractionResult.FAIL;
			}
			if (block instanceof BucketPickup pickup) {
				ItemStack pickupStack = pickup.pickupBlock(player, world, pos, state);
				// nothing picked up (e.g. a waterlogged block that refused), nothing to do
				if (pickupStack.isEmpty()) {
					return InteractionResult.FAIL;
				}
				ItemStack newStack;
				if (pickupStack.getItem() instanceof BucketItem bucket) {
					ItemStack result = withFluid(bucket.content, isCracked);
					copyExtraComponents(pickupStack, result);
					newStack = ItemUtils.createFilledResult(stack, player, result);
				} else if (pickupStack.getItem() instanceof SolidBucketItem bucket) {
					world.gameEvent(player, GameEvent.FLUID_PICKUP, pos);
					ItemStack result = withBlock(bucket.getBlock(), isCracked);
					copyExtraComponents(pickupStack, result);
					newStack = ItemUtils.createFilledResult(stack, player, result);
				} else {
					// unfortunately, nothing we can do in this case, can logically only handle blocks and fluids
					Ceramics.LOG.warn("Received an unexpected bucket {} on pickup, attempting to replace original state {}", pickupStack, state);
					world.setBlock(pos, state, 0);
					return InteractionResult.FAIL;
				}

				// stats and sounds
				player.awardStat(Stats.ITEM_USED.get(this));
				pickup.getPickupSound(state).ifPresent((sound) -> player.playSound(sound, 1.0F, 1.0F));
				if (!world.isClientSide()) {
					CriteriaTriggers.FILLED_BUCKET.trigger((ServerPlayer)player, stack);
				}
				// success
				return InteractionResult.SUCCESS.heldItemTransformedTo(newStack);
			}
		}
		return InteractionResult.FAIL;
	}

	@Override
	public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
		// only work if the bucket is empty and right clicking a cow
		if(!player.isCreative() && target instanceof AbstractCow && !target.isBaby()) {
			// sound
			player.playSound(SoundEvents.COW_MILK, 1.0F, 1.0F);
			if (!player.level().isClientSide()) {
				addItem(player, withMilk(isCracked));
				stack.shrink(1);
			}
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.PASS;
	}

	/**
	 * Copies data of a picked up bucket (such as extra data from another mod) onto the clay bucket, skipping our own bucket
	 * contents and the stack size of the vanilla bucket. Replaces the NBT merge of 1.20.
	 * @param pickupStack  Bucket returned by the block
	 * @param result       Clay bucket to update
	 */
	private static void copyExtraComponents(ItemStack pickupStack, ItemStack result) {
		DataComponentPatch patch = pickupStack.getComponentsPatch();
		if (!patch.isEmpty()) {
			DataComponentPatch.Builder builder = DataComponentPatch.builder();
			for (Map.Entry<DataComponentType<?>, Optional<?>> entry : patch.entrySet()) {
				DataComponentType<?> type = entry.getKey();
				if (type != Registration.BUCKET_FLUID.get() && type != Registration.BUCKET_BLOCK.get() && type != DataComponents.MAX_STACK_SIZE) {
					copyEntry(builder, entry);
				}
			}
			result.applyComponents(builder.build());
		}
	}

	/** Copies a single entry of the patch, generics helper */
	@SuppressWarnings("unchecked")
	private static <T> void copyEntry(DataComponentPatch.Builder builder, Map.Entry<DataComponentType<?>, Optional<?>> entry) {
		DataComponentType<T> type = (DataComponentType<T>) entry.getKey();
		Optional<?> value = entry.getValue();
		if (value.isPresent()) {
			builder.set(type, (T) value.get());
		} else {
			builder.remove(type);
		}
	}


	/* Stack properties */

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flagIn) {
		tooltip.accept(Component.translatable(this.getDescriptionId() + ".tooltip").withStyle(ChatFormatting.GRAY));
	}
}

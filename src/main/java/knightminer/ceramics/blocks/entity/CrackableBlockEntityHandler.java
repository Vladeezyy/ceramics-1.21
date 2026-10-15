package knightminer.ceramics.blocks.entity;

import dev.vladeezyy.kilnwork.block.SyncedBlockEntity;
import knightminer.ceramics.items.BaseClayBucketItem;
import knightminer.ceramics.items.CrackableBlockItem;
import knightminer.ceramics.recipe.CeramicsTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;

import javax.annotation.Nullable;

/**
 * Common logic for all crackable fluid blocks. Since 1.21.11 the cracks are the {@link ICrackableBlock#CRACKS} block
 * state property (synced and drawn by plain block models) instead of block entity data with a custom model.
 */
public class CrackableBlockEntityHandler {
	/** Tag of 1.20 and 1.21.1 block entities, read once from old worlds */
	public static final String TAG_CRACKS = "cracks";

	/** Parent block entity */
	private final SyncedBlockEntity parent;
	/** Whether to use crackable logic, used to allow one class to handle both cases */
	private boolean active;
	/** Cracks from an old world, applied to the block state once loaded */
	private int legacyCracks = 0;

	public CrackableBlockEntityHandler(SyncedBlockEntity parent, boolean active) {
		this.parent = parent;
		this.active = active;
	}

	/** Checks if this handler is active */
	public boolean isActive() {
		return active;
	}

	/* Blocks */

	/**
	 * Called when a new fluid is added to set the first crack stage if hot
	 * @param fluid  Fluid added
	 */
	public void fluidAdded(FluidStack fluid) {
		if (active && getCracks() == 0 && BaseClayBucketItem.doesCrack(fluid.getFluid())) {
			setCracks(1);
		}
	}

	/**
	 * Increases the cracks, helper for passing a fluid stack directly
	 * @param fluid  Fluid in block
	 */
	public void updateCracks(FluidStack fluid) {
		updateCracks(fluid.getFluid(), fluid.getAmount());
	}

	/**
	 * Increases or decreases the cracks based on the fluid temperature
	 * @param fluid  Fluid in block
	 */
	public void updateCracks(Fluid fluid, int relevantAmount) {
		if (active && BaseClayBucketItem.doesCrack(fluid)) {
			int cracks = getCracks();
			// after 5, break the block
			if (cracks >= 5) {
				Level world = parent.getLevel();
				if (world != null) {
					world.destroyBlock(parent.getBlockPos(), false);
					// if we have at least a bucket of fluid, place in world if possible
					if (relevantAmount > FluidType.BUCKET_VOLUME) {
						BlockState state = fluid.defaultFluidState().createLegacyBlock();
						if (state.getBlock() != Blocks.AIR) {
							world.setBlockAndUpdate(parent.getBlockPos(), state);
						}
						// if less, try to place a flowing fluid
					} else if (fluid instanceof FlowingFluid flowing) {
						int level = Math.max(1, relevantAmount * 8 / FluidType.BUCKET_VOLUME);
						world.setBlockAndUpdate(parent.getBlockPos(), flowing.getFlowing(level, false).createLegacyBlock());
					}
				}
			} else {
				// just increase by 1
				setCracks(cracks + 1);
			}
		}
	}

	/** Gets the current cracks value of this block */
	public int getCracks() {
		BlockState state = parent.getBlockState();
		return state.hasProperty(ICrackableBlock.CRACKS) ? state.getValue(ICrackableBlock.CRACKS) : 0;
	}

	/** Updates the cracks state of this block, on the server */
	public void setCracks(int cracks) {
		Level world = parent.getLevel();
		BlockState state = parent.getBlockState();
		if (active && world != null && !world.isClientSide() && state.hasProperty(ICrackableBlock.CRACKS)) {
			cracks = Math.max(0, Math.min(5, cracks));
			if (state.getValue(ICrackableBlock.CRACKS) != cracks) {
				world.setBlock(parent.getBlockPos(), state.setValue(ICrackableBlock.CRACKS, cracks), Block.UPDATE_ALL);
			}
		}
	}

	/* Items */

	/** Adds the cracks to the components of the block entity, used for drops and pick block */
	public void collectImplicitComponents(DataComponentMap.Builder builder) {
		int cracks = getCracks();
		if (active && cracks > 0) {
			builder.set(knightminer.ceramics.Registration.CRACKS.get(), cracks);
		}
	}

	/* Saving */

	/** Reads the activity from the block, and cracks saved by older versions */
	public void read(BlockState state, ValueInput input) {
		this.active = state.getBlock() instanceof ICrackableBlock crackable && crackable.isCrackable();
		this.legacyCracks = active ? input.getIntOr(TAG_CRACKS, 0) : 0;
	}

	/** Moves cracks from an old world into the block state, call once the block entity has a level */
	public void applyLegacyCracks() {
		if (legacyCracks > 0 && getCracks() == 0) {
			setCracks(legacyCracks);
		}
		legacyCracks = 0;
	}

	/** Interface to allow blocks to opt out of cracking behavior on some variants */
	public interface ICrackableBlock {
		/** Cracks of the block, 0 to 5, drawn by the block model */
		IntegerProperty CRACKS = IntegerProperty.create("cracks", 0, 5);

		/** Determines if this version of the block is crackable */
		boolean isCrackable();

		/** Applies the cracks of the placed item to the state */
		@Nullable
		default BlockState withItemCracks(@Nullable BlockState state, BlockPlaceContext context) {
			if (state != null && isCrackable() && state.hasProperty(CRACKS)) {
				return state.setValue(CRACKS, CrackableBlockItem.getCracks(context.getItemInHand()));
			}
			return state;
		}

		/**
		 * Attempts to repair a crackable block
		 * @param world    World instance
		 * @param pos      Position of crackable block
		 * @param player   Player interacting
		 * @param hand     Hand used
		 * @return  True if repaired, false for wrong repair item or no cracks
		 */
		static boolean tryRepair(LevelAccessor world, BlockPos pos, Player player, InteractionHand hand) {
			ItemStack held = player.getItemInHand(hand);
			if (held.is(CeramicsTags.Items.TERRACOTTA_CRACK_REPAIR) && world.getBlockEntity(pos) instanceof ICrackableBlockEntity te) {
				CrackableBlockEntityHandler handler = te.getCracksHandler();
				int cracks = handler.getCracks();
				if (handler.isActive() && cracks > 0) {
					// play sound
					world.playSound(player, pos, SoundType.GRAVEL.getPlaceSound(), SoundSource.BLOCKS, 1, 1);
					if (!world.isClientSide()) {
						// repair halfway
						handler.setCracks(Math.max(0, cracks - 3));
						// take clay
						if (!player.isCreative()) {
							held.shrink(1);
							player.setItemInHand(hand, held);
						}
					}
					return true;
				}
			}
			return false;
		}
	}

	/** Interface to make syncing easier */
	public interface ICrackableBlockEntity {
		/** Gets the cracks handler for this block entity */
		CrackableBlockEntityHandler getCracksHandler();
	}
}

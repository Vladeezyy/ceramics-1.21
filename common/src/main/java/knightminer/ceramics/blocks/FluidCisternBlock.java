package knightminer.ceramics.blocks;

import net.minecraft.world.InteractionResult;
import knightminer.ceramics.blocks.entity.CisternBlockEntity;
import knightminer.ceramics.blocks.entity.CrackableBlockEntityHandler.ICrackableBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import dev.vladeezyy.kilnwork.fluid.FluidTransferHelper;

import javax.annotation.Nullable;

/**
 * Fired cistern block that can store fluids
 */
public class FluidCisternBlock extends CisternBlock implements ICrackableBlock, EntityBlock {
  private final boolean crackable;
  public FluidCisternBlock(Properties properties, boolean crackable) {
    super(properties);
    this.crackable = crackable;
  }


  /* Tile entity */

  @Override
  public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
    return new CisternBlockEntity(pos, state, crackable);
  }

  /* Interaction */

  @SuppressWarnings("deprecation")
  @Deprecated
  @Override
  protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
    if (crackable && ICrackableBlock.tryRepair(world, pos, player, hand)) {
      return InteractionResult.SUCCESS;
    }
    if (FluidTransferHelper.interactWithTank(world, pos, player, hand, hit)) {
      return InteractionResult.SUCCESS;
    }
    return InteractionResult.TRY_WITH_EMPTY_HAND;
  }


  /* Structure behavior */

  /**
   * Finds the base TE for this extension
   * @param world  World instance
   * @param pos    Cistern extension position
   * @return  Optional containing base TE, or empty optional if base cannot be found
   */
  @Nullable
  private CisternBlockEntity findBase(Level world, BlockPos pos) {
    BlockPos base = pos;
    BlockState checkState;
    do {
      base = base.below();
      checkState = world.getBlockState(base);
    } while (checkState.is(this) && checkState.getValue(CisternBlock.EXTENSION));

    // if the position is a cistern, it means we found a base, return that position
    if (checkState.is(this) && world.getBlockEntity(base) instanceof CisternBlockEntity cistern) {
      return cistern;
    }
    // not found, return nothing
    return null;
  }

  @Override
  public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
    // cracks of the item are already in the state, see getStateForPlacement
    if (state.getValue(CisternBlock.EXTENSION)) {
      // try to find a base cistern below if an extension
      CisternBlockEntity base = findBase(world, pos);
      if (base != null) {
        base.addExtension(pos);
      }
    } else if (world.getBlockEntity(pos) instanceof CisternBlockEntity cistern) {
      cistern.tryMerge(pos.above());
    }
  }

  // breaking the cistern moves the fluid to the cisterns above, see CisternBlockEntity#preRemoveSideEffects (onRemove is gone since 1.21.5)

  @Override
  protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
    super.createBlockStateDefinition(builder);
    builder.add(CRACKS);
  }

  @Nullable
  @Override
  public BlockState getStateForPlacement(BlockPlaceContext context) {
    return withItemCracks(super.getStateForPlacement(context), context);
  }

  @SuppressWarnings("deprecation")
  @Override
  @Deprecated
  protected BlockState updateShape(BlockState state, LevelReader worldIn, ScheduledTickAccess ticks, BlockPos currentPos, Direction facing, BlockPos facingPos, BlockState facingState, RandomSource random) {
    // overridden to remove down connection, we handle that conditionally in the TE
    if (!facing.getAxis().isVertical()) {
      // barrel connects to
      state = state.setValue(CONNECTIONS.get(facing), isConnected(facing, facingState));
    }
    return state;
  }


  /* Cracking */

  @Override
  public boolean isCrackable() {
    return crackable;
  }

  @SuppressWarnings("deprecation")
  @Override
  @Deprecated
  public void randomTick(BlockState state, ServerLevel worldIn, BlockPos pos, RandomSource random) {
    if (isCrackable() && random.nextInt(5) == 0) {
      if (worldIn.getBlockEntity(pos) instanceof CisternBlockEntity te) {
        te.randomTick();
      }
    }
  }
}

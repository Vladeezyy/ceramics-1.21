package knightminer.ceramics.blocks;

import net.minecraft.world.InteractionResult;
import knightminer.ceramics.Ceramics;
import knightminer.ceramics.Registration;
import knightminer.ceramics.blocks.entity.ChannelBlockEntity;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.phys.BlockHitResult;
import dev.vladeezyy.kilnwork.util.BlockEntityHelper;

import javax.annotation.Nullable;

/**
 * Channel extension that supports moving fluids
 */
public class FlowingChannelBlock extends ChannelBlock implements ICrackableBlock, EntityBlock {
	private final boolean crackable;
	public FlowingChannelBlock(Properties props, boolean crackable) {
		super(props);
		this.crackable = crackable;
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new ChannelBlockEntity(pos, state, crackable);
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return BlockEntityHelper.serverTicker(level, type, Registration.CHANNEL_BLOCK_ENTITY.get(), ChannelBlockEntity.SERVER_TICKER);
	}

	// neighbor changes need nothing here: the capability caches of the channel follow the neighbors themselves

	@Override
	protected void activateTileEntity(BlockState state, Level world, BlockPos pos, Direction side) {
		if (world.getBlockEntity(pos) instanceof ChannelBlockEntity channel) {
			channel.refreshNeighbor(state, side);
		}
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
		if (isCrackable() && worldIn.getBlockEntity(pos) instanceof ChannelBlockEntity channel) {
			channel.randomTick();
		}
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (crackable && ICrackableBlock.tryRepair(world, pos, player, hand)) {
			return InteractionResult.SUCCESS;
		}
		return super.useItemOn(stack, state, world, pos, player, hand, hit);
	}

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
}

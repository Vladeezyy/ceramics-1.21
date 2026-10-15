package knightminer.ceramics.blocks.entity;

import dev.vladeezyy.kilnwork.block.SyncedBlockEntity;
import knightminer.ceramics.blocks.ChannelBlock;
import knightminer.ceramics.blocks.ChannelBlock.ChannelConnection;
import knightminer.ceramics.blocks.entity.CrackableBlockEntityHandler.ICrackableBlockEntity;
import knightminer.ceramics.network.CeramicsNetwork;
import knightminer.ceramics.network.ChannelFlowPacket;
import knightminer.ceramics.network.FluidUpdatePacket;
import knightminer.ceramics.network.FluidUpdatePacket.FluidUpdater;
import knightminer.ceramics.util.tank.ChannelSideTank;
import knightminer.ceramics.util.tank.ChannelTank;
import knightminer.ceramics.util.tank.FillOnlyFluidHandler;
import com.mojang.serialization.Codec;
import net.minecraft.util.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Plane;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.EmptyResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.util.EnumMap;
import java.util.Map;

/**
 * Logic for channel fluid transfer
 */
public class ChannelBlockEntity extends SyncedBlockEntity implements ICrackableBlockEntity, FluidUpdater {
	public static final BlockEntityTicker<ChannelBlockEntity> SERVER_TICKER = (level, pos, state, te) -> te.tick(state);
	/** Channel internal tank */
	private final ChannelTank tank = new ChannelTank(FaucetBlockEntity.MB_PER_TICK * 4, this);
	/** Handler to return from channel top */
	private final ResourceHandler<FluidResource> topHandler = new FillOnlyFluidHandler(tank);
	/** Tanks for inserting on each side */
	private final Map<Direction,ResourceHandler<FluidResource>> sideTanks = Util.make(new EnumMap<>(Direction.class), map -> {
		for (Direction direction : Plane.HORIZONTAL) {
			map.put(direction, new ChannelSideTank(this, tank, direction));
		}
	});
	/** Cache of tanks on all neighboring sides, server only */
	private final Map<Direction,BlockCapabilityCache<ResourceHandler<FluidResource>,Direction>> neighborTanks = new EnumMap<>(Direction.class);
	/** Stores if the channel is currently flowing, set to 2 to allow a small buffer */
	private final byte[] isFlowing = new byte[5];
	/** Crackable handling */
	private final CrackableBlockEntityHandler cracksHandler;

	public ChannelBlockEntity(BlockPos pos, BlockState state, boolean crackable) {
		super(knightminer.ceramics.Registration.CHANNEL_BLOCK_ENTITY.get(), pos, state);
		cracksHandler = new CrackableBlockEntityHandler(this, crackable);
	}

	public ChannelBlockEntity(BlockPos pos, BlockState state) {
		this(pos, state, false);
	}

	/**
	 * Gets the central fluid tank of this channel
	 * @return  Central tank
	 */
	public FluidStack getFluid() {
		return this.tank.getFluid();
	}

	@Override
	public CrackableBlockEntityHandler getCracksHandler() {
		return cracksHandler;
	}


	/* Fluid handlers */

	/**
	 * Gets the fluid handler for the given side, registered as the block capability
	 * @param side  Side queried, null for internal access
	 * @return  Fluid handler, or null if the side has none
	 */
	@Nullable
	public ResourceHandler<FluidResource> getFluidHandler(@Nullable Direction side) {
		// top side gets the insert direct
		if (side == null || side == Direction.UP) {
			return topHandler;
		}
		// side tanks keep track of which side inserts
		if (side != Direction.DOWN) {
			ChannelConnection connection = getBlockState().getValue(ChannelBlock.DIRECTION_MAP.get(side));
			if (connection == ChannelConnection.IN) {
				return sideTanks.get(side);
			}
			// for out, return an empty fluid handler so the block we are pouring into knows we support fluids, even though we disallow any interaction
			// this will get invalidated when the connection goes back to in later
			if (connection == ChannelConnection.OUT) {
				return EmptyResourceHandler.instance();
			}
		}
		return null;
	}

	@Override
	public void setBlockState(BlockState state) {
		super.setBlockState(state);
		// the handlers exposed on each side depend on the connections, so let neighbors query again
		if (level != null && !level.isClientSide()) {
			level.invalidateCapabilities(worldPosition);
		}
	}

	/**
	 * Gets the fluid handler from a neighbor
	 * @param side  Side of the neighbor to fetch
	 * @return  Fluid handler, or null
	 */
	@Nullable
	protected ResourceHandler<FluidResource> getNeighborHandler(Direction side) {
		if (!(level instanceof ServerLevel server)) {
			return null;
		}
		return neighborTanks.computeIfAbsent(side, s -> BlockCapabilityCache.create(
			Capabilities.Fluid.BLOCK, server, worldPosition.relative(s), s.getOpposite(), () -> !this.isRemoved(), () -> {})).getCapability();
	}


	/**
	 * Refreshes a neighbor based on the new connection
	 * @param state  The state that will later be put in the world, may not be the state currently in the world
	 * @param side   Side to update
	 */
	public void refreshNeighbor(BlockState state, Direction side) {
		// capability caches update themselves, just make sure neighbors notice the new connection
		if (level != null && !level.isClientSide()) {
			level.invalidateCapabilities(worldPosition);
		}
	}

	/**
	 * Called on random tick to increase or decrease cracks level
	 */
	public void randomTick() {
		cracksHandler.updateCracks(tank.getFluid());
	}


	/* Flowing property */

	/**
	 * Gets the index for the given side for flowing. Same as regular index but without up
	 * @param side  Side to index
	 * @return Flow index
	 */
	private int getFlowIndex(Direction side) {
		if (side.getAxis().isVertical()) {
			return 0;
		}
		return side.get3DDataValue() - 1;
	}

	/**
	 * Marks the given side as flowing for the sake of rendering
	 * @param side     Side to set
	 * @param flowing  True to mark it as flowing
	 */
	public void setFlow(Direction side, boolean flowing) {
		if (side == Direction.UP) {
			return;
		}
		// update flowing state
		int index = getFlowIndex(side);
		boolean wasFlowing = isFlowing[index] > 0;
		isFlowing[index] = (byte)(flowing ? 2 : 0);
		// send packet to client if it changed
		if(wasFlowing != flowing && level != null && !level.isClientSide()) {
			syncFlowToClient(side, flowing);
		}
	}

	/**
	 * Checks if the given side is flowing
	 * @param side  Side to check
	 * @return  True if flowing
	 */
	public boolean isFlowing(Direction side) {
		if (side == Direction.UP) {
			return false;
		}
		return isFlowing[getFlowIndex(side)] > 0;
	}


	/* Utilities */

	/**
	 * Gets the connection for a side
	 * @param side  Side to query
	 * @return  Connection on the specified side
	 */
	protected boolean isOutput(Direction side) {
		// just always return in for up, thats fine
		if(side == Direction.UP) {
			return false;
		}
		// down is boolean, sides is multistate
		if(side == Direction.DOWN) {
			return this.getBlockState().getValue(ChannelBlock.DOWN);
		}
		return this.getBlockState().getValue(ChannelBlock.DIRECTION_MAP.get(side)) == ChannelConnection.OUT;
	}

	/**
	 * Counts the number of side outputs on the given side
	 * @param state  State to check
	 * @return  Number of outputs
	 */
	private static int countOutputs(BlockState state) {
		int count = 0;
		for (Direction direction : Plane.HORIZONTAL) {
			if (state.getValue(ChannelBlock.DIRECTION_MAP.get(direction)) == ChannelConnection.OUT) {
				count++;
			}
		}
		return count;
	}

	/**
	 * Syncs the given flowing state to the client side
	 * @param side     Side to sync
	 * @param flowing  Flowing state to sync
	 */
	private void syncFlowToClient(Direction side, boolean flowing) {
		CeramicsNetwork.sendToClientsAround(new ChannelFlowPacket(worldPosition, side, flowing), level, worldPosition);
	}


	/* Flow */

	/**
	 * Ticking logic
	 */
	private void tick(BlockState state) {
		// must have fluid first
		FluidStack fluid = tank.getFluid();
		if(!fluid.isEmpty()) {
			// if we have down and can flow, skip sides
			boolean hasFlown = false;
			if(state.getValue(ChannelBlock.DOWN)) {
				hasFlown = trySide(Direction.DOWN, FaucetBlockEntity.MB_PER_TICK);
			}
			// try sides if we have any sides
			int outputs = countOutputs(state);
			if(!hasFlown && outputs > 0) {
				// split the fluid evenly between sides
				int flowRate = Mth.clamp(tank.getMaxUsable() / outputs, 1, FaucetBlockEntity.MB_PER_TICK);
				// then transfer on each side
				for(Direction side : Plane.HORIZONTAL) {
					trySide(side, flowRate);
				}
			}
		}

		// clear flowing if we should no longer flow on a side
		for (int i = 0; i < 5; i++) {
			if (isFlowing[i] > 0) {
				isFlowing[i]--;
				if (isFlowing[i] == 0) {
					Direction direction;
					if (i == 0) {
						direction = Direction.DOWN;
					} else {
						direction = Direction.from3DDataValue(i + 1);
					}
					syncFlowToClient(direction, false);
				}
			}
		}

		tank.freeFluid();
	}

	/**
	 * Tries transferring fluid on a single side of the channel
	 * @param side      Side to transfer from
	 * @param flowRate  Maximum amount to output
	 * @return  True if the side transferred fluid
	 */
	protected boolean trySide(Direction side, int flowRate) {
		if(tank.isEmpty() || !this.isOutput(side)) {
			return false;
		}

		// get the handler on the side, try filling
		ResourceHandler<FluidResource> handler = getNeighborHandler(side);
		return handler != null && fill(side, handler, flowRate);
	}

	/**
	 * Fill the fluid handler on the given side
	 * @param side     Side to fill
	 * @param handler  Handler to fill
	 * @param amount   Amount to fill
	 * @return  True if the side successfully filled something
	 */
	protected boolean fill(Direction side, ResourceHandler<FluidResource> handler, int amount) {
		// make sure we do not allow more than the fluid allows, should not happen but just in case
		int usable = Math.min(tank.getMaxUsable(), amount);
		if (usable > 0) {
			// move as much as the neighbor takes in one transaction
			if (ResourceHandlerUtil.move(tank, handler, resource -> true, usable, null) > 0) {
				// mark that the side is flowing
				setFlow(side, true);
				return true;
			}
		}

		// failed to flow, mark side as not flowing
		setFlow(side, false);
		return false;
	}


	/* NBT and sync */
	private static final String TAG_IS_FLOWING = "is_flowing";
	private static final String TAG_TANK = "tank";
	/** Keys of the NeoForge 1.21.1 FluidTank, kept for old worlds */
	private static final String TAG_FLUID = "Fluid";
	private static final String TAG_LOCKED = "locked";

	/**
	 * Sends a fluid update to the client with the current fluid
	 */
	public void sendFluidUpdate() {
		if (level != null && !level.isClientSide()) {
			CeramicsNetwork.sendToClientsAround(new FluidUpdatePacket(worldPosition, getFluid(), false), level, worldPosition);
		}
	}

	@Override
  public void updateFluid(FluidStack fluid, boolean unused) {
		tank.setFluid(fluid);
	}

	@Override
	protected boolean shouldSyncOnUpdate() {
		return true;
	}

	@Override
	protected void saveSynced(ValueOutput output) {
		super.saveSynced(output);
		output.store(TAG_IS_FLOWING, Codec.BYTE_BUFFER, ByteBuffer.wrap(isFlowing.clone()));
		ValueOutput tankOutput = output.child(TAG_TANK);
		if (!tank.isEmpty()) {
			tankOutput.store(TAG_FLUID, FluidStack.CODEC, tank.getFluid());
		}
		tankOutput.putInt(TAG_LOCKED, tank.getLocked());
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		// isFlowing
		input.read(TAG_IS_FLOWING, Codec.BYTE_BUFFER).ifPresent(buffer -> {
			int max = Math.min(5, buffer.remaining());
			for (int i = 0; i < max; i++) {
				isFlowing[i] = (byte) Mth.clamp(buffer.get(buffer.position() + i), 0, 2);
			}
		});
		// tank
		ValueInput tankInput = input.childOrEmpty(TAG_TANK);
		tank.setFluid(tankInput.read(TAG_FLUID, FluidStack.CODEC).orElse(FluidStack.EMPTY));
		tank.setLocked(tankInput.getIntOr(TAG_LOCKED, 0));
		cracksHandler.read(getBlockState(), input);
	}

	@Override
	public void onLoad() {
		super.onLoad();
		cracksHandler.applyLegacyCracks();
	}

	@Override
	protected void collectImplicitComponents(DataComponentMap.Builder builder) {
		super.collectImplicitComponents(builder);
		cracksHandler.collectImplicitComponents(builder);
	}
}

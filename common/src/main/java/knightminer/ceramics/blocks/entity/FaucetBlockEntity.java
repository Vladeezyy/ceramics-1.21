package knightminer.ceramics.blocks.entity;

import dev.vladeezyy.kilnwork.fluid.api.IFluidHandler.FluidAction;
import dev.vladeezyy.kilnwork.fluid.api.IFluidHandler;
import dev.vladeezyy.kilnwork.platform.KilnworkPlatform;
import dev.vladeezyy.kilnwork.block.SyncedBlockEntity;
import knightminer.ceramics.blocks.entity.CrackableBlockEntityHandler.ICrackableBlockEntity;
import knightminer.ceramics.network.CeramicsNetwork;
import knightminer.ceramics.network.FluidUpdatePacket;
import knightminer.ceramics.network.FluidUpdatePacket.FluidUpdater;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import dev.vladeezyy.kilnwork.fluid.api.FluidStack;

import javax.annotation.Nullable;

import static knightminer.ceramics.blocks.FaucetBlock.FACING;

public class FaucetBlockEntity extends SyncedBlockEntity implements ICrackableBlockEntity, FluidUpdater {
  public static final BlockEntityTicker<FaucetBlockEntity> SERVER_TICKER = (level, pos, state, entity) -> entity.tick();
  /** Transfer rate of the faucet */
  public static final int MB_PER_TICK = 25;
  /** amount of MB to extract from the input at a time */
  public static final int PACKET_SIZE = 250;

  private static final String TAG_DRAINED = "drained";
  private static final String TAG_RENDER_FLUID = "render_fluid";
  private static final String TAG_STOP = "stop";
  private static final String TAG_STATE = "state";
  private static final String TAG_LAST_REDSTONE = "lastRedstone";

  /** If true, faucet is currently pouring */
  private FaucetState faucetState = FaucetState.OFF;
  /** If true, redstone told this faucet to stop, so stop when ready */
  private boolean stopPouring = false;
  /** Current fluid in the faucet */
  private FluidStack drained = FluidStack.EMPTY;
  /** Fluid for rendering, used to reduce the number of packets. There is a brief moment where {@link this#drained} is empty but we should be rendering something */
  private FluidStack renderFluid = FluidStack.EMPTY;
  /** Used for pulse detection */
  private boolean lastRedstoneState;

  /** Cached fluid handler of the input to the faucet, server only */
  @Nullable
  private KilnworkPlatform.FluidHandlerCache inputHandler;
  /** Cached fluid handler of the output from the faucet, server only */
  @Nullable
  private KilnworkPlatform.FluidHandlerCache outputHandler;

  /** Crackable logic */
  private final CrackableBlockEntityHandler cracksHandler;

  public FaucetBlockEntity(BlockPos pos, BlockState state, boolean crackable) {
    super(knightminer.ceramics.Registration.FAUCET_BLOCK_ENTITY.get(), pos, state);
    cracksHandler = new CrackableBlockEntityHandler(this, crackable);
  }

  public FaucetBlockEntity(BlockPos pos, BlockState state) {
    this(pos, state, false);
  }

  /**
   * Called on random tick to increase or decrease the heat
   */
  public void randomTick() {
    // using render fluid so it works for the brief ticks between "packets"
    cracksHandler.updateCracks(renderFluid);
  }


  /* Fluid handler */

  /**
   * Finds the fluid handler on the given side
   * @param side  Side to check
   * @return  Fluid handler
   */
  @Nullable
  private KilnworkPlatform.FluidHandlerCache findFluidHandler(Direction side) {
    if (level instanceof ServerLevel server) {
      return KilnworkPlatform.get().fluidHandlerCache(server, worldPosition.relative(side), side.getOpposite(), () -> !this.isRemoved());
    }
    return null;
  }

  /**
   * Gets the input fluid handler
   * @return  Input fluid handler
   */
  @Nullable
  private IFluidHandler getInputHandler() {
    if (inputHandler == null) {
      inputHandler = findFluidHandler(getBlockState().getValue(FACING).getOpposite());
    }
    return inputHandler == null ? null : inputHandler.get();
  }

  /**
   * Gets the output fluid handler
   * @return  Output fluid handler
   */
  @Nullable
  private IFluidHandler getOutputHandler() {
    if (outputHandler == null) {
      outputHandler = findFluidHandler(Direction.DOWN);
    }
    return outputHandler == null ? null : outputHandler.get();
  }



  /* Data */

  /**
   * Gets whether the faucet is pouring
   * @return True if pouring
   */
  public boolean isPouring() {
    return faucetState != FaucetState.OFF;
  }

  /**
   * Gets the fluid currently being drained, mainly used for rendering
   * @return  Fluid being drained
   */
  public FluidStack getRenderFluid() {
    return renderFluid;
  }

  @Override
  public CrackableBlockEntityHandler getCracksHandler() {
    return cracksHandler;
  }


  /* Activation */

  /**
   * Toggles pouring state and initiates transfer if appropriate. Called on right click and from redstone
   */
  public void activate() {
    // don't run on client
    if (level == null || level.isClientSide()) {
      return;
    }
    // already pouring? we want to start
    switch (faucetState) {
      // off activates the faucet
      case OFF -> {
        stopPouring = false;
        doTransfer(true);
      }
      // powered deactivates the faucet, sync to client
      case POWERED -> {
        faucetState = FaucetState.OFF;
        syncToClient(FluidStack.EMPTY, false);
      }
      // pouring means we stop pouring as soon as possible
      case POURING -> stopPouring = true;
    }
  }

  /**
   * Flips hasSignal and schedules a tick if appropriate.
   * @param hasSignal  New signal state
   */
  public void handleRedstone(boolean hasSignal) {
    if (hasSignal != lastRedstoneState) {
      lastRedstoneState = hasSignal;
      if (hasSignal) {
        if (level != null){
          level.scheduleTick(worldPosition, this.getBlockState().getBlock(), 2);
        }
      } else if (faucetState == FaucetState.POWERED) {
        faucetState = FaucetState.OFF;
        syncToClient(FluidStack.EMPTY, false);
      }
    }
  }


  /* Pouring */

  private void tick() {
    // nothing to do if not pouring
    if (faucetState == FaucetState.OFF) {
      return;
      // if powered and we can transfer, schedule transfer for next tick
    } else if (faucetState == FaucetState.POWERED && doTransfer(false)) {
      faucetState = FaucetState.POURING;
      return;
    }

    // continue current stack
    if (!drained.isEmpty()) {
      pour();
      // stop if told to stop once done
    } else if (stopPouring) {
      reset();
      // otherwise keep going
    } else {
      doTransfer(true);
    }
  }

  /**
   * Initiate fluid transfer
   */
  private boolean doTransfer(boolean execute) {
    // still got content left
    IFluidHandler input = getInputHandler();
    IFluidHandler output = getOutputHandler();
    if (input != null && output != null) {
      // can we drain?
      FluidStack drained = input.drain(PACKET_SIZE, FluidAction.SIMULATE);
      if (!drained.isEmpty()) {
        // can we fill
        int filled = output.fill(drained, FluidAction.SIMULATE);
        if (filled > 0) {
          // fill if requested
          if (execute) {
            // drain the liquid and transfer it, buffer the amount for delay
            this.drained = input.drain(filled, FluidAction.EXECUTE);

            // have the cracks handler check if this fluid cracks the faucet
            this.cracksHandler.fluidAdded(this.drained);

            // sync to clients if we have changes
            if (faucetState == FaucetState.OFF || !FluidStack.isSameFluidSameComponents(renderFluid, drained)) {
              syncToClient(this.drained, true);
            }
            faucetState = FaucetState.POURING;

            // pour after initial packet, in case we end up resetting later
            pour();
          }
          return true;
        }
      }

      // if powered, keep faucet running
      if (lastRedstoneState) {
        // sync if either we were not pouring before (particle effects), or if the client thinks we have fluid
        if (execute && (faucetState == FaucetState.OFF || !renderFluid.isEmpty())) {
          syncToClient(FluidStack.EMPTY, true);
        }
        faucetState = FaucetState.POWERED;
        return false;
      }
    }
    // reset if not powered, or if nothing to do
    if (execute) {
      reset();
    }
    return false;
  }

  /**
   * Takes the liquid inside and executes one pouring step.
   */
  private void pour() {
    if (drained.isEmpty()) {
      return;
    }

    // ensure we have an output
        IFluidHandler output = getOutputHandler();
    if (output != null) {
      FluidStack fillStack = drained.copyWithAmount(Math.min(drained.getAmount(), MB_PER_TICK));

      // can we fill?
      int filled = output.fill(fillStack, IFluidHandler.FluidAction.SIMULATE);
      if (filled > 0) {
        // update client if they do not think we have fluid
        if (!FluidStack.isSameFluidSameComponents(renderFluid, drained)) {
          syncToClient(drained, true);
        }

        // transfer it
        this.drained.shrink(filled);
        fillStack.setAmount(filled);
        output.fill(fillStack, IFluidHandler.FluidAction.EXECUTE);
      }
    }
    else {
      // output got lost. all liquid buffered is lost.
      reset();
    }
  }

  /**
   * Resets TE to default state.
   */
  private void reset() {
    stopPouring = false;
    drained = FluidStack.EMPTY;
    if (faucetState != FaucetState.OFF || !renderFluid.isEmpty()) {
      faucetState = FaucetState.OFF;
      syncToClient(FluidStack.EMPTY, false);
    }
  }




  /* NBT and networking */

  /**
   * Sends an update to the client with the most recent
   * @param fluid       New fluid
   * @param isPouring   New isPouring status
   */
  private void syncToClient(FluidStack fluid, boolean isPouring) {
    renderFluid = fluid.copy();
    if (level instanceof ServerLevel server) {
      CeramicsNetwork.sendToClientsAround(new FluidUpdatePacket(worldPosition, fluid, isPouring), server, worldPosition);
    }
  }

  /**
   * Sets draining fluid to specified stack.
   * @param fluid new FluidStack
   */
  @Override
  public void updateFluid(FluidStack fluid, boolean isPouring) {
    // pouring and powered are interchangable on the client
    this.faucetState = isPouring ? FaucetState.POURING : FaucetState.OFF;
    this.renderFluid = fluid;
  }

  @Override
  protected boolean shouldSyncOnUpdate() {
    return true;
  }

  @Override
  protected void saveSynced(ValueOutput output) {
    super.saveSynced(output);
    output.putByte(TAG_STATE, (byte)faucetState.ordinal());
    if (!renderFluid.isEmpty()) {
      output.store(TAG_RENDER_FLUID, FluidStack.CODEC, renderFluid);
    }
  }

  @Override
  public void saveAdditional(ValueOutput output) {
    super.saveAdditional(output);
    output.putBoolean(TAG_STOP, stopPouring);
    output.putBoolean(TAG_LAST_REDSTONE, lastRedstoneState);
    if (!drained.isEmpty()) {
      output.store(TAG_DRAINED, FluidStack.CODEC, drained);
    }
  }

  @Override
  protected void loadAdditional(ValueInput input) {
    super.loadAdditional(input);
    faucetState = FaucetState.fromIndex(input.getByteOr(TAG_STATE, (byte)0));
    stopPouring = input.getBooleanOr(TAG_STOP, false);
    lastRedstoneState = input.getBooleanOr(TAG_LAST_REDSTONE, false);
    // fluids
    drained = input.read(TAG_DRAINED, FluidStack.CODEC).orElse(FluidStack.EMPTY);
    renderFluid = input.read(TAG_RENDER_FLUID, FluidStack.CODEC).orElse(FluidStack.EMPTY);
    cracksHandler.read(getBlockState(), input);
  }

  // NeoForge calls it by signature (IBlockEntityExtension), Fabric from its block entity load event
  public void onLoad() {
    cracksHandler.applyLegacyCracks();
  }

  @Override
  protected void collectImplicitComponents(DataComponentMap.Builder builder) {
    super.collectImplicitComponents(builder);
    cracksHandler.collectImplicitComponents(builder);
  }

  private enum FaucetState {
    OFF,
    POURING,
    POWERED;

    /** Gets the state for the given index */
    public static FaucetState fromIndex(int index) {
      return switch (index) {
        case 1 -> POURING;
        case 2 -> POWERED;
        default -> OFF;
      };
    }
  }
}

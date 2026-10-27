package knightminer.ceramics.blocks.entity;

import dev.vladeezyy.kilnwork.block.SyncedBlockEntity;
import knightminer.ceramics.Ceramics;
import knightminer.ceramics.blocks.CisternBlock;
import knightminer.ceramics.blocks.entity.CrackableBlockEntityHandler.ICrackableBlockEntity;
import knightminer.ceramics.items.BaseClayBucketItem;
import knightminer.ceramics.network.CeramicsNetwork;
import knightminer.ceramics.network.FluidUpdatePacket;
import knightminer.ceramics.network.FluidUpdatePacket.FluidUpdater;
import knightminer.ceramics.util.tank.CisternTank;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import dev.vladeezyy.kilnwork.fluid.api.FluidStack;
import dev.vladeezyy.kilnwork.fluid.api.IFluidHandler;
import dev.vladeezyy.kilnwork.fluid.api.IFluidHandler.FluidAction;

import javax.annotation.Nullable;

public class CisternBlockEntity extends SyncedBlockEntity implements ICrackableBlockEntity, FluidUpdater {
  private static final String TAG_FLUID = "fluid";
  private static final String TAG_EXTENSIONS = "extensions";

  /* Base fields */
  /** Tank instance for this cistern */
  private final CisternTank tank = new CisternTank(this);
  /** Number of extensions on this tank */
  private int extensions = 0;

  /**
   * Handler exposed as the block capability. It always forwards to the tank of the base cistern, so the object never
   * changes and neighbors do not need to drop their capability caches when the structure changes.
   */
  private final IFluidHandler publicHandler = new BaseForwardingHandler();
  /** Base cistern found for this extension, cleared when the structure changes */
  @Nullable
  private CisternBlockEntity cachedBase;
  /** Index of the cistern for rendering */
  private int renderIndex = 0;

  /** Cracking logic */
  private final CrackableBlockEntityHandler cracksHandler;

  public CisternBlockEntity(BlockPos pos, BlockState state, boolean crackable) {
    super(knightminer.ceramics.Registration.CISTERN_BLOCK_ENTITY.get(), pos, state);
    cracksHandler = new CrackableBlockEntityHandler(this, crackable);
  }

  public CisternBlockEntity(BlockPos pos, BlockState state) {
    this(pos, state, false);
  }


  /* Getters */

  /** Gets the number of extensions on this cistern */
  public int getExtensions() {
    return extensions;
  }

  /** Gets the index of this cistern in the structure for the sake of rendering */
  public int getRenderIndex() {
    findBase();
    return renderIndex;
  }

  /** Clears the cached base of this cistern and of all extensions above, as the structure changed */
  private void invalidateHandlers() {
    cachedBase = null;
    renderIndex = 0;
    if (level != null) {
      Block block = getBlockState().getBlock();
      BlockPos.MutableBlockPos pos = worldPosition.mutable();
      for (int i = 0; i < level.getMaxY() - worldPosition.getY(); i++) {
        pos.move(0, 1, 0);
        BlockState state = level.getBlockState(pos);
        if (!state.is(block) || !state.getValue(CisternBlock.EXTENSION) || !(level.getBlockEntity(pos) instanceof CisternBlockEntity cistern)) {
          break;
        }
        cistern.cachedBase = null;
        cistern.renderIndex = 0;
      }
    }
  }

  @Override
  @SuppressWarnings("deprecation")
  public void setBlockState(BlockState state) {
    boolean extensionChanged = !state.hasProperty(CisternBlock.EXTENSION) || !getBlockState().hasProperty(CisternBlock.EXTENSION)
      || state.getValue(CisternBlock.EXTENSION) != getBlockState().getValue(CisternBlock.EXTENSION);
    super.setBlockState(state);
    // became a base or an extension, extensions above must find their base again (cracks do not matter)
    if (extensionChanged) {
      invalidateHandlers();
    }
  }

  // NeoForge calls it by signature (IBlockEntityExtension), Fabric from its block entity load event
  public void onLoad() {
    cracksHandler.applyLegacyCracks();
  }

  /** Gets the capacity per layer of the block entity */
  public int capacityPerLayer() {
    Block block = getBlockState().getBlock();
    if (knightminer.ceramics.Registration.PORCELAIN_CISTERN.contains(block)) {
      return 6 * FluidStack.BUCKET_VOLUME;
    }
    return 4 * FluidStack.BUCKET_VOLUME;
  }

  /** Gets the capacity for the given height of the block entity */
  public final int capacityFor(int height) {
    return height * capacityPerLayer();
  }

  @Override
  public CrackableBlockEntityHandler getCracksHandler() {
    return cracksHandler;
  }


  /* Parent behavior */

  /** Transfers the relevant amount of fluid into a new base */
  private void transferBaseTo(Block block, int index) {
    assert level != null;
    BlockPos newBase = worldPosition.above(index);
    BlockState newState = level.getBlockState(newBase);
    if (newState.is(block) && newState.getValue(CisternBlock.EXTENSION)) {
      BlockEntity te = level.getBlockEntity(newBase);
      if (te instanceof CisternBlockEntity cistern) {
        // if we have fluid in the new base, extract that into that base
        FluidStack fluid = FluidStack.EMPTY;
        int amountAboveRemoved = tank.getFluidAmount() - capacityFor(index);
        if (amountAboveRemoved > 0) {
          fluid = tank.getFluid().copyWithAmount(amountAboveRemoved);
        }
        // subtract index, that is extensions below the index
        cistern.makeBase(fluid, extensions - index);
      }
    }
  }

  /** Called on the server before the block is removed or replaced by another block, was onRemove of the block */
  @Override
  public void preRemoveSideEffects(BlockPos pos, BlockState state) {
    super.preRemoveSideEffects(pos, state);
    if (state.getValue(CisternBlock.EXTENSION)) {
      CisternBlockEntity base = findBase();
      if (base != null && base != this) {
        base.removeExtension(pos);
      }
    } else {
      onBroken(state.getBlock());
    }
  }

  /** Called when a base block is broken to transfer relevant fluid to extensions */
  public void onBroken(Block block) {
    if (extensions > 0) {
      transferBaseTo(block, 1);
    }
  }

  /** Attempts to merge a cistern at the given position with this one */
  public void tryMerge(BlockPos checkPos) {
    assert level != null;
    if (level.getBlockState(checkPos).is(getBlockState().getBlock()) && level.getBlockEntity(checkPos) instanceof CisternBlockEntity cistern) {
      cistern.makeExtension(this);
    }
  }

  /** Adds a new cistern as an extension to this one. Called by the new cistern when its placed in the world. */
  public void addExtension(BlockPos extPos) {
    assert level != null;
    int newExtensions = extPos.getY() - worldPosition.getY();
    if (newExtensions > extensions) {
      extensions = newExtensions;
      setChangedFast();
      tryMerge(extPos.above());
    }
  }

  /** Removes the extension at the given location. Called on the parent when a cistern extension above is removed */
  public void removeExtension(BlockPos extPos) {
    assert level != null;
    int removed = extPos.getY() - worldPosition.getY();
    if (removed <= extensions) {
      if (removed < extensions) {
        transferBaseTo(getBlockState().getBlock(), removed + 1);
      }
      extensions = removed - 1;
      setChangedFast();
      tank.validateAmount();
    }
  }

  /** Called by the tank when fluid is added */
  public void onFluidAdded(int amountAdded) {
    onTankChanged(false);
    // if cracking is active and the fluid cracks the block, crack all blocks impacted by the fluid
    FluidStack fluid = tank.getFluid();
    if (cracksHandler.isActive() && level != null && !level.isClientSide() && BaseClayBucketItem.doesCrack(fluid.getFluid())) {
      int capacityPerLayer = capacityPerLayer();
      int startIndex = (fluid.getAmount() - amountAdded + capacityPerLayer - 1) / capacityPerLayer;
      int endIndex = (fluid.getAmount() - 1) / capacityPerLayer;
      for (int i = startIndex; i <= endIndex; i++) {
        if (level.getBlockEntity(worldPosition.above(i)) instanceof ICrackableBlockEntity crackable) {
          crackable.getCracksHandler().fluidAdded(fluid);
        }
      }
    }
  }


  /* Extension behavior */

  /** Finds the base cistern of this structure, updating the render index */
  @Nullable
  private CisternBlockEntity findBase() {
    if (!getBlockState().getValue(CisternBlock.EXTENSION)) {
      renderIndex = 0;
      return this;
    }
    if (cachedBase != null && !cachedBase.isRemoved() && cachedBase.level == level && !cachedBase.getBlockState().getValue(CisternBlock.EXTENSION)) {
      return cachedBase;
    }
    cachedBase = null;
    renderIndex = 0;
    if (level == null) {
      Ceramics.LOG.error("No world when trying to get cistern's fluid handler");
      return null;
    }
    Block block = getBlockState().getBlock();
    BlockPos.MutableBlockPos pos = worldPosition.mutable();
    int minY = level.getMinY();
    BlockState state;
    do {
      pos.move(0, -1, 0);
      state = level.getBlockState(pos);
    } while (pos.getY() > minY && state.is(block) && state.getValue(CisternBlock.EXTENSION));
    if (state.is(block) && level.getBlockEntity(pos) instanceof CisternBlockEntity base) {
      cachedBase = base;
      renderIndex = worldPosition.getY() - pos.getY();
      return base;
    }
    if (!level.isClientSide()) {
      Ceramics.LOG.error("Missing cistern tile entity below cistern extension");
    }
    return null;
  }

  /** Gets the tank holding the fluid for this cistern, which is the tank of the base for extensions */
  protected CisternTank getInternalTank() {
    CisternBlockEntity base = findBase();
    return base == null ? tank : base.tank;
  }

  /** Gets the public fluid handler associated with this cistern, registered as the block capability */
  public IFluidHandler getPublicHandler() {
    return publicHandler;
  }

  /** Fluid of the structure, for rendering and checks */
  public FluidStack getFluid() {
    return getInternalTank().getFluid();
  }

  /** Called on random block tick to update the heat */
  public void randomTick() {
    if (cracksHandler.isActive()) {
      if (getBlockState().getValue(CisternBlock.EXTENSION)) {
        FluidStack fluid = getInternalTank().getFluid();
        int localAmount = fluid.getAmount() - capacityFor(renderIndex);
        if (localAmount > 0) {
          cracksHandler.updateCracks(fluid.getFluid(), localAmount);
        } else {
          cracksHandler.updateCracks(Fluids.EMPTY, 0);
        }
      } else {
        FluidStack fluid = tank.getFluid();
        cracksHandler.updateCracks(fluid.getFluid(), Math.min(fluid.getAmount(), capacityPerLayer()));
      }
    }
  }


  /* State updating */

  /** Makes the cistern a base */
  protected void makeBase(FluidStack fluid, int extensions) {
    assert level != null;
    this.extensions = extensions;
    tank.setFluid(fluid);
    onTankChanged(true);
    level.setBlockAndUpdate(worldPosition, getBlockState().setValue(CisternBlock.EXTENSION, false));
    invalidateHandlers();
    setChangedFast();
  }

  /** Checks if the tank contains the given fluid */
  public boolean containsFluid(FluidStack fluid) {
    return tank.getFluidAmount() == 0 || FluidStack.isSameFluidSameComponents(tank.getFluid(), fluid);
  }

  /** Merges the contents of another cistern into this */
  protected void mergeCisterns(FluidStack fluid, int newExtensions) {
    extensions += newExtensions;
    if (!fluid.isEmpty()) {
      tank.fill(fluid, FluidAction.EXECUTE);
    }
    invalidateHandlers();
    setChangedFast();
  }

  /** Converts a given base cistern into an extension. Called by the base when a new cistern is added below an existing one */
  protected void makeExtension(CisternBlockEntity parent) {
    assert level != null;
    FluidStack fluid = tank.getFluid();
    if (fluid.isEmpty() || parent.containsFluid(fluid)) {
      level.setBlockAndUpdate(worldPosition, getBlockState().setValue(CisternBlock.EXTENSION, true));
      parent.mergeCisterns(fluid, extensions + 1);
      invalidateHandlers();
      extensions = 0;
      tank.setFluid(FluidStack.EMPTY);
      onTankChanged(true);
      setChangedFast();
    }
  }


  /* Networking */

  /** Called when the tank contents change */
  public void onTankChanged(boolean shouldRefreshCapabilities) {
    if (level != null && !level.isClientSide()) {
      setChangedFast();
      CeramicsNetwork.sendToClientsAround(new FluidUpdatePacket(worldPosition, tank.getFluid(), shouldRefreshCapabilities), level, worldPosition);
    }
  }

  @Override
  public void updateFluid(FluidStack fluid, boolean shouldRefreshCapabilities) {
    tank.setFluid(fluid);
    if (shouldRefreshCapabilities) {
      invalidateHandlers();
    }
  }

  @Override
  protected boolean shouldSyncOnUpdate() {
    return true;
  }


  /* Serialization */

  @Override
  protected void loadAdditional(ValueInput input) {
    super.loadAdditional(input);
    extensions = input.getIntOr(TAG_EXTENSIONS, 0);
    tank.setFluid(input.read(TAG_FLUID, FluidStack.OPTIONAL_CODEC).orElse(FluidStack.EMPTY));
    cracksHandler.read(getBlockState(), input);
  }

  @Override
  protected void saveSynced(ValueOutput output) {
    super.saveSynced(output);
    if (tank.getFluidAmount() > 0) {
      output.store(TAG_FLUID, FluidStack.OPTIONAL_CODEC, tank.getFluid());
    }
  }

  @Override
  protected void saveAdditional(ValueOutput output) {
    super.saveAdditional(output);
    output.putInt(TAG_EXTENSIONS, extensions);
  }

  @Override
  protected void collectImplicitComponents(DataComponentMap.Builder builder) {
    super.collectImplicitComponents(builder);
    cracksHandler.collectImplicitComponents(builder);
  }


  /** Fluid handler forwarding to the tank of the base cistern */
  private class BaseForwardingHandler implements IFluidHandler {
    @Override
    public int getTanks() {
      return 1;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
      return getInternalTank().getFluidInTank(tank);
    }

    @Override
    public int getTankCapacity(int tank) {
      return getInternalTank().getTankCapacity(tank);
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
      return getInternalTank().isFluidValid(tank, stack);
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
      return getInternalTank().fill(resource, action);
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
      return getInternalTank().drain(resource, action);
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
      return getInternalTank().drain(maxDrain, action);
    }
  }
}

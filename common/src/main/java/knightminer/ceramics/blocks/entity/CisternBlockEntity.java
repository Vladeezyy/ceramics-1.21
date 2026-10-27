package knightminer.ceramics.blocks.entity;

import knightminer.ceramics.Ceramics;
import knightminer.ceramics.Registration;
import knightminer.ceramics.blocks.CisternBlock;
import knightminer.ceramics.blocks.entity.CrackableBlockEntityHandler.ICrackableBlockEntity;
import knightminer.ceramics.items.BaseClayBucketItem;
import knightminer.ceramics.network.CeramicsNetwork;
import knightminer.ceramics.network.FluidUpdatePacket;
import knightminer.ceramics.network.FluidUpdatePacket.FluidUpdater;
import knightminer.ceramics.util.tank.CisternTank;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import dev.vladeezyy.kilnwork.fluid.api.FluidStack;
import dev.vladeezyy.kilnwork.fluid.api.IFluidHandler;
import dev.vladeezyy.kilnwork.fluid.api.IFluidHandler.FluidAction;
import dev.vladeezyy.kilnwork.block.SyncedBlockEntity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class CisternBlockEntity extends SyncedBlockEntity implements ICrackableBlockEntity, FluidUpdater {
  /** Max capacity per cistern block */
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
   * In 1.20 this was a chain of lazy optionals with invalidation listeners.
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
    super(Registration.CISTERN_BLOCK_ENTITY.get(), pos, state);
    cracksHandler = new CrackableBlockEntityHandler(this, crackable);
  }

  public CisternBlockEntity(BlockPos pos, BlockState state) {
    this(pos, state, false);
  }


  /* Getters */

  /**
   * Gets the number of extensions on this cistern
   * @return  Number of extensions
   */
  public int getExtensions() {
    return extensions;
  }

  /**
   * Gets the index of this cistern in the structure for the sake of rendering
   * @return  Render index of cistern
   */
  public int getRenderIndex() {
    findBase();
    return renderIndex;
  }

  /**
   * Clears the cached base of this cistern and of all extensions above, as the structure changed
   */
  private void invalidateHandlers() {
    cachedBase = null;
    renderIndex = 0;
    if (level != null) {
      Block block = getBlockState().getBlock();
      BlockPos.MutableBlockPos pos = worldPosition.mutable();
      for (int i = 0; i < level.getMaxBuildHeight() - worldPosition.getY(); i++) {
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
  public void setBlockState(BlockState state) {
    super.setBlockState(state);
    // became a base or an extension, extensions above must find their base again
    invalidateHandlers();
  }

  /**
   * Gets the capacity per layer of the tile entity
   * @return  Capacity per layer
   */
  public int capacityPerLayer() {
    Block block = getBlockState().getBlock();
    if (Registration.PORCELAIN_CISTERN.contains(block)) {
      return 6 * FluidStack.BUCKET_VOLUME;
    }
    return 4 * FluidStack.BUCKET_VOLUME;
  }

  /**
   * Gets the capacity for the given height of the tile entity
   * @param height  Cistern height
   * @return  Capacity
   */
  public final int capacityFor(int height) {
    return height * capacityPerLayer();
  }

  @Override
  public CrackableBlockEntityHandler getCracksHandler() {
    return cracksHandler;
  }

  @Nonnull


  /* Parent behavior */

  /**
   * Transfers the relevant amount of fluid into a new base
   * @param index  Number of blocks the new base is above this block
   */
  private void transferBaseTo(Block block, int index) {
    assert level != null;
    // ensure the block above is the right type and is an extension
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

  /**
   * Called when a base block is broken to transfer relevant fluid to extensions
   * @param block  Block type, used since the block at the position may be air at the time of calling
   */
  public void onBroken(Block block) {
    if (extensions > 0) {
      transferBaseTo(block, 1);
    }
  }

  /**
   * Attempts to merge a cistern at the given position with this one
   * @param checkPos  Position to check
   */
  public void tryMerge(BlockPos checkPos) {
    assert level != null;
    // check if there is another cistern above the new one that should also be connected
    if (level.getBlockState(checkPos).is(getBlockState().getBlock()) && level.getBlockEntity(checkPos) instanceof CisternBlockEntity cistern) {
      cistern.makeExtension(this);
    }
  }

  /**
   * Adds a new cistern as an extension to this one. Called by the new cistern when its placed in the world.
   * @param extPos  Position of extension
   */
  public void addExtension(BlockPos extPos) {
    assert level != null;
    // update extensions if the new position is outside the cistern (mostly safety)
    int newExtensions = extPos.getY() - worldPosition.getY();
    if (newExtensions > extensions) {
      extensions = newExtensions;
      setChangedFast();
      // check if there is another cistern above the new one that should also be connected
      tryMerge(extPos.above());
    }
  }

  /**
   * Removes the extension at the given location. Called on the parent when a cistern extension above is removed
   * @param extPos  Extension to remove
   */
  public void removeExtension(BlockPos extPos) {
    assert extPos.getY() > worldPosition.getY();
    assert level != null;
    // index of the extension that was removed
    int removed = extPos.getY() - worldPosition.getY();
    // check if the removed position was within our current cistern
    if (removed <= extensions) {
      // if not the top, make a new base
      if (removed < extensions) {
        transferBaseTo(getBlockState().getBlock(), removed + 1);
      }
      // our new extensions simply one less than the removed index
      extensions = removed - 1;
      setChangedFast();
      // shrink the fluid if needed
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
      // first index needing an update
      int startIndex = (fluid.getAmount() - amountAdded + capacityPerLayer - 1) / capacityPerLayer;
      // last index needing an update
      int endIndex = (fluid.getAmount() - 1) / capacityPerLayer;
      // loop through indexes, updating them
      for (int i = startIndex; i <= endIndex; i++) {
        if (level.getBlockEntity(worldPosition.above(i)) instanceof ICrackableBlockEntity crackable) {
          crackable.getCracksHandler().fluidAdded(fluid);
        }
      }
    }
  }


  /* Extension behavior */

  /**
   * Finds the base cistern of this structure, updating the render index
   * @return  Base cistern, this if not an extension, null if the base is missing
   */
  @Nullable
  private CisternBlockEntity findBase() {
    if (!getBlockState().getValue(CisternBlock.EXTENSION)) {
      renderIndex = 0;
      return this;
    }
    // cached base still valid?
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
    int minY = level.getMinBuildHeight();
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
      // render thread often has a few ticks with no cistern for an extension the base is broken
      Ceramics.LOG.error("Missing cistern tile entity below cistern extension");
    }
    return null;
  }

  /**
   * Gets the tank holding the fluid for this cistern, which is the tank of the base for extensions
   * @return  Tank
   */
  protected CisternTank getInternalTank() {
    CisternBlockEntity base = findBase();
    return base == null ? tank : base.tank;
  }

  /**
   * Gets the public fluid handler associated with this cistern, registered as the block capability
   * @return  Fluid handler
   */
  public IFluidHandler getPublicHandler() {
    return publicHandler;
  }

  /** Called on random block tick to update the heat */
  public void randomTick() {
    if (cracksHandler.isActive()) {
      if (getBlockState().getValue(CisternBlock.EXTENSION)) {
        // extensions: if fluid does not reach here, treat as empty
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

  /**
   * Makes the barrel a base
   * @param fluid       Fluid to contain
   * @param extensions  Number of extensions
   */
  protected void makeBase(FluidStack fluid, int extensions) {
    assert level != null;
    // extra extra data from parent
    this.extensions = extensions;
    tank.setFluid(fluid);
    // notify client and any listening blocks of changes
    onTankChanged(true);
    // update block state in world, invalidates the extensions above
    level.setBlockAndUpdate(worldPosition, getBlockState().setValue(CisternBlock.EXTENSION, false));
    invalidateHandlers();
    setChangedFast();
  }

  /**
   * Checks if the tank contains the given fluid
   * @param fluid  Fluid to check
   * @return  True if the fluid is contained, false otherwise
   */
  public boolean containsFluid(FluidStack fluid) {
    return tank.getFluidAmount() == 0 || FluidStack.isSameFluidSameComponents(tank.getFluid(), fluid);
  }

  /**
   * Merges the contents of another cistern into this
   * @param fluid          Fluid contents to merge in
   * @param newExtensions  New extensions to add
   */
  protected void mergeCisterns(FluidStack fluid, int newExtensions) {
    extensions += newExtensions;
    if (!fluid.isEmpty()) {
      tank.fill(fluid, FluidAction.EXECUTE);
    }
    invalidateHandlers();
    setChangedFast();
  }

  /**
   * Converts a given base cistern into an extension. Called by the base when a new cistern is added below an existing one
   * @param parent  New parent to the extension
   */
  protected void makeExtension(CisternBlockEntity parent) {
    assert level != null;
    // ensure the fluid is valid for the parent
    FluidStack fluid = tank.getFluid();
    if (fluid.isEmpty() || parent.containsFluid(fluid)) {
      // update block to extension block
      level.setBlockAndUpdate(worldPosition, getBlockState().setValue(CisternBlock.EXTENSION, true));
      // merge contents into the parent, add an extra 1 for ourself
      parent.mergeCisterns(fluid, extensions + 1);
      // clear our data
      invalidateHandlers();
      extensions = 0;
      tank.setFluid(FluidStack.EMPTY);
      // tell the client the fluid now lives in the parent
      onTankChanged(true);
      setChangedFast();
    }
  }


  /* Networking */

  /**
   * Called when the tank contents change
   */
  public void onTankChanged(boolean shouldRefreshCapabilities) {
    if(level != null && !level.isClientSide) {
      setChangedFast();
      CeramicsNetwork.sendToClientsAround(new FluidUpdatePacket(worldPosition, tank.getFluid(), shouldRefreshCapabilities), level, worldPosition);
    }
  }

  /**
   * Called client side to update the tank fluid
   * @param fluid                      New fluid
   * @param shouldRefreshCapabilities  If true, capability handlers should be invalidated. Typically means the block was changed from extension to base
   */
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
  protected void loadAdditional(CompoundTag tags, HolderLookup.Provider registries) {
    super.loadAdditional(tags, registries);
    extensions = tags.getInt(TAG_EXTENSIONS);
    if (tags.contains(TAG_FLUID, Tag.TAG_COMPOUND)) {
      tank.readFromNBT(registries, tags.getCompound(TAG_FLUID));
    } else {
      tank.setFluid(FluidStack.EMPTY);
    }
    cracksHandler.readNBT(getBlockState(), tags);
  }

  @Override
  protected void saveSynced(CompoundTag compound, HolderLookup.Provider registries) {
    super.saveSynced(compound, registries);
    if (tank.getFluidAmount() > 0) {
      compound.put(TAG_FLUID, tank.writeToNBT(registries));
    }
    cracksHandler.writeNBT(compound);
  }

  @Override
  public void saveAdditional(CompoundTag compound, HolderLookup.Provider registries) {
    super.saveAdditional(compound, registries);
    compound.putInt(TAG_EXTENSIONS, extensions);
  }

  @Override
  protected void collectImplicitComponents(DataComponentMap.Builder builder) {
    super.collectImplicitComponents(builder);
    cracksHandler.collectImplicitComponents(builder);
  }

  @Override
  protected void applyImplicitComponents(DataComponentInput input) {
    super.applyImplicitComponents(input);
    cracksHandler.applyImplicitComponents(input.get(Registration.CRACKS.get()));
  }

  @Override
  public void removeComponentsFromTag(CompoundTag tag) {
    super.removeComponentsFromTag(tag);
    tag.remove(CrackableBlockEntityHandler.TAG_CRACKS);
  }


  /** Fluid handler forwarding to the tank of the base cistern */
  private class BaseForwardingHandler implements IFluidHandler {
    @Override
    public int getTanks() {
      return 1;
    }

    @Override
    public FluidStack getFluidInTank(int index) {
      return getInternalTank().getFluidInTank(index);
    }

    @Override
    public int getTankCapacity(int index) {
      return getInternalTank().getTankCapacity(index);
    }

    @Override
    public boolean isFluidValid(int index, FluidStack stack) {
      return getInternalTank().isFluidValid(index, stack);
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

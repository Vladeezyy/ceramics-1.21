package knightminer.ceramics.util.tank;

import knightminer.ceramics.blocks.entity.ChannelBlockEntity;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/** Tank for channel contents */
public class ChannelTank extends SingleFluidTank<ChannelTank.State> {
  /**
   * Amount of fluid that may not be extracted this tick
   * Essentially, since we cannot guarantee tick order, this prevents us from having a net 0 fluid for the renderer
   * if draining and filling at the same time
   */
  private int locked;
  private final int capacity;
  /** Tank owner */
  private final ChannelBlockEntity parent;

  record State(FluidStack fluid, int locked) {}

  public ChannelTank(int capacity, ChannelBlockEntity parent) {
    this.capacity = capacity;
    this.parent = parent;
  }

  @Override
  public int getCapacity() {
    return capacity;
  }

  /**
   * Called on channel update to clear the lock, allowing this fluid to be drained
   */
  public void freeFluid() {
    this.locked = 0;
  }

  /**
   * Returns the maximum fluid that can be extracted from this tank
   * @return  Max fluid that can be pulled
   */
  public int getMaxUsable() {
    return Math.max(fluid.getAmount() - locked, 0);
  }

  public int getLocked() {
    return locked;
  }

  public void setLocked(int locked) {
    this.locked = locked;
  }

  @Override
  public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
    int inserted = super.insert(index, resource, amount, transaction);
    if (inserted > 0) {
      locked += inserted;
    }
    return inserted;
  }

  @Override
  protected State createSnapshot() {
    return new State(fluid, locked);
  }

  @Override
  protected void revertToSnapshot(State snapshot) {
    fluid = snapshot.fluid;
    locked = snapshot.locked;
  }

  @Override
  protected void onRootCommit(State original) {
    if (fluid.getAmount() > original.fluid.getAmount()) {
      // crack the channel if hot and crackable
      parent.getCracksHandler().fluidAdded(fluid);
    }
    // sync to the client when it became empty or got fluid
    if (original.fluid.isEmpty() != fluid.isEmpty()) {
      parent.sendFluidUpdate();
    }
  }
}

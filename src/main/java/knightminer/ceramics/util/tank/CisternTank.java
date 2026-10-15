package knightminer.ceramics.util.tank;

import knightminer.ceramics.blocks.entity.CisternBlockEntity;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Handles all tank behaviors for the cistern
 */
public class CisternTank extends SingleFluidTank<FluidStack> {
  /** Relevant block entity */
  protected final CisternBlockEntity parent;

  public CisternTank(CisternBlockEntity parent) {
    this.parent = parent;
  }

  @Override
  public int getCapacity() {
    // 1 for the base, plus 1 per extension
    return parent.capacityFor(parent.getExtensions() + 1);
  }

  /** Ensures the tank does not have too much fluid */
  public void validateAmount() {
    int extra = fluid.getAmount() - getCapacity();
    if (extra > 0) {
      drainNow(extra);
    }
  }

  @Override
  protected FluidStack createSnapshot() {
    return fluid;
  }

  @Override
  protected void revertToSnapshot(FluidStack snapshot) {
    fluid = snapshot;
  }

  @Override
  protected void onRootCommit(FluidStack original) {
    int added = fluid.getAmount() - original.getAmount();
    if (added > 0) {
      parent.onFluidAdded(added);
    } else if (added < 0) {
      parent.onTankChanged(false);
    }
  }
}

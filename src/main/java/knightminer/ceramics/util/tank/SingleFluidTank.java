package knightminer.ceramics.util.tank;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import java.util.Objects;

/**
 * Tank holding one fluid, for the transfer API: every change is journaled so an aborted transaction undoes it.
 * Since NeoForge 21.9 fluids move in transactions instead of the fill/drain with simulate of IFluidHandler.
 * @param <S>  Snapshot type, at least the fluid
 */
public abstract class SingleFluidTank<S> extends SnapshotJournal<S> implements ResourceHandler<FluidResource> {
  protected FluidStack fluid = FluidStack.EMPTY;

  /** Capacity of the tank */
  public abstract int getCapacity();

  public FluidStack getFluid() {
    return fluid;
  }

  /** Replaces the fluid without a transaction, for loading and syncing */
  public void setFluid(FluidStack fluid) {
    this.fluid = fluid;
  }

  public int getFluidAmount() {
    return fluid.getAmount();
  }

  public boolean isEmpty() {
    return fluid.isEmpty();
  }

  /** True if the fluid may go in this tank */
  protected boolean isFluidValid(FluidResource resource) {
    return true;
  }

  @Override
  public int size() {
    return 1;
  }

  @Override
  public FluidResource getResource(int index) {
    Objects.checkIndex(index, 1);
    return FluidResource.of(fluid);
  }

  @Override
  public long getAmountAsLong(int index) {
    Objects.checkIndex(index, 1);
    return fluid.getAmount();
  }

  @Override
  public long getCapacityAsLong(int index, FluidResource resource) {
    Objects.checkIndex(index, 1);
    return resource.isEmpty() || isFluidValid(resource) ? getCapacity() : 0;
  }

  @Override
  public boolean isValid(int index, FluidResource resource) {
    Objects.checkIndex(index, 1);
    return isFluidValid(resource);
  }

  @Override
  public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
    Objects.checkIndex(index, 1);
    TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
    if (!isFluidValid(resource) || !fluid.isEmpty() && !resource.matches(fluid)) {
      return 0;
    }
    int inserted = Math.min(amount, getCapacity() - fluid.getAmount());
    if (inserted > 0) {
      updateSnapshots(transaction);
      fluid = fluid.isEmpty() ? resource.toStack(inserted) : fluid.copyWithAmount(fluid.getAmount() + inserted);
    }
    return Math.max(inserted, 0);
  }

  @Override
  public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
    Objects.checkIndex(index, 1);
    TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
    if (fluid.isEmpty() || !resource.matches(fluid)) {
      return 0;
    }
    int extracted = Math.min(amount, fluid.getAmount());
    if (extracted > 0) {
      updateSnapshots(transaction);
      fluid = extracted == fluid.getAmount() ? FluidStack.EMPTY : fluid.copyWithAmount(fluid.getAmount() - extracted);
    }
    return extracted;
  }

  /* Helpers without a transaction from the caller */

  /** Fills right away, returns the amount filled */
  public int fillNow(FluidStack stack) {
    if (stack.isEmpty()) {
      return 0;
    }
    try (Transaction tx = Transaction.openRoot()) {
      int filled = insert(0, FluidResource.of(stack), stack.getAmount(), tx);
      tx.commit();
      return filled;
    }
  }

  /** Drains up to the amount right away, returns what came out */
  public FluidStack drainNow(int amount) {
    if (fluid.isEmpty() || amount <= 0) {
      return FluidStack.EMPTY;
    }
    try (Transaction tx = Transaction.openRoot()) {
      FluidResource resource = FluidResource.of(fluid);
      int drained = extract(0, resource, amount, tx);
      tx.commit();
      return resource.toStack(drained);
    }
  }
}

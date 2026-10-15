package knightminer.ceramics.util.tank;

import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * Fluid handler wrapper that only allows filling
 */
public class FillOnlyFluidHandler implements ResourceHandler<FluidResource> {
  private final ResourceHandler<FluidResource> parent;

  public FillOnlyFluidHandler(ResourceHandler<FluidResource> parent) {
    this.parent = parent;
  }

  @Override
  public int size() {
    return parent.size();
  }

  @Override
  public FluidResource getResource(int index) {
    return parent.getResource(index);
  }

  @Override
  public long getAmountAsLong(int index) {
    return parent.getAmountAsLong(index);
  }

  @Override
  public long getCapacityAsLong(int index, FluidResource resource) {
    return parent.getCapacityAsLong(index, resource);
  }

  @Override
  public boolean isValid(int index, FluidResource resource) {
    return parent.isValid(index, resource);
  }

  @Override
  public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
    return parent.insert(index, resource, amount, transaction);
  }

  @Override
  public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
    return 0;
  }
}

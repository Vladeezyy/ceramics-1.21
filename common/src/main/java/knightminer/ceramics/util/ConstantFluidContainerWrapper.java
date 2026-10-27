package knightminer.ceramics.util;

import knightminer.ceramics.items.BaseClayBucketItem;
import net.minecraft.world.item.ItemStack;
import dev.vladeezyy.kilnwork.fluid.api.FluidStack;
import dev.vladeezyy.kilnwork.fluid.api.IFluidHandlerItem;

/**
 * Fluid handler for an item that always holds the same fluid, and can only be emptied fully into its container item.
 * Mantle had this class in 1.20, it is gone in Mantle 1.21.
 */
public class ConstantFluidContainerWrapper implements IFluidHandlerItem {
  private final FluidStack fluid;
  private ItemStack container;

  public ConstantFluidContainerWrapper(FluidStack fluid, ItemStack container) {
    this.fluid = fluid;
    this.container = container;
  }

  @Override
  public ItemStack getContainer() {
    return container;
  }

  @Override
  public int getTanks() {
    return 1;
  }

  @Override
  public FluidStack getFluidInTank(int tank) {
    return fluid.copy();
  }

  @Override
  public int getTankCapacity(int tank) {
    return fluid.getAmount();
  }

  @Override
  public boolean isFluidValid(int tank, FluidStack stack) {
    return false;
  }

  @Override
  public int fill(FluidStack resource, FluidAction action) {
    return 0;
  }

  @Override
  public FluidStack drain(FluidStack resource, FluidAction action) {
    if (!FluidStack.isSameFluidSameComponents(resource, fluid)) {
      return FluidStack.EMPTY;
    }
    return drain(resource.getAmount(), action);
  }

  @Override
  public FluidStack drain(int maxDrain, FluidAction action) {
    // can only take out all of the fluid at once, and only from a single item
    if (container.getCount() != 1 || maxDrain < fluid.getAmount()) {
      return FluidStack.EMPTY;
    }
    if (action.execute()) {
      container = BaseClayBucketItem.remainder(container);
    }
    return fluid.copy();
  }
}

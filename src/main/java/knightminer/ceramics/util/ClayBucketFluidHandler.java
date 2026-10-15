package knightminer.ceramics.util;

import knightminer.ceramics.items.BaseClayBucketItem;
import knightminer.ceramics.items.SolidClayBucketItem;
import knightminer.ceramics.recipe.CeramicsTags;
import dev.vladeezyy.kilnwork.tag.TagPreference;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.transfer.ItemAccessResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import java.util.Objects;

/**
 * Fluid handler of clay buckets: a full bucket holds 1000 mB, filling turns an empty bucket into a filled one (cracked
 * for hot fluids), emptying turns it back, or breaks it if cracked. Replaces the 1.20 FluidBucketWrapper subclass.
 * Powder snow buckets count as the powdered snow fluid if a mod adds one.
 */
public class ClayBucketFluidHandler extends ItemAccessResourceHandler<FluidResource> {
  public ClayBucketFluidHandler(ItemAccess itemAccess) {
    super(itemAccess, 1);
  }

  /** Fluid inside the bucket of the given resource */
  private static Fluid getFluid(ItemResource resource) {
    ItemStack stack = resource.toStack();
    if (stack.getItem() instanceof SolidClayBucketItem solid) {
      return solid.getBlock(stack) == Blocks.POWDER_SNOW ? TagPreference.getPreference(CeramicsTags.Fluids.POWDERED_SNOW).orElse(Fluids.EMPTY) : Fluids.EMPTY;
    }
    if (stack.getItem() instanceof BaseClayBucketItem bucket) {
      return bucket.getFluid(stack);
    }
    return Fluids.EMPTY;
  }

  @Override
  protected FluidResource getResourceFrom(ItemResource accessResource, int index) {
    Fluid fluid = getFluid(accessResource);
    return fluid == Fluids.EMPTY ? FluidResource.EMPTY : FluidResource.of(fluid);
  }

  @Override
  protected int getAmountFrom(ItemResource accessResource, int index) {
    return getResourceFrom(accessResource, index).isEmpty() ? 0 : FluidType.BUCKET_VOLUME;
  }

  @Override
  public boolean isValid(int index, FluidResource resource) {
    // blacklisted fluids and fluids with extra data do not go in clay buckets
    return !resource.getFluid().is(CeramicsTags.Fluids.BUCKET_BLACKLIST) && resource.isComponentsPatchEmpty();
  }

  @Override
  protected ItemResource update(ItemResource accessResource, int index, FluidResource newResource, int newAmount) {
    if (newAmount != FluidType.BUCKET_VOLUME || !(accessResource.getItem() instanceof BaseClayBucketItem bucket)) {
      // clay buckets only hold whole buckets
      return ItemResource.EMPTY;
    }
    return ItemResource.of(BaseClayBucketItem.withFluid(newResource.getFluid(), bucket.isCracked()));
  }

  @Override
  protected int getCapacity(int index, FluidResource resource) {
    Objects.checkIndex(index, size());
    return FluidType.BUCKET_VOLUME;
  }

  @Override
  public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
    Objects.checkIndex(index, size());
    TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
    int count = itemAccess.getAmount();
    ItemResource current = itemAccess.getResource();
    // only whole buckets come out
    if (count == 0 || amount < count * FluidType.BUCKET_VOLUME || !resource.equals(getResourceFrom(current, index))) {
      return 0;
    }
    ItemStack remainder = knightminer.ceramics.items.BaseClayBucketItem.remainder(current.toStack());
    int done = remainder.isEmpty()
      // cracked buckets break when emptied
      ? itemAccess.extract(current, count, transaction)
      : itemAccess.exchange(ItemResource.of(remainder), count, transaction);
    return done * FluidType.BUCKET_VOLUME;
  }
}

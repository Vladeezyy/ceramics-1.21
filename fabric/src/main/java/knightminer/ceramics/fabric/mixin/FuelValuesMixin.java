package knightminer.ceramics.fabric.mixin;

import knightminer.ceramics.items.FluidClayBucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.FuelValues;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A lava clay bucket is fuel (NeoForge asks the item for each stack, the fuel values only know items) */
@Mixin(FuelValues.class)
public abstract class FuelValuesMixin {
  @Inject(method = "burnDuration", at = @At("HEAD"), cancellable = true)
  private void ceramics$clayBucketBurnDuration(ItemStack fuel, CallbackInfoReturnable<Integer> cir) {
    if (fuel.getItem() instanceof FluidClayBucketItem bucket) {
      cir.setReturnValue(bucket.getBurnTime(fuel, null, (FuelValues) (Object) this));
    }
  }

  @Inject(method = "isFuel", at = @At("HEAD"), cancellable = true)
  private void ceramics$clayBucketIsFuel(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
    if (stack.getItem() instanceof FluidClayBucketItem bucket) {
      cir.setReturnValue(bucket.getBurnTime(stack, null, (FuelValues) (Object) this) > 0);
    }
  }
}

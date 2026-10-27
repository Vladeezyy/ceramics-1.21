package knightminer.ceramics.fabric.mixin.client;

import knightminer.ceramics.fabric.FabricCeramicsPlatform;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Where NeoForge fires PlayerDestroyItemEvent on the client: an item used up by a click (cracked clay buckets break) */
@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeMixin {
  @Unique
  private ItemStack ceramics$held = ItemStack.EMPTY;

  @Inject(method = "useItem", at = @At("HEAD"))
  private void ceramics$rememberHeld(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
    ceramics$held = player.getItemInHand(hand).copy();
  }

  @Inject(method = "useItemOn", at = @At("HEAD"))
  private void ceramics$rememberHeldOn(LocalPlayer player, InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
    ceramics$held = player.getItemInHand(hand).copy();
  }

  @Inject(method = "useItem", at = @At("RETURN"))
  private void ceramics$useItemDestroyed(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
    ceramics$fire(player, hand);
  }

  @Inject(method = "useItemOn", at = @At("RETURN"))
  private void ceramics$useItemOnDestroyed(LocalPlayer player, InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
    ceramics$fire(player, hand);
  }

  @Unique
  private void ceramics$fire(Player player, InteractionHand hand) {
    if (!ceramics$held.isEmpty() && player.getItemInHand(hand).isEmpty()) {
      FabricCeramicsPlatform.fireItemDestroyed(player, ceramics$held);
    }
    ceramics$held = ItemStack.EMPTY;
  }
}

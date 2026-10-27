package knightminer.ceramics.fabric.client;

import dev.vladeezyy.kilnwork.fabric.client.KilnworkFabricClient;
import dev.vladeezyy.kilnwork.fluid.api.FluidStack;
import dev.vladeezyy.kilnwork.platform.KilnworkClientPlatform;
import knightminer.ceramics.Ceramics;
import knightminer.ceramics.Registration;
import knightminer.ceramics.client.CeramicsClient;
import knightminer.ceramics.items.FluidClayBucketItem;
import knightminer.ceramics.items.SolidClayBucketItem;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/** Client entry of Ceramics on Fabric */
public class CeramicsFabricClient implements ClientModInitializer {
  @Override
  public void onInitializeClient() {
    CeramicsClient.init();
    CeramicsClient.cutoutBlocks().forEach(block -> BlockRenderLayerMap.INSTANCE.putBlock(block, RenderType.cutout()));
    CeramicsModelsFabric.register();
    // the contents of clay buckets are tint index 1 of their models (ClayBucketModelFabric)
    ColorProviderRegistry.ITEM.register((stack, tintIndex) -> tintIndex == 1 ? contentsColor(stack) : -1,
      Registration.FLUID_CLAY_BUCKET.get(), Registration.CRACKED_FLUID_CLAY_BUCKET.get(), Registration.SOLID_CLAY_BUCKET.get(), Registration.CRACKED_SOLID_CLAY_BUCKET.get());
    KilnworkFabricClient.register();
    if (Boolean.getBoolean("ceramics.scene")) {
      try {
        Class.forName("knightminer.ceramics.selftest.fabric.SelfTestFabric").getMethod("initClient").invoke(null);
      } catch (ReflectiveOperationException e) {
        Ceramics.LOG.error("Could not start the client scene", e);
      }
    }
  }

  private static int contentsColor(ItemStack stack) {
    if (stack.getItem() instanceof FluidClayBucketItem bucket) {
      Fluid fluid = bucket.getFluid(stack);
      return fluid == Fluids.EMPTY ? -1 : KilnworkClientPlatform.get().tint(new FluidStack(fluid, FluidStack.BUCKET_VOLUME), null, null);
    }
    if (stack.getItem() instanceof SolidClayBucketItem bucket) {
      Block block = bucket.getBlock(stack);
      net.minecraft.client.color.item.ItemColor color = block.asItem() == Items.AIR ? null : ColorProviderRegistry.ITEM.get(block.asItem());
      return color == null ? -1 : color.getColor(new ItemStack(block.asItem()), 0);
    }
    return -1;
  }
}

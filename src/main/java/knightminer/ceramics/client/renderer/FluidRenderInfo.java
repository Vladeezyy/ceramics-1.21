package knightminer.ceramics.client.renderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Sprites and color of a fluid for the block entity renderers. Since 26.1 they come from the fluid model of the fluid
 * state instead of the client fluid type extension.
 */
record FluidRenderInfo(TextureAtlasSprite still, TextureAtlasSprite flowing, int color) {
  static FluidRenderInfo of(FluidStack fluid) {
    FluidModel model = Minecraft.getInstance().getModelManager().getFluidStateModelSet().get(fluid.getFluid().defaultFluidState());
    int color = model.fluidTintSource() == null ? -1 : model.fluidTintSource().colorAsStack(fluid);
    return new FluidRenderInfo(model.stillMaterial().sprite(), model.flowingMaterial().sprite(), color);
  }
}

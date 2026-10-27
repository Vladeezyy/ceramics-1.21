package knightminer.ceramics.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.vladeezyy.kilnwork.client.render.FluidCuboid;
import dev.vladeezyy.kilnwork.client.render.FluidCuboidRenderer;
import knightminer.ceramics.blocks.CisternBlock;
import knightminer.ceramics.blocks.entity.CisternBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Plane;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import dev.vladeezyy.kilnwork.platform.KilnworkClientPlatform;
import dev.vladeezyy.kilnwork.platform.KilnworkPlatform;
import dev.vladeezyy.kilnwork.fluid.api.FluidStack;
import org.joml.Vector3f;

import javax.annotation.Nullable;

/**
 * Renderer for cistern blocks. Since 1.21.9 the block entity is read into a render state first, then drawn from it.
 */
public class CisternBlockEntityRenderer implements BlockEntityRenderer<CisternBlockEntity, CisternBlockEntityRenderer.State> {
  public CisternBlockEntityRenderer(BlockEntityRendererProvider.Context context) {}

  /** Data of one cistern for drawing */
  public static class State extends BlockEntityRenderState {
    FluidStack fluid = FluidStack.EMPTY;
    /** Fluid above the bottom of this cistern of the structure */
    int amount;
    int capacityPerLayer;
  }

  @Override
  public State createRenderState() {
    return new State();
  }

  @Override
  public void extractRenderState(CisternBlockEntity cistern, State state, float partialTick, Vec3 camera, @Nullable ModelFeatureRenderer.CrumblingOverlay crumbling) {
    BlockEntityRenderer.super.extractRenderState(cistern, state, partialTick, camera, crumbling);
    FluidStack fluid = cistern.getFluid();
    state.fluid = fluid.copy();
    // capacity for gives us the minimum amount to start rendering in this segment
    state.amount = fluid.getAmount() - cistern.capacityFor(cistern.getRenderIndex());
    state.capacityPerLayer = cistern.capacityPerLayer();
  }

  @Override
  public void submit(State renderState, PoseStack matrices, SubmitNodeCollector collector, CameraRenderState camera) {
    FluidStack fluid = renderState.fluid;
    int amount = renderState.amount;
    // render nothing beyond the base capacity
    if (fluid.isEmpty() || amount <= 0) {
      return;
    }
    // get the model pair, if the capacity is above the capacity per cistern, use the overfull model (no top face)
    BlockState state = renderState.blockState;
    CisternFluids model = CisternFluids.REGISTRY.get(state.getBlock());
    if (model == null) {
      return;
    }
    // fetch textures and attributes
    TextureAtlasSprite still = KilnworkClientPlatform.get().stillSprite(fluid);
    TextureAtlasSprite flowing = KilnworkClientPlatform.get().flowingSprite(fluid);
    int color = KilnworkClientPlatform.get().tint(fluid, null, null);
    int light = FluidCuboidRenderer.withBlockLight(renderState.lightCoords, KilnworkPlatform.get().fluidLightLevel(fluid));

    // if full, just render all full sides
    int capacityPerLayer = renderState.capacityPerLayer;
    if (amount > capacityPerLayer) {
      for (Direction direction : Plane.HORIZONTAL) {
        // state and model must contain that direction
        FluidCuboid cuboid = model.side(direction);
        if (cuboid != null && state.getValue(CisternBlock.CONNECTIONS.get(direction))) {
          FluidCuboidRenderer.submitCuboid(matrices, collector, cuboid, still, flowing, cuboid.getFromScaled(), cuboid.getToScaled(), color, light, false);
        }
      }
    } else {
      // determine the relevant height of the center
      FluidCuboid center = model.base(state.getValue(CisternBlock.EXTENSION));
      Vector3f from = center.getFromScaled();
      Vector3f to = new Vector3f(center.getToScaled());
      float minY = from.y();
      to.y = minY + amount * (to.y() - minY) / (float)capacityPerLayer;
      FluidCuboidRenderer.submitCuboid(matrices, collector, center, still, still, from, to, color, light, false);

      // scale the sides based on the center
      for (Direction direction : Plane.HORIZONTAL) {
        // state and model must contain that direction
        FluidCuboid cuboid = model.side(direction);
        if (cuboid != null && state.getValue(CisternBlock.CONNECTIONS.get(direction))) {
          // bottom of the side must be smaller than the height to consider
          Vector3f sFrom = cuboid.getFromScaled();
          if (sFrom.y() < to.y()) {
            // if the side end is larger than the center, clamp it down
            Vector3f sTo = cuboid.getToScaled();
            if (sTo.y() > to.y()) {
              sTo = new Vector3f(sTo);
              sTo.y = to.y();
            }
            FluidCuboidRenderer.submitCuboid(matrices, collector, cuboid, still, still, sFrom, sTo, color, light, false);
          }
        }
      }
    }
  }
}

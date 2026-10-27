package knightminer.ceramics.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.vladeezyy.kilnwork.client.render.FluidCuboid;
import dev.vladeezyy.kilnwork.client.render.FluidCuboidRenderer;
import dev.vladeezyy.kilnwork.client.render.KilnworkFluidModels;
import knightminer.ceramics.blocks.FaucetBlock;
import knightminer.ceramics.blocks.entity.FaucetBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import dev.vladeezyy.kilnwork.platform.KilnworkClientPlatform;
import dev.vladeezyy.kilnwork.platform.KilnworkPlatform;
import dev.vladeezyy.kilnwork.fluid.api.FluidStack;

import javax.annotation.Nullable;
import java.util.List;

public class FaucetBlockEntityRenderer implements BlockEntityRenderer<FaucetBlockEntity, FaucetBlockEntityRenderer.State> {
  public FaucetBlockEntityRenderer(BlockEntityRendererProvider.Context context) {}

  /** Data of one faucet for drawing */
  public static class State extends BlockEntityRenderState {
    /** Block state, the field of the base class is private since 26.1 */
    BlockState state = net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
    FluidStack fluid = FluidStack.EMPTY;
    /** Fluid shapes of the blocks below the faucet */
    List<List<FluidCuboid>> below = List.of();
  }

  @Override
  public State createRenderState() {
    return new State();
  }

  // NeoForge extension method (overrides it there by signature)
  public AABB getRenderBoundingBox(FaucetBlockEntity te) {
    // fluid can render into the block below
    BlockPos pos = te.getBlockPos();
    return new AABB(pos.getX(), pos.getY() - 1, pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
  }

  @Override
  public void extractRenderState(FaucetBlockEntity faucet, State state, float partialTick, Vec3 camera, @Nullable ModelFeatureRenderer.CrumblingOverlay crumbling) {
    BlockEntityRenderer.super.extractRenderState(faucet, state, partialTick, camera, crumbling);
    state.state = faucet.getBlockState();
    FluidStack fluid = faucet.getRenderFluid();
    Level level = faucet.getLevel();
    if (!faucet.isPouring() || fluid.isEmpty() || level == null) {
      state.fluid = FluidStack.EMPTY;
      state.below = List.of();
      return;
    }
    state.fluid = fluid.copy();
    state.below = FluidCuboidRenderer.collectFaucetFluids(level, faucet.getBlockPos(), faucet.getBlockState().getValue(FaucetBlock.FACING));
  }

  @Override
  public void submit(State renderState, PoseStack matrices, SubmitNodeCollector collector, CameraRenderState camera) {
    FluidStack renderFluid = renderState.fluid;
    if (renderFluid.isEmpty()) {
      return;
    }

    // fetch faucet model to determine where to render fluids
    BlockState state = renderState.state;
    List<FluidCuboid> fluids = KilnworkFluidModels.BLOCK_FLUIDS.get(state, List.of());
    if (!fluids.isEmpty()) {
      // if side, rotate fluid model
      Direction direction = state.getValue(FaucetBlock.FACING);
      boolean isRotated = FluidCuboidRenderer.applyRotation(matrices, direction);

      // fluid props
      int color = KilnworkClientPlatform.get().tint(renderFluid, null, null);
      TextureAtlasSprite still = KilnworkClientPlatform.get().stillSprite(renderFluid);
      TextureAtlasSprite flowing = KilnworkClientPlatform.get().flowingSprite(renderFluid);
      int light = FluidCuboidRenderer.withBlockLight(renderState.lightCoords, KilnworkPlatform.get().fluidLightLevel(renderFluid));

      // render all cubes in the model
      for (FluidCuboid cube : fluids) {
        FluidCuboidRenderer.submitCuboid(matrices, collector, cube, 0, still, flowing, color, light, false);
      }

      // render into the block(s) below
      FluidCuboidRenderer.submitFaucetFluids(renderState.below, matrices, collector, still, flowing, color, light);

      // if rotated, pop back rotation
      if (isRotated) {
        matrices.popPose();
      }
    }
  }
}

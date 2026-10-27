package knightminer.ceramics.client.renderer;

import dev.vladeezyy.kilnwork.platform.KilnworkPlatform;
import dev.vladeezyy.kilnwork.client.render.KilnworkFluidModels;
import dev.vladeezyy.kilnwork.client.render.FluidCuboidRenderer;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.BlockPos;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import knightminer.ceramics.blocks.FaucetBlock;
import knightminer.ceramics.blocks.entity.FaucetBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import dev.vladeezyy.kilnwork.platform.KilnworkClientPlatform;
import dev.vladeezyy.kilnwork.fluid.api.FluidStack;
import dev.vladeezyy.kilnwork.client.render.FluidCuboid;

import java.util.List;
import java.util.function.Function;

public class FaucetBlockEntityRenderer implements BlockEntityRenderer<FaucetBlockEntity> {
  public FaucetBlockEntityRenderer(BlockEntityRendererProvider.Context context) {}

  // NeoForge extension method (overrides it there by signature)
  public AABB getRenderBoundingBox(FaucetBlockEntity te) {
    // fluid can render into the block below
    BlockPos pos = te.getBlockPos();
    return new AABB(pos.getX(), pos.getY() - 1, pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
  }


  @Override
  public void render(FaucetBlockEntity tileEntity, float partialTicks, PoseStack matrices, MultiBufferSource bufferIn, int combinedLightIn, int combinedOverlayIn) {
    FluidStack renderFluid = tileEntity.getRenderFluid();
    if (!tileEntity.isPouring() || renderFluid.isEmpty()) {
      return;
    }

    // safety
    Level world = tileEntity.getLevel();
    if (world == null) {
      return;
    }

    // fetch faucet model to determine where to render fluids
    BlockState state = tileEntity.getBlockState();
    List<FluidCuboid> fluids = KilnworkFluidModels.BLOCK_FLUIDS.get(state, List.of());
    if (!fluids.isEmpty()) {
      // if side, rotate fluid model
      Direction direction = state.getValue(FaucetBlock.FACING);
      boolean isRotated = FluidCuboidRenderer.applyRotation(matrices, direction);

      // fluid props
      int color = KilnworkClientPlatform.get().tint(renderFluid, null, null);
      Function<ResourceLocation, TextureAtlasSprite> spriteGetter = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS);
      TextureAtlasSprite still = KilnworkClientPlatform.get().stillSprite(renderFluid);
      TextureAtlasSprite flowing = KilnworkClientPlatform.get().flowingSprite(renderFluid);
      combinedLightIn = FluidCuboidRenderer.withBlockLight(combinedLightIn, KilnworkPlatform.get().fluidLightLevel(renderFluid));

      // render all cubes in the model
      VertexConsumer buffer = FluidCuboidRenderer.getBuffer(bufferIn);
      for (FluidCuboid cube : fluids) {
        FluidCuboidRenderer.renderCuboid(matrices, buffer, cube, 0, still, flowing, color, combinedLightIn, false);
      }

      // render into the block(s) below
      FluidCuboidRenderer.renderFaucetFluids(world, tileEntity.getBlockPos(), direction, matrices, buffer, still, flowing, color, combinedLightIn);

      // if rotated, pop back rotation
      if(isRotated) {
        matrices.popPose();
      }
    }
  }
}

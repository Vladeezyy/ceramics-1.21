package knightminer.ceramics.client.renderer;

import dev.vladeezyy.kilnwork.client.render.KilnworkFluidModels;
import dev.vladeezyy.kilnwork.client.render.FluidCuboidRenderer;
import net.minecraft.world.phys.AABB;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import knightminer.ceramics.blocks.ChannelBlock;
import knightminer.ceramics.blocks.ChannelBlock.ChannelConnection;
import knightminer.ceramics.blocks.entity.ChannelBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Plane;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import dev.vladeezyy.kilnwork.client.render.ChannelFluids;
import dev.vladeezyy.kilnwork.client.render.FluidCuboid;

public class ChannelBlockEntityRenderer implements BlockEntityRenderer<ChannelBlockEntity> {
	public ChannelBlockEntityRenderer(BlockEntityRendererProvider.Context context) {}

	@Override
	public AABB getRenderBoundingBox(ChannelBlockEntity te) {
		// fluid can render into the block below
		BlockPos pos = te.getBlockPos();
		return new AABB(pos.getX(), pos.getY() - 1, pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
	}


	@Override
	public void render(ChannelBlockEntity te, float partialTicks, PoseStack matrices, MultiBufferSource buffer, int light, int combinedOverlayIn)  {
		FluidStack fluid = te.getFluid();
		if (fluid.isEmpty()) {
			return;
		}

		// fetch model properties
		Level world = te.getLevel();
		if (world == null) {
			return;
		}
		BlockPos pos = te.getBlockPos();
		BlockState state = te.getBlockState();
		ChannelFluids model = KilnworkFluidModels.CHANNEL_FLUIDS.get(state.getBlock());
		if (model == null) {
			return;
		}

		// fluid attributes
		IClientFluidTypeExtensions client = IClientFluidTypeExtensions.of(fluid.getFluid());
		TextureAtlasSprite still = FluidCuboidRenderer.getBlockSprite(client.getStillTexture(fluid));
		TextureAtlasSprite flowing = FluidCuboidRenderer.getBlockSprite(client.getFlowingTexture(fluid));
		VertexConsumer builder = FluidCuboidRenderer.getBuffer(buffer);
		int color = client.getTintColor(fluid);
		light = FluidCuboidRenderer.withBlockLight(light, fluid.getFluid().getFluidType().getLightLevel(fluid));

		// render sides first, while doing so we will determine center "flow"
		FluidCuboid cube;
		boolean isRotated;
		Direction centerFlow = Direction.UP;
		for (Direction direction : Plane.HORIZONTAL) {
			// check if we have that side on the block
			ChannelConnection connection = state.getValue(ChannelBlock.DIRECTION_MAP.get(direction));
			if (connection.canFlow()) {
				// apply rotation for the side
				isRotated = FluidCuboidRenderer.applyRotation(matrices, direction);
				// get the relevant fluid model, render it
				if (te.isFlowing(direction)) {
					cube = model.side().flow(connection == ChannelConnection.OUT);

					// add to center direction
					if (connection == ChannelConnection.OUT) {
						// if unset (up), use this direction
						if (centerFlow == Direction.UP) {
							centerFlow = direction;
							// if set and it disagrees, set the fail state (down)
						} else if (centerFlow != direction) {
							centerFlow = Direction.DOWN;
						}
					}
					// render the extra edge against other blocks
					if (!world.getBlockState(pos.relative(direction)).is(state.getBlock())) {
						FluidCuboidRenderer.renderCuboid(matrices, builder, model.side().edge(), 0, still, flowing, color, light, false);
					}
				} else {
					cube = model.side().still();
				}
				FluidCuboidRenderer.renderCuboid(matrices, builder, cube, 0, still, flowing, color, light, false);
				// undo rotation
				if (isRotated) {
					matrices.popPose();
				}
			}
		}

		// render center
		isRotated = false;
		if (centerFlow.getAxis().isVertical()) {
			cube = model.center(false);
		} else {
			cube = model.center(true);
			isRotated = FluidCuboidRenderer.applyRotation(matrices, centerFlow);
		}
		// render the cube and pop back
		FluidCuboidRenderer.renderCuboid(matrices, builder, cube, 0, still, flowing, color, light, false);
		if (isRotated) {
			matrices.popPose();
		}

		// render flow downwards
		if (state.getValue(ChannelBlock.DOWN) && te.isFlowing(Direction.DOWN)) {
			cube = model.down();
			FluidCuboidRenderer.renderCuboid(matrices, builder, cube, 0, still, flowing, color, light, false);

			// render into the block(s) below
			FluidCuboidRenderer.renderFaucetFluids(world, pos, Direction.DOWN, matrices, builder, still, flowing, color, light);
		}
	}
}
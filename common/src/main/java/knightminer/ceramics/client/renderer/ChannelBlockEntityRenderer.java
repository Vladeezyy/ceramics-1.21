package knightminer.ceramics.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.vladeezyy.kilnwork.client.render.ChannelFluids;
import dev.vladeezyy.kilnwork.client.render.FluidCuboid;
import dev.vladeezyy.kilnwork.client.render.FluidCuboidRenderer;
import dev.vladeezyy.kilnwork.client.render.KilnworkFluidModels;
import knightminer.ceramics.blocks.ChannelBlock;
import knightminer.ceramics.blocks.ChannelBlock.ChannelConnection;
import knightminer.ceramics.blocks.entity.ChannelBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Plane;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import dev.vladeezyy.kilnwork.platform.KilnworkClientPlatform;
import dev.vladeezyy.kilnwork.platform.KilnworkPlatform;
import dev.vladeezyy.kilnwork.fluid.api.FluidStack;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class ChannelBlockEntityRenderer implements BlockEntityRenderer<ChannelBlockEntity, ChannelBlockEntityRenderer.State> {
	public ChannelBlockEntityRenderer(BlockEntityRendererProvider.Context context) {}

	/** Data of one channel for drawing */
	public static class State extends BlockEntityRenderState {
		FluidStack fluid = FluidStack.EMPTY;
		/** Sides currently flowing, down included */
		final Set<Direction> flowing = EnumSet.noneOf(Direction.class);
		/** Sides facing a block that is not this channel, those get an extra edge */
		final Set<Direction> edges = EnumSet.noneOf(Direction.class);
		/** Fluid shapes of the blocks below when pouring down */
		List<List<FluidCuboid>> below = List.of();
	}

	@Override
	public State createRenderState() {
		return new State();
	}

  // NeoForge extension method (overrides it there by signature)
	public AABB getRenderBoundingBox(ChannelBlockEntity te) {
		// fluid can render into the block below
		BlockPos pos = te.getBlockPos();
		return new AABB(pos.getX(), pos.getY() - 1, pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
	}

	@Override
	public void extractRenderState(ChannelBlockEntity te, State state, float partialTick, Vec3 camera, @Nullable ModelFeatureRenderer.CrumblingOverlay crumbling) {
		BlockEntityRenderer.super.extractRenderState(te, state, partialTick, camera, crumbling);
		state.flowing.clear();
		state.edges.clear();
		state.below = List.of();
		Level level = te.getLevel();
		FluidStack fluid = te.getFluid();
		if (fluid.isEmpty() || level == null) {
			state.fluid = FluidStack.EMPTY;
			return;
		}
		state.fluid = fluid.copy();
		BlockPos pos = te.getBlockPos();
		BlockState blockState = te.getBlockState();
		for (Direction direction : Plane.HORIZONTAL) {
			if (te.isFlowing(direction)) {
				state.flowing.add(direction);
			}
			if (!level.getBlockState(pos.relative(direction)).is(blockState.getBlock())) {
				state.edges.add(direction);
			}
		}
		if (blockState.getValue(ChannelBlock.DOWN) && te.isFlowing(Direction.DOWN)) {
			state.flowing.add(Direction.DOWN);
			state.below = FluidCuboidRenderer.collectFaucetFluids(level, pos, Direction.DOWN);
		}
	}

	@Override
	public void submit(State renderState, PoseStack matrices, SubmitNodeCollector collector, CameraRenderState camera) {
		FluidStack fluid = renderState.fluid;
		if (fluid.isEmpty()) {
			return;
		}
		BlockState state = renderState.blockState;
		ChannelFluids model = KilnworkFluidModels.CHANNEL_FLUIDS.get(state.getBlock());
		if (model == null) {
			return;
		}

		// fluid attributes
		TextureAtlasSprite still = KilnworkClientPlatform.get().stillSprite(fluid);
		TextureAtlasSprite flowing = KilnworkClientPlatform.get().flowingSprite(fluid);
		int color = KilnworkClientPlatform.get().tint(fluid, null, null);
		int light = FluidCuboidRenderer.withBlockLight(renderState.lightCoords, KilnworkPlatform.get().fluidLightLevel(fluid));

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
				if (renderState.flowing.contains(direction)) {
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
					if (renderState.edges.contains(direction)) {
						FluidCuboidRenderer.submitCuboid(matrices, collector, model.side().edge(), 0, still, flowing, color, light, false);
					}
				} else {
					cube = model.side().still();
				}
				FluidCuboidRenderer.submitCuboid(matrices, collector, cube, 0, still, flowing, color, light, false);
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
		FluidCuboidRenderer.submitCuboid(matrices, collector, cube, 0, still, flowing, color, light, false);
		if (isRotated) {
			matrices.popPose();
		}

		// render flow downwards
		if (renderState.flowing.contains(Direction.DOWN)) {
			FluidCuboidRenderer.submitCuboid(matrices, collector, model.down(), 0, still, flowing, color, light, false);
			// render into the block(s) below
			FluidCuboidRenderer.submitFaucetFluids(renderState.below, matrices, collector, still, flowing, color, light);
		}
	}
}

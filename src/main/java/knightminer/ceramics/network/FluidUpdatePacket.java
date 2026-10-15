package knightminer.ceramics.network;

import knightminer.ceramics.Ceramics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import slimeknights.mantle.client.SafeClientAccess;
import slimeknights.mantle.util.BlockEntityHelper;

/**
 * Base class for fluid updating behavior
 */
public record FluidUpdatePacket(BlockPos pos, FluidStack fluid, boolean data) implements CustomPacketPayload {
	public static final Type<FluidUpdatePacket> TYPE = new Type<>(Ceramics.getResource("fluid_update"));

	public FluidUpdatePacket {
		// in singleplayer the packet reaches the client without being encoded, so never share the server's stack
		fluid = fluid.copy();
	}
	public static final StreamCodec<RegistryFriendlyByteBuf, FluidUpdatePacket> STREAM_CODEC = StreamCodec.composite(
		BlockPos.STREAM_CODEC, FluidUpdatePacket::pos,
		FluidStack.OPTIONAL_STREAM_CODEC, FluidUpdatePacket::fluid,
		ByteBufCodecs.BOOL, FluidUpdatePacket::data,
		FluidUpdatePacket::new);

	@Override
	public Type<FluidUpdatePacket> type() {
		return TYPE;
	}

	/** Handles the packet on the client main thread */
	public void handle(IPayloadContext context) {
		HandleClient.handle(this);
	}

	/** Helper for a block entity to update a fluid */
	public interface FluidUpdater {
		/** Updates the fluid from the server */
		void updateFluid(FluidStack fluid, boolean data);
	}

	private static class HandleClient {
		private static void handle(FluidUpdatePacket packet) {
			Level level = SafeClientAccess.getLevel();
			if (BlockEntityHelper.isBlockLoaded(level, packet.pos) && level.getBlockEntity(packet.pos) instanceof FluidUpdater updater) {
				updater.updateFluid(packet.fluid, packet.data);
			}
		}
	}
}

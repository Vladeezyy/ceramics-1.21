package knightminer.ceramics.network;

import knightminer.ceramics.Ceramics;
import knightminer.ceramics.blocks.entity.ChannelBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import dev.vladeezyy.kilnwork.client.KilnworkClient;
import dev.vladeezyy.kilnwork.util.BlockEntityHelper;

/** Packet for when the flowing state changes on a channel side */
public record ChannelFlowPacket(BlockPos pos, Direction side, boolean flow) implements CustomPacketPayload {
	public static final Type<ChannelFlowPacket> TYPE = new Type<>(Ceramics.getResource("channel_flow"));
	public static final StreamCodec<FriendlyByteBuf, ChannelFlowPacket> STREAM_CODEC = StreamCodec.composite(
		BlockPos.STREAM_CODEC, ChannelFlowPacket::pos,
		Direction.STREAM_CODEC, ChannelFlowPacket::side,
		ByteBufCodecs.BOOL, ChannelFlowPacket::flow,
		ChannelFlowPacket::new);

	@Override
	public Type<ChannelFlowPacket> type() {
		return TYPE;
	}

	/** Handles the packet on the client main thread */
	public void handle(IPayloadContext context) {
		HandleClient.handle(this);
	}

	private static class HandleClient {
		private static void handle(ChannelFlowPacket packet) {
			Level level = KilnworkClient.getLevel();
			if (BlockEntityHelper.isBlockLoaded(level, packet.pos) && level.getBlockEntity(packet.pos) instanceof ChannelBlockEntity channel) {
				channel.setFlow(packet.side, packet.flow);
			}
		}
	}
}

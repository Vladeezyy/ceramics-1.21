package knightminer.ceramics.network;

import knightminer.ceramics.Ceramics;
import knightminer.ceramics.blocks.entity.CrackableBlockEntityHandler.ICrackableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import dev.vladeezyy.kilnwork.client.KilnworkClient;
import dev.vladeezyy.kilnwork.util.BlockEntityHelper;

public record CrackableCrackPacket(BlockPos pos, int cracks) implements CustomPacketPayload {
	public static final Type<CrackableCrackPacket> TYPE = new Type<>(Ceramics.getResource("crackable_crack"));
	public static final StreamCodec<FriendlyByteBuf, CrackableCrackPacket> STREAM_CODEC = StreamCodec.composite(
		BlockPos.STREAM_CODEC, CrackableCrackPacket::pos,
		ByteBufCodecs.VAR_INT, CrackableCrackPacket::cracks,
		CrackableCrackPacket::new);

	@Override
	public Type<CrackableCrackPacket> type() {
		return TYPE;
	}

	/** Handles the packet on the client main thread */
	public void handle(IPayloadContext context) {
		HandleClient.handle(this);
	}

	private static class HandleClient {
		private static void handle(CrackableCrackPacket packet) {
			Level level = KilnworkClient.getLevel();
			if (BlockEntityHelper.isBlockLoaded(level, packet.pos) && level.getBlockEntity(packet.pos) instanceof ICrackableBlockEntity crackable) {
				crackable.getCracksHandler().setCracks(packet.cracks);
			}
		}
	}
}

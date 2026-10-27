package knightminer.ceramics.network;

import dev.vladeezyy.kilnwork.platform.KilnworkPlatform;
import dev.vladeezyy.kilnwork.platform.LoaderHooks;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/**
 * Handler for all packets internal to Ceramics
 */
public class CeramicsNetwork {
  private CeramicsNetwork() {}

  /**
   * Registers all packets (the loader part applies them)
   */
  public static void register() {
    LoaderHooks.clientboundPacket(FluidUpdatePacket.TYPE, FluidUpdatePacket.STREAM_CODEC, (packet, player) -> packet.handle());
    LoaderHooks.clientboundPacket(ChannelFlowPacket.TYPE, ChannelFlowPacket.STREAM_CODEC, (packet, player) -> packet.handle());
    LoaderHooks.clientboundPacket(CrackableCrackPacket.TYPE, CrackableCrackPacket.STREAM_CODEC, (packet, player) -> packet.handle());
  }

  /**
   * Sends a packet to all clients watching the chunk of the position, automatically checking the server world cast
   * @param msg    Packet
   * @param world  World instance
   * @param pos    Position
   */
  public static void sendToClientsAround(CustomPacketPayload msg, @Nullable Level world, BlockPos pos) {
    if (world instanceof ServerLevel server) {
      KilnworkPlatform.get().sendToTracking(server, pos, msg);
    }
  }
}

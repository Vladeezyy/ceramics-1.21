package knightminer.ceramics.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import javax.annotation.Nullable;

/**
 * Handler for all packets internal to Ceramics
 */
public class CeramicsNetwork {
  /** Network version, raise when packets change */
  private static final String VERSION = "2";

  private CeramicsNetwork() {}

  /**
   * Registers all packets, called on the mod bus
   */
  public static void registerPackets(RegisterPayloadHandlersEvent event) {
    PayloadRegistrar registrar = event.registrar(VERSION);
    registrar.playToClient(FluidUpdatePacket.TYPE, FluidUpdatePacket.STREAM_CODEC, FluidUpdatePacket::handle);
    registrar.playToClient(ChannelFlowPacket.TYPE, ChannelFlowPacket.STREAM_CODEC, ChannelFlowPacket::handle);
    registrar.playToClient(CrackableCrackPacket.TYPE, CrackableCrackPacket.STREAM_CODEC, CrackableCrackPacket::handle);
  }

  /**
   * Sends a packet to all clients watching the chunk of the position, automatically checking the server world cast
   * @param msg    Packet
   * @param world  World instance
   * @param pos    Position
   */
  public static void sendToClientsAround(CustomPacketPayload msg, @Nullable Level world, BlockPos pos) {
    if (world instanceof ServerLevel server) {
      PacketDistributor.sendToPlayersTrackingChunk(server, new ChunkPos(pos), msg);
    }
  }
}

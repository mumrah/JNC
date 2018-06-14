package net.tarpn.packet.impl;

import java.util.function.Consumer;
import net.tarpn.DataPort;
import net.tarpn.packet.Packet;
import net.tarpn.packet.PacketAction;
import net.tarpn.packet.PacketRequest;
import net.tarpn.packet.PacketRequestHandler;
import net.tarpn.packet.PacketRouter;

public class ForwardingPacketAction implements PacketRequestHandler {

  private final PacketRouter router;

  public ForwardingPacketAction(PacketRouter router) {
    this.router = router;
  }


  @Override
  public void onRequest(PacketRequest packetRequest) {
    DataPort port = router.routePacket(packetRequest.getPacket());

  }
}

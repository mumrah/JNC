package net.tarpn.network;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import net.tarpn.network.NetRom.OpType;
import net.tarpn.packet.PacketHandler;
import net.tarpn.packet.PacketRequest;
import net.tarpn.packet.impl.ax25.AX25Packet;
import net.tarpn.packet.impl.ax25.AX25Packet.Protocol;
import net.tarpn.packet.impl.ax25.AX25Packet.UnnumberedFrame.ControlType;
import net.tarpn.packet.impl.ax25.IFrame;
import net.tarpn.packet.impl.ax25.UFrame;

public class NetRomPacketHandler implements PacketHandler {

  @Override
  public void onPacket(PacketRequest packetRequest) {
    AX25Packet packet = (AX25Packet)packetRequest.getPacket();
    if(packet instanceof UFrame) {
      UFrame sFrame = (UFrame)packet;
      if (sFrame.getControlType().equals(ControlType.SABM)) {
        UFrame ua = UFrame.create(packet.getDestination(), packet.getSource(), ControlType.UA, true);
        packetRequest.replyWith(ua);
      }
    }
    if(packet instanceof IFrame) {
      IFrame iFrame = (IFrame)packet;
      if(iFrame.getProtocol().equals(Protocol.NETROM)) {
        NetRom netRom = NetRom.read(ByteBuffer.wrap(iFrame.getInfo()));
        System.err.println(netRom);
        if(netRom.getOpType().equals(OpType.ConnectRequest)) {
          NetRom respFrame = new NetRomConnectAck(
              netRom.getDestCall(),
              netRom.getOriginCall(),
              (byte)(netRom.getTtl() - 1),
              netRom.getCircuitIndex(),
              netRom.getCircuitID(),
              (byte)0x64,
              (byte)0x02,
              OpType.ConnectAcknowledge.asByte(),
              ((NetRomConnect)netRom).getProposedWindowSize()
          );
          // Connect
          ByteBuffer buffer = ByteBuffer.allocate(1024);
          respFrame.write(buffer);
          int len = buffer.position();
          buffer.position(0);
          byte[] out = new byte[len];
          buffer.get(out, 0, len);
          IFrame resp = IFrame.create(iFrame.getDestination(), iFrame.getSource(),
              (byte)0, (byte)0, true, Protocol.NETROM, out);
          packetRequest.replyWith(resp);
        }
      } else {
        String message = new String(iFrame.getInfo(), StandardCharsets.US_ASCII).trim();
        if(message.equalsIgnoreCase("info")) {
          IFrame resp = IFrame.create(iFrame.getDestination(), iFrame.getSource(),
              (byte)0, (byte)0,
              true, Protocol.NO_LAYER3,
              "Java Node Controller\r".getBytes(StandardCharsets.US_ASCII));
          packetRequest.replyWith(resp);
        } else if(message.equalsIgnoreCase("bye")) {
          UFrame ua = UFrame.create(packet.getDestination(), packet.getSource(), ControlType.DISC, true);
          packetRequest.replyWith(ua);
        }
      }
    }
  }
}

package net.tarpn.packet.impl;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.tarpn.util.Util;
import net.tarpn.frame.Frame;
import net.tarpn.packet.Packet;
import net.tarpn.packet.PacketReader;
import net.tarpn.packet.impl.ax25.AX25Call;
import net.tarpn.packet.impl.ax25.AX25Packet;
import net.tarpn.packet.impl.ax25.AX25Packet.UnnumberedFrame.ControlType;
import net.tarpn.packet.impl.ax25.IFrame;
import net.tarpn.packet.impl.ax25.SFrame;
import net.tarpn.packet.impl.ax25.UFrame;
import net.tarpn.packet.impl.ax25.UIFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AX25PacketReader implements PacketReader {

  private static final Logger LOG = LoggerFactory.getLogger(AX25PacketReader.class);

  @Override
  public void accept(Frame frame, Consumer<Packet> packetHandler) {
    try {
      AX25Packet packet = parse(frame.getData());
      packetHandler.accept(packet);
    } catch (Throwable t) {
      LOG.error("Failed to parse AX.25 packet " + Util.toHexDump(frame.getData()), t);
    }
  }

  public static AX25Packet parse(byte[] packet) {
    ByteBuffer buffer = ByteBuffer.wrap(packet);

    AX25Call dest = AX25Call.read(buffer);
    AX25Call source = AX25Call.read(buffer);
    List<AX25Call> paths = new ArrayList<>();
    if(!source.isLast()) {
      while(true) {
        AX25Call rpt = AX25Call.read(buffer);
        paths.add(rpt);
        if(rpt.isLast()) {
          break;
        }
      }
    }

    byte controlByte = buffer.get();
    boolean pollFinalSet = (controlByte & 0x10) == 0x10;

    final AX25Packet frame;
    if((controlByte & 0x01) == 0) {
      // I frame
      byte pidByte = buffer.get();
      int infoLen = packet.length - buffer.position();
      byte[] info = new byte[infoLen];
      buffer.get(info, 0, infoLen);
      frame = new IFrame(packet, dest, source, paths, controlByte, info, pidByte);
    } else {
      if((controlByte & 0x03) == 0x03) {
        ControlType controlType = ControlType.fromControlByte(controlByte);
        // U frame
        if(controlType.equals(ControlType.UI)) {
          // UI Unnumbered Information
          byte pidByte = buffer.get();
          int infoLen = packet.length - buffer.position();
          byte[] info = new byte[infoLen];
          buffer.get(info, 0, infoLen);
          frame = new UIFrame(packet, dest, source, paths, controlByte, pollFinalSet, info, pidByte);
        } else {
          frame = new UFrame(packet, dest, source, paths, controlByte, pollFinalSet);
        }
      } else {
        frame = new SFrame(packet, dest, source, paths, controlByte, pollFinalSet);
      }
    }
    return frame;
  }
}

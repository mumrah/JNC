package net.tarpn.packet.impl;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import net.tarpn.packet.Packet;
import net.tarpn.packet.PacketHandler;
import net.tarpn.packet.PacketRequest;
import net.tarpn.packet.impl.ax25.AX25Call;
import net.tarpn.packet.impl.ax25.AX25Packet;
import net.tarpn.packet.impl.ax25.AX25Packet.Command;
import net.tarpn.packet.impl.ax25.AX25Packet.Protocol;
import net.tarpn.packet.impl.ax25.AX25Packet.SupervisoryFrame;
import net.tarpn.packet.impl.ax25.AX25Packet.UnnumberedFrame.ControlType;
import net.tarpn.packet.impl.ax25.DXE;
import net.tarpn.packet.impl.ax25.IFrame;
import net.tarpn.packet.impl.ax25.SFrame;
import net.tarpn.packet.impl.ax25.UFrame;
import net.tarpn.packet.impl.ax25.UIFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AX25PacketHandler implements PacketHandler {

  private static Logger LOG = LoggerFactory.getLogger(AX25PacketHandler.class);

  Map<AX25Call, DXE> sessions = new ConcurrentHashMap<>();

  @Override
  public void onPacket(PacketRequest packet) {
    AX25Packet ax25Packet = (AX25Packet)packet.getPacket();
    switch (ax25Packet.getFrameType()) {
      case I:
        handleIFrame((IFrame)ax25Packet, packet::replyWith);
        break;
      case S:
        handleSFrame((SFrame)ax25Packet, packet::replyWith);
        break;
      case U:
        handleUFrame((UFrame)ax25Packet, packet::replyWith);
        break;
      case UI:
        handleUIFrame((UIFrame)ax25Packet, packet::replyWith);
        break;
    }
  }

  /**
   * Information frames
   */
  void handleIFrame(IFrame frame, Consumer<Packet> response) {
    LOG.info("<I>: " + frame.getInfoAsASCII());
    SFrame ack = SFrame.create(
        frame.getSourceCall(),
        frame.getDestCall(),
        Command.RESPONSE,
        SupervisoryFrame.ControlType.RR,
        frame.getSendSequenceNumber() + 1,
        true);
    response.accept(ack);
  }

  /**
   * Supervisory frames, RR, RNR, REJ
   */
  void handleSFrame(SFrame frame, Consumer<Packet> response) {
    LOG.info("<S>: " + frame.getControlType());
    if(frame.getControlType().equals(SupervisoryFrame.ControlType.RR)) {
      if(frame.getCommand().equals(Command.COMMAND) && frame.isPollOrFinalSet()) {
        // Sender is asking how we're doing
        // TODO send back an RR with Command.RESPONSE and Final=true
      }
    }
  }

  /**
   * Un-numbered frames, SABM, UA, DISC, DM. UI handled separately
   */
  void handleUFrame(UFrame frame, Consumer<Packet> response) {
    if (frame.getControlType().equals(ControlType.SABM)) {
      AX25Call dest = frame.getSourceCall();
      AX25Call source = frame.getDestCall();
      DXE session = sessions.computeIfAbsent(frame.getSourceCall(),
          ax25Call -> new DXE(frame.getSourceCall(), frame.getDestCall()));
      if(!session.isConnected()) {
        LOG.info("Got SABM, sending UA");
        // Acknowledge the connect request
        UFrame ua = UFrame.create(dest, source, ControlType.UA, true);
        response.accept(ua);

        LOG.info("Sending welcome message");
        // Welcome message
        IFrame info = IFrame.create(dest, source,
            0, 0, true, Protocol.NO_LAYER3,
            "Welcome to the David's Java Node Controller\r".getBytes(StandardCharsets.US_ASCII));
        response.accept(info);
      } else {
        // Reject the connection
        UFrame ua = UFrame.create(dest, source, ControlType.DM, true);
        response.accept(ua);
      }
    }
  }

  void handleUIFrame(UIFrame frame, Consumer<Packet> response) {
    LOG.info("<UI>" + frame.getInfoAsASCII());
  }
}

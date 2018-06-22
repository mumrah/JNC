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
import net.tarpn.packet.impl.ax25.DXE.State;
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
    DXE session = sessions.computeIfAbsent(frame.getSourceCall(),
        ax25Call -> new DXE(frame.getSourceCall(), frame.getDestCall()));
    if(session.getState().equals(State.CONNECTED)) {
      String message = new String(frame.getInfo(), StandardCharsets.US_ASCII).trim();
      if(frame.getSendSequenceNumber() == session.getReceiveState()) {
        // Got an I Frame with expected N(R)
        session.incrementReceiveState();
        if(session.getReceiveState() % 2 == 0) {
          IFrame info = IFrame.create(
              frame.getSourceCall(),
              frame.getDestCall(),
              Command.RESPONSE,
              session.getNextSendState(),
              session.getReceiveState(),
              true,
              Protocol.NO_LAYER3,
              ("You said '" + message + "'\r").getBytes(StandardCharsets.US_ASCII));
          //response.accept(info);
        } else {
          SFrame ack = SFrame.create(
              frame.getSourceCall(),
              frame.getDestCall(),
              Command.RESPONSE,
              SupervisoryFrame.ControlType.RR,
              session.getReceiveState(),
              true);
          response.accept(ack);
        }
      }
    }
  }

  /**
   * Supervisory frames, RR, RNR, REJ
   */
  void handleSFrame(SFrame frame, Consumer<Packet> response) {
    LOG.info("<S>: " + frame.getControlType());
    DXE session = sessions.computeIfAbsent(frame.getSourceCall(),
        ax25Call -> new DXE(frame.getSourceCall(), frame.getDestCall()));
    if(session.getState().equals(State.CONNECTED)) {
      if(frame.getControlType().equals(SupervisoryFrame.ControlType.RR)) {
        if(frame.getCommand().equals(Command.COMMAND) && frame.isPollOrFinalSet()) {
          // Sender is asking how we're doing
          SFrame resp = SFrame.create(
              frame.getSourceCall(),
              frame.getDestCall(),
              Command.RESPONSE,
              SupervisoryFrame.ControlType.RR,
              session.getReceiveState(),
              true);
          response.accept(resp);
        }
      }
    } else {
      // Indicate we are not connected
      UFrame dm = UFrame.create(
          frame.getSourceCall(),
          frame.getDestCall(),
          Command.RESPONSE,
          ControlType.DM,
          true);
      response.accept(dm);
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
      if(session.getState().equals(State.CLOSED)) {
        // Acknowledge the connect request
        LOG.info("Got SABM, sending UA");
        session.reset();
        session.setState(State.CONNECTED);
        UFrame ua = UFrame.create(dest, source, Command.RESPONSE, ControlType.UA, true);
        response.accept(ua);

        // Send a welcome message
        LOG.info("Sending welcome message");
        IFrame info = IFrame.create(dest, source, Command.COMMAND,
            session.getNextSendState(),
            0,
            true,
            Protocol.NO_LAYER3,
            "Welcome to the David's Java Node Controller\r".getBytes(StandardCharsets.US_ASCII));
        response.accept(info);
      } else if(session.getState().equals(State.CONNECTING)) {
        // Already sent one UA but was not heard, try again
        LOG.info("Got another SABM, sending UA again");
        UFrame ua = UFrame.create(dest, source, Command.RESPONSE, ControlType.UA, true);
        response.accept(ua);
        session.setState(State.CONNECTING);
      } else {
        // Reject the connection
        UFrame ua = UFrame.create(dest, source, Command.RESPONSE, ControlType.DM, true);
        response.accept(ua);
      }
    } else if(frame.getControlType().equals(ControlType.DISC)) {
      // Got a disconnect request, respond with UA and/or DM
      AX25Call source = frame.getSourceCall();
      AX25Call dest = frame.getDestCall();
      DXE session = sessions.computeIfAbsent(source, ax25Call -> new DXE(source, dest));
      if(session.getState().equals(State.CONNECTED)) {
        // Acknowledge the disconnect request
        LOG.info("Got DISC, sending UA and DM");
        UFrame ua = UFrame.create(source, dest, Command.RESPONSE, ControlType.UA, true);
        response.accept(ua);
        UFrame dm = UFrame.create(source, dest, Command.RESPONSE, ControlType.DM, true);
        response.accept(dm);
      } else {
        LOG.info("Got DISC but not connected, just sending DM");
        // Reject the disconnect
        UFrame dm = UFrame.create(source, dest, Command.RESPONSE, ControlType.DM, true);
        response.accept(dm);
      }
    }
  }

  void handleUIFrame(UIFrame frame, Consumer<Packet> response) {
    LOG.info("<UI>" + frame.getInfoAsASCII());
    DXE session = sessions.computeIfAbsent(frame.getSourceCall(),
        ax25Call -> new DXE(frame.getSourceCall(), frame.getDestCall()));
  }
}

package net.tarpn.frame;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import net.tarpn.config.Configuration;
import net.tarpn.frame.impl.KISS.Command;
import net.tarpn.frame.impl.KISSFrame;
import net.tarpn.frame.impl.KISSFrameReader;
import net.tarpn.packet.impl.AX25PacketReader;
import net.tarpn.packet.impl.ax25.AX25Call;
import net.tarpn.packet.impl.ax25.AX25Packet;
import net.tarpn.packet.impl.ax25.AX25Packet.HasInfo;
import net.tarpn.packet.impl.ax25.AX25Packet.Protocol;
import net.tarpn.network.netrom.NetRomCircuitManager;

public class TarpnSniffer {
  public static void main(String[] args) throws Exception {
    InputStream socatOutput = ClassLoader.getSystemResourceAsStream("tarpn-socat.txt");
    BufferedReader lineReader = new BufferedReader(new InputStreamReader(socatOutput));
    String line;
    StringBuilder buffer = new StringBuilder();
    while((line = lineReader.readLine()) != null) {
      if (line.startsWith(">") || line.startsWith("<")) {
        continue;
      } else {
        buffer.append(line.trim());
        buffer.append(" ");
      }
    }

    String[] hexBytes = buffer.toString().split("\\s+");

    KISSFrameReader reader = new KISSFrameReader(-1);
    List<Frame> frames = new ArrayList<>();
    for (int i = 0; i < hexBytes.length; i++) {
      int val = Integer.parseInt(hexBytes[i], 16);
      reader.accept(val, frames::add);
    }

    /*
    NetRomCircuitManager netRomHandler = new NetRomCircuitManager(config);
    for(Frame frame : frames) {
      if(((KISSFrame)frame).getKissCommand().equals(Command.Data)) {
        AX25Packet packet = AX25PacketReader.parse(frame.getData());
        if(packet instanceof HasInfo) {
          if (((HasInfo)packet).getProtocol().equals(Protocol.NETROM)) {
            netRomHandler.onPacket(packet, out -> {});
          }
        } else {
          System.err.println(packet);
        }
      }
    }
    */
  }
}

package net.tarpn;

import java.nio.charset.StandardCharsets;
import net.tarpn.config.Configuration;
import net.tarpn.io.DataPort;
import net.tarpn.io.impl.DataLinkManager;
import net.tarpn.io.impl.SerialDataPort;
import net.tarpn.packet.impl.ax25.AX25Call;
import net.tarpn.packet.impl.ax25.AX25Packet.HasInfo;
import net.tarpn.packet.impl.ax25.AX25StateEvent;
import net.tarpn.packet.impl.ax25.DataLinkEvent.DataIndicationDataLinkEvent;
import net.tarpn.packet.impl.ax25.DataLinkEvent.Type;

public class DataLinkMain {
  public static void main(String[] args) throws Exception {

    Configuration config = Configuration.newBuilder()
        .setAlias("DAVID2")
        .setNodeCall(AX25Call.create("K4DBZ", 2))
        .build();

    DataPort port1 = SerialDataPort.openPort(1, "/dev/tty.wchusbserial1410", 9600);

    DataLinkManager dataLinkManager = DataLinkManager.create(config, port1, dataLinkEvent -> {
      System.err.println("Got event " + dataLinkEvent);
      if(dataLinkEvent.getType().equals(Type.DL_DATA)) {
        byte[] info = ((HasInfo)((DataIndicationDataLinkEvent)dataLinkEvent).getPacket()).getInfo();
        if (new String(info, StandardCharsets.US_ASCII).equalsIgnoreCase("BYE")) {
          //
        }
      }
    }, packetRequest -> {});

    dataLinkManager.start();
  }
}

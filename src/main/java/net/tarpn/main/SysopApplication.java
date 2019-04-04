package net.tarpn.main;

import static net.tarpn.util.Util.ascii;

import java.util.function.Consumer;

import net.tarpn.datalink.DataLinkPrimitive;
import net.tarpn.datalink.DataLinkPrimitive.Type;
import net.tarpn.packet.impl.ax25.AX25Packet.Protocol;

public class SysopApplication {

  public void handle(DataLinkPrimitive request, Consumer<DataLinkPrimitive> response) {
    if(request.getType().equals(Type.DL_DATA)) {
      String message = ascii(request.getLinkInfo().getInfo()).trim();
      if(message.equalsIgnoreCase("BYE")) {
        response.accept(DataLinkPrimitive.newDisconnectRequest(request.getRemoteCall()));
      } else if(message.equalsIgnoreCase("PING")) {
        response.accept(DataLinkPrimitive.newDataRequest(request.getRemoteCall(), Protocol.NO_LAYER3, ascii("PONG\r")));
      } else {
        System.err.println("Got Message: " + message);
      }
    } else {
      System.err.println("Got Primitive: " + request);
    }

    /*
    String[] tokens = line.split(" ", 3); // Command Op1 Op2
    if(tokens[0].equalsIgnoreCase("PORTS") || line.equalsIgnoreCase("P")) {
      network.getPorts().values().forEach(dataPortManager -> {
        resp.accept(String.format("%d %s %s\r\n", dataPortManager.getDataPort().getPortNumber(),
            dataPortManager.getDataPort().getName(), dataPortManager.getDataPort().getType()));
      });
    }
    else if(tokens[0].equalsIgnoreCase("CONNECT") || tokens[0].equalsIgnoreCase("C")) {
      System.err.println("Trying for CONNECT");
      int port = Integer.parseInt(tokens[1]);
      network.getPortManager(port).getAx25StateHandler().getEventQueue().add(
          AX25StateEvent.createConnectEvent(AX25Call.fromString(tokens[2]))
      );
    }
    */
    /*else if(line.trim().equalsIgnoreCase("D")) {
      System.err.println("Trying for DISCONNECT");
      network.getPortManager(1).getAx25StateHandler().getEventQueue().add(
          AX25StateEvent.createDisconnectEvent(AX25Call.create("K4DBZ", 9))
      );
    } else {
      //System.err.println("Sending INFO");
      network.getPortManager(1).getAx25StateHandler().getEventQueue().add(
          AX25StateEvent.createDataEvent(
              AX25Call.create("K4DBZ", 9),
              Protocol.NO_LAYER3,
              line.trim().concat("\r").getBytes(StandardCharsets.US_ASCII))
      );
    }
    */
  }

}
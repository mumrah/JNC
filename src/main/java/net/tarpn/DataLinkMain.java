package net.tarpn;

import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.Queue;
import net.tarpn.config.Configuration;
import net.tarpn.io.DataPort;
import net.tarpn.io.impl.DataLinkManager;
import net.tarpn.io.impl.SerialDataPort;
import net.tarpn.packet.impl.ax25.AX25Call;
import net.tarpn.packet.impl.ax25.DataLinkPrimitive;
import net.tarpn.packet.impl.ax25.DataLinkPrimitive.Type;

public class DataLinkMain {
  public static void main(String[] args) {

    Configuration config = Configuration.newBuilder()
        .setAlias("DAVID2")
        .setNodeCall(AX25Call.create("K4DBZ", 2))
        .build();

    DataPort port1 = SerialDataPort.openPort(1, "/dev/tty.wchusbserial1410", 9600);

    Queue<DataLinkPrimitive> inQueue = new LinkedList<>();

    DataLinkManager dataLinkManager = DataLinkManager.create(config, port1, inQueue::add, packetRequest -> {});
    dataLinkManager.start();

    while(!Thread.currentThread().isInterrupted()) {
      DataLinkPrimitive primitive = inQueue.poll();
      if(primitive != null) {
        if(primitive.getType().equals(Type.DL_DATA)) {
          String message = new String(primitive.getPacket().getInfo(), StandardCharsets.US_ASCII).trim();
          if(message.equalsIgnoreCase("BYE")) {
            dataLinkManager.acceptDataLinkPrimitive(DataLinkPrimitive.newDisconnectRequest(primitive.getRemoteCall()));
          } else {
            System.err.println("Got Message: " + message);
          }
        } else {
          System.err.println("Got Primitive: " + primitive);
        }
      } else {
        try {
          Thread.sleep(10);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
    }
  }
}

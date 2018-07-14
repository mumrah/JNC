package net.tarpn.app;

import java.util.LinkedList;
import java.util.Queue;
import net.tarpn.config.Configuration;
import net.tarpn.io.DataPort;
import net.tarpn.datalink.DataLinkManager;
import net.tarpn.io.impl.SerialDataPort;
import net.tarpn.packet.impl.ax25.AX25Call;
import net.tarpn.datalink.LinkPrimitive;

public class DataLinkMain {
  public static void main(String[] args) {

    Configuration config = Configuration.newBuilder()
        .setAlias("DAVID2")
        .setNodeCall(AX25Call.create("K4DBZ", 2))
        .build();

    DataPort port1 = SerialDataPort.openPort(1, "/dev/tty.wchusbserial1410", 9600);

    Queue<LinkPrimitive> inQueue = new LinkedList<>();

    DataLinkManager dataLinkManager = DataLinkManager.create(config, port1, inQueue::add, packetRequest -> {});
    dataLinkManager.start();

    SysopApplication app = new SysopApplication();
    while(!Thread.currentThread().isInterrupted()) {
      LinkPrimitive primitive = inQueue.poll();
      if(primitive != null) {
        app.handle(primitive, dataLinkManager::acceptDataLinkPrimitive);
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

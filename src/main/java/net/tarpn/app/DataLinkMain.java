package net.tarpn.app;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;
import net.tarpn.config.Config;
import net.tarpn.config.PortConfig;
import net.tarpn.datalink.DataLinkManager;
import net.tarpn.datalink.LinkPrimitive;
import net.tarpn.io.DataPort;
import net.tarpn.io.impl.PortFactory;

public class DataLinkMain {
  public static void main(String[] args) throws IOException {
    Config config = Config.read("conf/sample.ini");

    PortConfig portConfig = config.getPortConfigs().get(1);

    DataPort port1 = PortFactory.createPortFromConfig(portConfig);

    Queue<LinkPrimitive> inQueue = new LinkedList<>();

    DataLinkManager dataLinkManager = DataLinkManager.create(portConfig, port1, inQueue::add, packetRequest -> {});
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

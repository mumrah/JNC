package net.tarpn.main;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import net.tarpn.network.NetworkManager;
import net.tarpn.network.netrom.NetRomSocket;
import net.tarpn.packet.impl.ax25.AX25Call;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NodeApplication {

  private static final Logger LOG = LoggerFactory.getLogger(NodeApplication.class);

  private final NetworkManager network;

  private NetRomSocket socket = null;

  public NodeApplication(NetworkManager networkManager) {
    this.network = networkManager;
  }

  public void handleLine(String line, Consumer<String> response) {
    String[] tokens = line.split(" ", 2);
    String command = tokens[0];
    if(commandEquals(command, "C", "CONNECT")) {
      String address = tokens[1];
      // TODO lookup call from alias in routing table
      AX25Call call = AX25Call.fromString(address);
      socket = network.open(call);
      int attempts = 0;
      LOG.debug("Attempting to connecting to " + call);

      try {
        //socket.tryConnect();
        while(!socket.isConnected() && attempts++ < 10) {
          Thread.sleep((int)Math.pow(2, attempts) * 1000);
          //socket.tryConnect();
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }

      if(!socket.isConnected()) {
        response.accept("Could not connect to " + call + " after 10 attempts");
        socket = null;
      } else {
        LOG.debug("Connected to " + call);
        response.accept("Connected to " + call + "!");
      }
    } else if(commandEquals(command, "I", "INFO")) {
      response.accept("Here is some info.");
    } else if(commandEquals(command, "B", "BYE")) {
      LOG.debug("Attempting to disconnect from " + socket.getAddress());
      socket.close();
      LOG.debug("Disconnected from " + socket.getAddress());
    } else {
      // Not a command
      if(socket.isConnected()) {
        LOG.debug("Sending info '" + line + "' to " + socket.getAddress());
        socket.send(line.getBytes(StandardCharsets.US_ASCII));
      } else {
        response.accept("Huh?");
      }
    }
  }

  private boolean commandEquals(String test, String commandShort, String commandLong) {
    return test.equalsIgnoreCase(commandShort) || test.equalsIgnoreCase(commandLong);
  }
}

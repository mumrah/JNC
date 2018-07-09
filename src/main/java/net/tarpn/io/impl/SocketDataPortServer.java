package net.tarpn.io.impl;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import net.tarpn.io.DataPort;
import net.tarpn.packet.impl.ax25.AX25Packet;

// TODO need to wire this in with the NetworkManager stuff
public class SocketDataPortServer {

  private final ExecutorService clientExecutor = Executors.newCachedThreadPool();
  private final Consumer<AX25Packet> incomingPackets;

  public SocketDataPortServer(Consumer<AX25Packet> incomingPackets) {
    this.incomingPackets = incomingPackets;
  }

  public Runnable getRunnable() {
    return () -> {
      try {
        ServerSocket serverSocket = new ServerSocket(7777);
        System.err.println("Started socket server on 7777");
        while(true) {
          Socket clientSocket = serverSocket.accept();
          System.err.println("New connection at " + clientSocket.getLocalAddress());
          DataPort port = new SocketDataPort(clientSocket.getLocalPort(), clientSocket.getRemoteSocketAddress().toString(), clientSocket);
          // TODO
        }
      } catch (IOException e) {
        throw new RuntimeException("Socket server had an error", e);
      }
    };
  }
}

package net.tarpn.io.impl;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.tarpn.Main;
import net.tarpn.frame.FrameReader;
import net.tarpn.frame.impl.KISSFrameReader;
import net.tarpn.io.DataPort;
import net.tarpn.message.Message;

public class SocketDataPortServer implements Runnable {

  private final ExecutorService clientExecutor = Executors.newCachedThreadPool();
  private final Queue<Message> inboundMessageQueue;

  public SocketDataPortServer(Queue<Message> inboundMessageQueue) {
    this.inboundMessageQueue = inboundMessageQueue;
  }

  @Override
  public void run() {
    try {
      ServerSocket serverSocket = new ServerSocket(7777);
      System.err.println("Started socket server on 7777");
      while(true) {
        Socket clientSocket = serverSocket.accept();
        System.err.println("New connection at " + clientSocket.getLocalAddress());
        DataPort port = new SocketDataPort(clientSocket.getLocalPort(), clientSocket.getRemoteSocketAddress().toString(), clientSocket);
        FrameReader frameReader = new KISSFrameReader(port.getPortNumber());
        clientExecutor.submit(
            Main.newPortHandler(port, frameReader, inboundMessageQueue));
      }
    } catch (IOException e) {
      throw new RuntimeException("Socket server had an error", e);
    }

  }
}

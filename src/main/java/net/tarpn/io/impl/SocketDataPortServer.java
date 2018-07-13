package net.tarpn.io.impl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.tarpn.app.SysopApplication;
import net.tarpn.network.NetworkManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SocketDataPortServer {

  private static final Logger LOG = LoggerFactory.getLogger(SocketDataPortServer.class);
  private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();

  private final NetworkManager networkManager;
  private final SysopApplication app;


  public SocketDataPortServer(NetworkManager networkManager) {
    this.networkManager = networkManager;
    this.app = new SysopApplication();
  }

  public void start() {
    EXECUTOR.submit(() -> {
      try {
        ServerSocket serverSocket = new ServerSocket(7777);
        System.err.println("Started socket server on 7777");
        while(!Thread.currentThread().isInterrupted()) {
          Socket clientSocket = serverSocket.accept();
          System.err.println("New connection at " + clientSocket.getLocalAddress());

          // reader
          EXECUTOR.submit(() -> {
            InputStream inputStream = null;
            try {
              inputStream = clientSocket.getInputStream();
            } catch (IOException e) {
              LOG.error("Could not open client input stream", e);
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            while(!Thread.currentThread().isInterrupted()) {
              try {
                String line;
                while((line = reader.readLine()) != null) {
                  // TODO send to app
                }
              } catch (IOException e) {
                LOG.error("Could not read line from client", e);
              }
            }
          });
        }
      } catch (IOException e) {
        throw new RuntimeException("Socket server had an error", e);
      }
    });
  }
}

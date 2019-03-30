package net.tarpn.io.socket;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.*;
import java.util.function.Consumer;

import net.tarpn.app.Application;
import net.tarpn.app.ApplicationRegistry;
import net.tarpn.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SocketDataPortServer {

  private static final Logger LOG = LoggerFactory.getLogger(SocketDataPortServer.class);
  private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();

  private final ApplicationRegistry applicationRegistry;
  private Future<?> acceptorFuture;

  public SocketDataPortServer(ApplicationRegistry applicationRegistry) {
    this.applicationRegistry = applicationRegistry;
  }

  public void start() {
    ServerSocket serverSocket = null;
    try {
      serverSocket = new ServerSocket(7777);
    } catch (IOException e) {
      throw new RuntimeException("Could not start socket server", e);
    }

    SocketAcceptor acceptor = new SocketAcceptor(serverSocket, clientSocket ->
      EXECUTOR.submit(new SocketClientHandler(clientSocket,
              applicationRegistry.loadApplication("default", String.class))));
    acceptorFuture = EXECUTOR.submit(acceptor);
  }

  public void join() throws ExecutionException, InterruptedException {
    acceptorFuture.get();
  }

  static class SocketAcceptor implements Runnable {

    private final ServerSocket serverSocket;
    private final Consumer<Socket> onAccept;

    SocketAcceptor(ServerSocket serverSocket, Consumer<Socket> onAccept) {
      this.serverSocket = serverSocket;
      this.onAccept = onAccept;
    }

    @Override
    public void run() {
      while(!Thread.currentThread().isInterrupted()) {
        Socket clientSocket;
        try {
          clientSocket = serverSocket.accept();
          onAccept.accept(clientSocket);
        } catch (IOException e) {
          throw new RuntimeException("Socket server had an error", e);
        }
        System.err.println("New connection at " + clientSocket.getLocalAddress());
      }
    }
  }

  static class SocketClientHandler implements Runnable {

    private final Socket clientSocket;
    private final Application<String> textApplication;

    SocketClientHandler(Socket clientSocket, Application<String> textApplication) {
      this.clientSocket = clientSocket;
      this.textApplication = textApplication;
    }

    @Override
    public void run() {
      InputStream inputStream;
      OutputStream outputStream;
      try {
        inputStream = clientSocket.getInputStream();
        outputStream = clientSocket.getOutputStream();
      } catch (IOException e) {
        LOG.error("Could not open client input stream", e);
        throw new RuntimeException(e);
      }

      BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
      PrintWriter writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(outputStream)), true);
      textApplication.onConnect(writer::println);

      while (!Thread.currentThread().isInterrupted() && clientSocket.isConnected()) {
        // TODO timeout on inactivity
        try {
          String line = reader.readLine();
          if (line != null) {
            textApplication.handle(line, writer::println, () -> {
              LOG.info("Closing client socket");
              Util.closeQuietly(clientSocket);
              Thread.currentThread().interrupt();
            });
          } else {
            Thread.sleep(10);
          }
        } catch (InterruptedException e) {
          LOG.error("Got interrupted, shutting down client connection", e);
          writer.println("Sorry, we had an error. Closing.");
          Util.closeQuietly(clientSocket);
          Thread.currentThread().interrupt();
          break;
        } catch (IOException e) {
          LOG.error("Could not read line from client", e);
          Util.closeQuietly(clientSocket);
          break;
        }
      }
    }
  }
}

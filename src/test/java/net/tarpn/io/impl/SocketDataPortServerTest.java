package net.tarpn.io.impl;

import net.tarpn.app.ApplicationRegistry;
import net.tarpn.io.socket.SocketDataPortServer;
import org.junit.Test;

import java.util.concurrent.ExecutionException;

public class SocketDataPortServerTest {
    @Test
    public void testServer() throws ExecutionException, InterruptedException {
        ApplicationRegistry registry = new ApplicationRegistry();
        registry.registerApplication(new ApplicationRegistry.DefaultApplication(registry));
        registry.registerApplication(new ApplicationRegistry.EchoApplication());

        SocketDataPortServer server = new SocketDataPortServer(registry);
        server.start();
        server.join();
    }
}

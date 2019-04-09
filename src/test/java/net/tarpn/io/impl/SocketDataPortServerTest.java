package net.tarpn.io.impl;

import net.tarpn.app.ApplicationRegistry;
import net.tarpn.app.DefaultApplication;
import net.tarpn.app.EchoApplication;
import net.tarpn.io.socket.SocketDataPortServer;
import org.junit.Test;

import java.util.concurrent.ExecutionException;

public class SocketDataPortServerTest {
    @Test
    public void testServer() throws ExecutionException, InterruptedException {
        ApplicationRegistry registry = new ApplicationRegistry();
        registry.registerApplication(new DefaultApplication(registry));
        registry.registerApplication(new EchoApplication());

        SocketDataPortServer server = new SocketDataPortServer(registry);
        server.start();
        server.join();
    }
}

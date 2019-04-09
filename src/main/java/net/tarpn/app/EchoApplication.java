package net.tarpn.app;

import java.util.function.Consumer;

public class EchoApplication implements Application<String> {
    @Override
    public String getName() {
        return "echo";
    }

    @Override
    public Class<String> getType() {
        return String.class;
    }

    @Override
    public void onConnect(Consumer<String> response) {
        response.accept("Welcome to ECHO");
    }

    @Override
    public void handle(String request, Consumer<String> response, Runnable closer) {
        if (request.equalsIgnoreCase("BYE")) {
            response.accept("Goodbye from ECHO");
            closer.run();
        } else {
            response.accept("You said: " + request);
        }
    }

    @Override
    public void onDisconnect(Consumer<String> response) {
        response.accept("Goodbye from ECHO");
    }
}

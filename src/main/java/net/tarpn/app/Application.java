package net.tarpn.app;

import java.util.function.Consumer;

public interface Application<T> {

    String getName();

    Class<T> getType();

    void onConnect(Consumer<T> response);

    void handle(T request, Consumer<T> response, Runnable closer);

    void onDisconnect(Consumer<T> response);
}

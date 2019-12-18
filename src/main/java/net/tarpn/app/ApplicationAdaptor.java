package net.tarpn.app;

import net.tarpn.network.netrom.NetworkPrimitive;
import net.tarpn.util.Util;

import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

public class ApplicationAdaptor implements Consumer<NetworkPrimitive> {

    private final Application<String> application;
    private final Consumer<NetworkPrimitive> networkConsumer;

    public ApplicationAdaptor(Application<String> application, Consumer<NetworkPrimitive> networkConsumer) {
        this.application = application;
        this.networkConsumer = networkConsumer;
    }

    @Override
    public void accept(NetworkPrimitive event) {
        Consumer<String> stringResponder = response -> networkConsumer.accept(null);
                //NetworkPrimitive.newData(event.getRemoteCall(), response.getBytes(StandardCharsets.UTF_8)));
        try {
            switch (event.getType()) {
                case NL_CONNECT:
                    Thread.sleep(100);
                    application.onConnect(stringResponder);
                    break;
                case NL_DISCONNECT:
                    Thread.sleep(100);
                    application.onDisconnect(stringResponder);
                    break;
                case NL_INFO:
                    application.handle(Util.toEscapedASCII(event.getInfo()), stringResponder, () -> {
                        //networkConsumer.accept(NetworkPrimitive.newDisconnect(event.getRemoteCall()));
                    });
                    break;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

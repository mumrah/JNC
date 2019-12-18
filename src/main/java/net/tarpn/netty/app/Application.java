package net.tarpn.netty.app;


public interface Application {
    default void onConnect(Context context) throws Exception { };

    /**
     * This application has been disconnected from its channel. Clean up any resources here
     * @param context
     * @throws Exception
     */
    default void onDisconnect(Context context) throws Exception { };

    default void onError(Context context, Throwable t) throws Exception { };

    /**
     * New data is available to be read by this application. The main logic goes here
     * @param context
     * @param message
     * @throws Exception
     */
    void read(Context context, byte[] message) throws Exception;

    /**
     * Request to close this application. If this application's channel is a L2
     * link, this sends a DISC to the remote station. If it's a telnet session, it will
     * close the client socket.
     *
     * @param context
     * @throws Exception
     */
    default void close(Context context) throws Exception { };
}

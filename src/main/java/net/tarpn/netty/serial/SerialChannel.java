package net.tarpn.netty.serial;

import com.fazecast.jSerialComm.SerialPort;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPromise;
import io.netty.channel.oio.OioByteStreamChannel;

import java.io.FilterInputStream;
import java.io.IOException;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

public class SerialChannel extends OioByteStreamChannel {

    public static class SerialDeviceAddress extends SocketAddress {

        private final String value;

        /**
         * Creates a SerialDeviceAddress representing the address of the serial port.
         *
         * @param value the address of the device (e.g. COM1, /dev/ttyUSB0, ...)
         */
        public SerialDeviceAddress(String value) {
            this.value = value;
        }

        /**
         * @return The serial port address of the device (e.g. COM1, /dev/ttyUSB0, ...)
         */
        public String value() {
            return value;
        }
    }

    private static final SerialDeviceAddress LOCAL_ADDRESS = new SerialDeviceAddress("localhost");

    private final SerialChannelConfig config;

    private boolean open = true;
    private SerialDeviceAddress deviceAddress;
    private SerialPort serialPort;

    public SerialChannel() {
        super(null);
        config = new DefaultSerialChannelConfig(this);
        config.setWaitTimeMillis(1000);
    }

    @Override
    public SerialChannelConfig config() {
        return config;
    }

    @Override
    public boolean isOpen() {
        return open;
    }

    @Override
    protected AbstractUnsafe newUnsafe() {
        return new SerialUnsafe();
    }

    @Override
    protected void doConnect(SocketAddress remoteAddress, SocketAddress localAddress) throws Exception {
        SerialDeviceAddress remote = (SerialDeviceAddress) remoteAddress;
        SerialPort port = SerialPort.getCommPort(remote.value());
        port.setBaudRate(config().getBaudrate());
        // TODO configure these timeouts
        port.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 100, 0);
        port.openPort(100);

        deviceAddress = remote;
        serialPort = port;
    }

    protected void doInit() throws Exception {
        if (!serialPort.isOpen()) {
            throw new Exception("Serial Port " + serialPort.getDescriptivePortName() + " is not open");
        }
        // TODO other settings
        FilterInputStream inputStream = new FilterInputStream(serialPort.getInputStream()) {

            @Override
            public int read() throws IOException {
                return in.read();
            }

            @Override
            public int read(byte[] b) throws IOException {
                return in.read(b);
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                byte[] buf = new byte[len];
                int read = in.read(buf);
                System.arraycopy(buf, 0, b, off, read);
                return read;
            }

        };
        activate(inputStream, serialPort.getOutputStream());
    }

    @Override
    public SerialDeviceAddress localAddress() {
        return (SerialDeviceAddress) super.localAddress();
    }

    @Override
    public SerialDeviceAddress remoteAddress() {
        return (SerialDeviceAddress) super.remoteAddress();
    }

    @Override
    protected SerialDeviceAddress localAddress0() {
        return LOCAL_ADDRESS;
    }

    @Override
    protected SerialDeviceAddress remoteAddress0() {
        return deviceAddress;
    }

    @Override
    protected void doBind(SocketAddress localAddress) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void doDisconnect() throws Exception {
        doClose();
    }

    @Override
    protected void doClose() throws Exception {
        open = false;
        try {
            super.doClose();
        } finally {
            if (serialPort != null) {
                serialPort.closePort();
                serialPort = null;
            }
        }
    }

    @Override
    protected boolean isInputShutdown() {
        return !open;
    }

    @Override
    protected ChannelFuture shutdownInput() {
        return newFailedFuture(new UnsupportedOperationException("shutdownInput"));
    }

    private final class SerialUnsafe extends AbstractUnsafe {
        @Override
        public void connect(
                final SocketAddress remoteAddress,
                final SocketAddress localAddress, final ChannelPromise promise) {
            if (!promise.setUncancellable() || !ensureOpen(promise)) {
                return;
            }

            try {
                final boolean wasActive = isActive();
                doConnect(remoteAddress, localAddress);

                int waitTime = config().getOption(SerialChannelOption.WAIT_TIME);
                if (waitTime > 0) {
                    eventLoop().schedule(() -> {
                        try {
                            doInit();
                            safeSetSuccess(promise);
                            if (!wasActive && isActive()) {
                                pipeline().fireChannelActive();
                            }
                        } catch (Throwable t) {
                            safeSetFailure(promise, t);
                            closeIfClosed();
                        }
                    }, waitTime, TimeUnit.MILLISECONDS);
                } else {
                    doInit();
                    safeSetSuccess(promise);
                    if (!wasActive && isActive()) {
                        pipeline().fireChannelActive();
                    }
                }
            } catch (Throwable t) {
                safeSetFailure(promise, t);
                closeIfClosed();
            }
        }
    }
}

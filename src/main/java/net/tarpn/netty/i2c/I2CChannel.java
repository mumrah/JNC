package net.tarpn.netty.i2c;

import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CDevice;
import com.pi4j.io.i2c.I2CFactory;
import com.pi4j.platform.Platform;
import com.pi4j.platform.PlatformManager;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelPromise;
import io.netty.channel.DefaultChannelConfig;
import io.netty.channel.oio.OioByteStreamChannel;
import net.tarpn.frame.impl.KISS;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

public class I2CChannel extends OioByteStreamChannel {

    private static final I2CDeviceAddress LOCAL_ADDRESS = new I2CDeviceAddress(0, 0);

    public static class I2CDeviceAddress extends SocketAddress {

        public final int i2cBus;
        public final int i2cAddress;

        public I2CDeviceAddress(int i2cBus, int i2cAddress) {
           this.i2cBus = i2cBus;
           this.i2cAddress = i2cAddress;
        }
    }

    private final ChannelConfig config;

    private boolean open = true;
    private I2CDeviceAddress deviceAddress;
    private I2CBus i2cBus;
    private I2CDevice i2CDevice;

    public I2CChannel() {
        super(null);
        this.config = new DefaultChannelConfig(this);
    }

    @Override
    public ChannelConfig config() {
        return config;
    }

    @Override
    public boolean isOpen() {
        return open;
    }

    @Override
    protected AbstractUnsafe newUnsafe() {
        return new I2CUnsafe();
    }

    @Override
    protected void doConnect(SocketAddress remoteAddress, SocketAddress localAddress) throws Exception {
        I2CDeviceAddress remote = (I2CDeviceAddress) remoteAddress;
        PlatformManager.setPlatform(Platform.RASPBERRYPI); // TODO make this an option
        I2CBus i2c = I2CFactory.getInstance(remote.i2cBus);
        I2CDevice device = i2c.getDevice(remote.i2cAddress);
        this.deviceAddress = remote;
        this.i2cBus = i2c;
        this.i2CDevice = device;
    }

    protected void doInit() throws Exception {
        if (i2CDevice == null) {
            throw new Exception("I2C Port " + deviceAddress + " is not open");
        }

        InputStream inputStream = new InputStream() {
            @Override
            public int read() throws IOException {
                int val;
                // For TNC-Pi, 0x0e means no data. Maybe make this an option
                while ((val = i2CDevice.read()) == 0x0e) {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        throw new IOException(e);
                    }
                }
                return val;
            }
        };

        OutputStream outputStream = new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                i2CDevice.write((byte)(b & 0xff));
            }
        };

        activate(inputStream, outputStream);
    }

    @Override
    public I2CDeviceAddress localAddress() {
        return (I2CDeviceAddress) super.localAddress();
    }

    @Override
    public I2CDeviceAddress remoteAddress() {
        return (I2CDeviceAddress) super.remoteAddress();
    }

    @Override
    protected I2CDeviceAddress localAddress0() {
        return LOCAL_ADDRESS;
    }

    @Override
    protected I2CDeviceAddress remoteAddress0() {
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
            if (i2CDevice != null) {
                i2cBus.close();
                i2CDevice = null;
                i2cBus = null;
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

    private class I2CUnsafe extends AbstractUnsafe {
        @Override
        public void connect(final SocketAddress remoteAddress,
                            final SocketAddress localAddress,
                            final ChannelPromise promise) {
            if (!promise.setUncancellable() || !ensureOpen(promise)) {
                return;
            }

            try {
                final boolean wasActive = isActive();
                doConnect(remoteAddress, localAddress);

                // Reset the TNC
                i2CDevice.write(KISS.Protocol.FEND.asByte());
                i2CDevice.write((byte)15);
                i2CDevice.write((byte)2);

                // Wait a bit
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
                }, 2000, TimeUnit.MILLISECONDS);

            } catch (Throwable t) {
                safeSetFailure(promise, t);
                closeIfClosed();
            }
        }
    }
}

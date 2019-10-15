package net.tarpn.netty.serial;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.*;

import java.util.Map;

import static net.tarpn.netty.serial.SerialChannelOption.WAIT_TIME;


public class DefaultSerialChannelConfig extends DefaultChannelConfig implements SerialChannelConfig {
    private volatile int baudrate = 115200;
    private volatile boolean dtr;
    private volatile boolean rts;
    private volatile Stopbits stopbits = Stopbits.STOPBITS_1;
    //private volatile Databits databits = Databits.DATABITS_8;
    private volatile Paritybit paritybit = Paritybit.NONE;
    private volatile int waitTime;
    private volatile int readTimeout = 1000;

    DefaultSerialChannelConfig(SerialChannel channel) {
        super(channel);
        setAllocator(new PreferHeapByteBufAllocator(getAllocator()));
    }

    @Override
    public Map<ChannelOption<?>, Object> getOptions() {
        return getOptions(super.getOptions(), WAIT_TIME);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getOption(ChannelOption<T> option) {
        if (option == WAIT_TIME) {
            return (T) Integer.valueOf(getWaitTimeMillis());
        }
        return super.getOption(option);
    }

    @Override
    public <T> boolean setOption(ChannelOption<T> option, T value) {
        validate(option, value);

        if (option == WAIT_TIME) {
            setWaitTimeMillis((Integer) value);
        }
        return true;
    }


    @Override
    public SerialChannelConfig setBaudrate(int baudrate) {
        this.baudrate = baudrate;
        return this;
    }

    @Override
    public SerialChannelConfig setStopbits(Stopbits stopbits) {
        this.stopbits = stopbits;
        return this;
    }

    @Override
    public SerialChannelConfig setParitybit(Paritybit paritybit) {
        this.paritybit = paritybit;
        return this;
    }

    @Override
    public int getBaudrate() {
        return baudrate;
    }

    @Override
    public Stopbits getStopbits() {
        return stopbits;
    }

    @Override
    public Paritybit getParitybit() {
        return paritybit;
    }

    @Override
    public boolean isDtr() {
        return dtr;
    }

    @Override
    public SerialChannelConfig setDtr(boolean dtr) {
        this.dtr = dtr;
        return this;
    }

    @Override
    public boolean isRts() {
        return rts;
    }

    @Override
    public SerialChannelConfig setRts(boolean rts) {
        this.rts = rts;
        return this;
    }

    @Override
    public int getWaitTimeMillis() {
        return waitTime;
    }

    @Override
    public SerialChannelConfig setWaitTimeMillis(int waitTimeMillis) {
        this.waitTime = waitTimeMillis;
        return this;
    }

    @Override
    public SerialChannelConfig setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
        return this;
    }

    @Override
    public int getReadTimeout() {
        return readTimeout;
    }

    @Override
    public SerialChannelConfig setConnectTimeoutMillis(int connectTimeoutMillis) {
        super.setConnectTimeoutMillis(connectTimeoutMillis);
        return this;
    }

    @Override
    @Deprecated
    public SerialChannelConfig setMaxMessagesPerRead(int maxMessagesPerRead) {
        super.setMaxMessagesPerRead(maxMessagesPerRead);
        return this;
    }

    @Override
    public SerialChannelConfig setWriteSpinCount(int writeSpinCount) {
        super.setWriteSpinCount(writeSpinCount);
        return this;
    }

    @Override
    public SerialChannelConfig setAllocator(ByteBufAllocator allocator) {
        super.setAllocator(allocator);
        return this;
    }

    @Override
    public SerialChannelConfig setRecvByteBufAllocator(RecvByteBufAllocator allocator) {
        super.setRecvByteBufAllocator(allocator);
        return this;
    }

    @Override
    public SerialChannelConfig setAutoRead(boolean autoRead) {
        super.setAutoRead(autoRead);
        return this;
    }

    @Override
    public SerialChannelConfig setAutoClose(boolean autoClose) {
        super.setAutoClose(autoClose);
        return this;
    }

    @Override
    public SerialChannelConfig setWriteBufferHighWaterMark(int writeBufferHighWaterMark) {
        super.setWriteBufferHighWaterMark(writeBufferHighWaterMark);
        return this;
    }

    @Override
    public SerialChannelConfig setWriteBufferLowWaterMark(int writeBufferLowWaterMark) {
        super.setWriteBufferLowWaterMark(writeBufferLowWaterMark);
        return this;
    }

    @Override
    public SerialChannelConfig setWriteBufferWaterMark(WriteBufferWaterMark writeBufferWaterMark) {
        super.setWriteBufferWaterMark(writeBufferWaterMark);
        return this;
    }

    @Override
    public SerialChannelConfig setMessageSizeEstimator(MessageSizeEstimator estimator) {
        super.setMessageSizeEstimator(estimator);
        return this;
    }
}

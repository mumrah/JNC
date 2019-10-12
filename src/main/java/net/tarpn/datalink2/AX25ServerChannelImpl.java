package net.tarpn.datalink2;

import com.fazecast.jSerialComm.SerialPort;
import net.tarpn.config.PortConfig;
import net.tarpn.datalink.DataLinkPrimitive;
import net.tarpn.packet.Packet;
import net.tarpn.packet.impl.ax25.AX25Call;
import net.tarpn.packet.impl.ax25.AX25StateMachine;
import net.tarpn.util.Clock;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Queue;

public class DataLinkChannelImpl implements DataLinkChannel {

    private final SerialPort serialPort;
    private final Queue<Packet> outboundPackets;
    private final AX25StateMachine ax25StateHandler;

    DataLinkChannelImpl(PortConfig portConfig, Clock clock) {
        this.serialPort = SerialPort.getCommPort(portConfig.getSerialDevice());
        this.serialPort.setBaudRate(portConfig.getSerialSpeed());
        this.outboundPackets = new ArrayDeque<>();
        this.ax25StateHandler = new AX25StateMachine(portConfig, outboundPackets::add, this::onDataLinkEvent, clock);
    }

    /**
     * React to DL events emitted from the state machine
     * @param dataLinkPrimitive
     */
    private void onDataLinkEvent(DataLinkPrimitive dataLinkPrimitive) {
       switch (dataLinkPrimitive.getType()) {
           case DL_CONNECT:
               break;
           case DL_DISCONNECT:
               break;
           case DL_DATA:
               break;
           case DL_UNIT_DATA:
               break;
           case DL_ERROR:
               break;
       }
    }

    @Override
    public DataLinkChannel bind(AX25Call local) {
        this.serialPort.openPort();
        return this;
    }

    @Override
    public boolean connect(AX25Call remote) {

    }

    @Override
    public AX25Call getLocalCallsign() {
        return null;
    }

    @Override
    public AX25Call getRemoteCallsign() {
        return null;
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        InputStream inputStream = serialPort.getInputStream();
        if (inputStream.available() > 0) {
            int available = inputStream.available();
            byte[] buffer = new byte[available];
            int read = inputStream.read(buffer);
            dst.put(buffer, 0, read);
            return read;
        } else {
            return 0;
        }
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        OutputStream outputStream = serialPort.getOutputStream();
        int size = src.remaining();
        byte[] buffer = new byte[size];
        src.get(buffer);
        outputStream.write(buffer);
        return size;
    }

    @Override
    public boolean isOpen() {
        return serialPort.isOpen();
    }

    @Override
    public void close() throws IOException {
        serialPort.closePort();
    }
}

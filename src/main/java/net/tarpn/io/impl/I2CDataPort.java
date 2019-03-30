package net.tarpn.io.impl;

import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CDevice;
import com.pi4j.io.i2c.I2CFactory;
import net.tarpn.io.DataPort;
import net.tarpn.util.Util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class I2CDataPort implements DataPort {

    private final int portNumber;
    private final int i2cBusIdx;
    private final int i2cAddr;

    private I2CBus i2CBus;
    private I2CDevice i2CDevice;

    I2CDataPort(int portNumber, int i2cBusIdx, int i2cAddr) {
        this.portNumber = portNumber;
        this.i2cBusIdx = i2cBusIdx;
        this.i2cAddr = i2cAddr;
    }


    public static I2CDataPort createPort(int portNumber, int i2cBusIdx, int i2cAddr) {
        return new I2CDataPort(portNumber, i2cBusIdx, i2cAddr);
    }

    @Override
    public void open() throws IOException {
        try {
            i2CBus = I2CFactory.getInstance(i2cBusIdx);
            i2CDevice = i2CBus.getDevice(i2cAddr);
        } catch (Exception e) {
            throw new IOException("Could not open i2c device " + Util.toHexString(i2cAddr) + " on bus " + i2cBusIdx);
        }
    }

    @Override
    public void close() throws IOException {
        i2CBus.close();
    }

    @Override
    public boolean isOpen() {
        return i2CDevice != null;
    }

    @Override
    public boolean reopen() {
        return false;
    }

    @Override
    public InputStream getInputStream() {
        return new InputStream() {
            @Override
            public int read() throws IOException {
                return i2CDevice.read();
            }
        };
    }

    @Override
    public OutputStream getOutputStream() {
        return new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                //ByteBuffer buffer = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN);
                //buffer.putInt(b);
                i2CDevice.write((byte)b);
            }
        };
    }

    @Override
    public int getPortNumber() {
        return portNumber;
    }

    @Override
    public String getName() {
        return "i2c";
    }

    @Override
    public String getType() {
        return "i2c " + Util.toHexString(i2cAddr);
    }
}

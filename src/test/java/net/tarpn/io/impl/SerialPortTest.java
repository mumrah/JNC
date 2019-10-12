package net.tarpn.io.impl;

import com.pi4j.io.serial.Serial;
import com.pi4j.io.serial.SerialConfig;
import com.pi4j.io.serial.SerialFactory;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

public class SerialPortTest {
    @Test
    public void test() throws Exception {
        Serial serial = SerialFactory.createInstance();
        SerialConfig config = new SerialConfig();
        serial.open("/tmp/vmodem0", 9600);
    }
}

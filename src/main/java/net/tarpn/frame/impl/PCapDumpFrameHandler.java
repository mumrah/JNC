package net.tarpn.frame.impl;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import net.tarpn.frame.FrameHandler;
import net.tarpn.frame.FrameRequest;

public class PCapDumpFrameHandler implements FrameHandler {

  private final OutputStream outputStream;

  public PCapDumpFrameHandler() {
    try {
      Path dumpFile = Paths.get("build/ax25.pcap");
      if(Files.exists(dumpFile)) {
        outputStream = Files.newOutputStream(Paths.get("build/ax25.pcap"), StandardOpenOption.WRITE);
      } else {
        outputStream = Files.newOutputStream(Paths.get("build/ax25.pcap"), StandardOpenOption.CREATE_NEW);
        writeHeader();
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  void writeHeader() {
    int[] header = new int[]{
        0xd4, 0xc3, 0xb2, 0xa1, // magic
        0x02, 0x00, 0x04, 0x00,  // version
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, // time stuff?
        0xff, 0xff, 0x00, 0x00, // snapshot length
        0x03, 0x00, 0x00, 0x00, // AX.25 protocol
    };
    byte[] headerbytes = new byte[header.length];
    for(int i=0; i<header.length; i++){
      headerbytes[i] = (byte)header[i];
    }
    try {
      outputStream.write(headerbytes);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void onFrame(FrameRequest frameRequest) {
    byte[] data = frameRequest.getFrame().getData();
    dump(data);
  }

  public synchronized void dump(byte[] data) {
    int time = (int)(System.currentTimeMillis() / 1000);
    ByteBuffer bb = ByteBuffer.allocate(1024);
    bb.order(ByteOrder.LITTLE_ENDIAN);
    bb.putInt(time); // seconds
    bb.putInt(0); // microseconds
    bb.putInt(data.length);
    bb.putInt(data.length);
    bb.put(data);
    int len = bb.position();
    bb.position(0);

    byte[] packet = new byte[len];
    bb.get(packet, 0, len);
    try {
      outputStream.write(packet);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}

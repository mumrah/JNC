package net.tarpn;

import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class Util {
  public static void hexDump(byte[] msg, PrintStream printStream) {
    for (int j = 1; j < msg.length+1; j++) {
      if (j % 8 == 1 || j == 0) {
        if( j != 0){
          printStream.println();
        }
        printStream.format("0%d\t|\t", j / 8);
      }
      printStream.format("%02X", msg[j-1]);
      //if (j % 4 == 0) {
        printStream.print(" ");
      //}
    }
    printStream.println();
  }

  public static String toHexString(byte[] msg) {
    StringBuilder out = new StringBuilder();
    for(int i=0; i<msg.length; i++) {
      out.append(Integer.toHexString(msg[i] & 0xFF));
      out.append(" ");
    }
    return out.toString().trim();
  }

  public static String toHexString(byte b) {
    return String.format("0x%02x", b);
  }

  public static String toEscapedASCII(byte[] msg) {
    return new String(msg, StandardCharsets.US_ASCII)
        .replace("\r", "\\r")
        .replace("\n", "\\n")
        .replace("\t", "\\t")
        .replace("\b", "\\b")
        .replace("\f", "\\f");
  }

  public static void pcap(byte[] ax25) throws Exception {
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

    int time = (int)(System.currentTimeMillis() / 1000);
    ByteBuffer bb = ByteBuffer.allocate(1024);
    bb.order(ByteOrder.LITTLE_ENDIAN);
    bb.put(headerbytes);
    bb.putInt(time); // seconds
    bb.putInt(0); // microseconds
    bb.putInt(ax25.length);
    bb.putInt(ax25.length);
    bb.put(ax25);
    int len = bb.position();
    bb.position(0);



    byte[] packet = new byte[len];
    bb.get(packet, 0, len);

    //DatagramSocket socket = new DatagramSocket();
    //socket.send(new DatagramPacket(packet,  len, InetAddress.getByName("localhost"), 5555));
    //socket.close();
    OutputStream os = Files.newOutputStream(
        Paths.get("/Users/mumrah/Downloads/test.pcap"), StandardOpenOption.CREATE_NEW);
    os.write(packet);
    os.close();
  }

  public static void main(String[] args) throws Exception {
    pcap(new byte[]{
        (byte)0x96, (byte)0x68, (byte)0x88, (byte)0x84, (byte)0xB4, (byte)0x40, (byte)0x02, (byte)0x96,
        (byte)0x68, (byte)0x88, (byte)0x84, (byte)0xB4, (byte)0x40, (byte)0x85, (byte)0x73
    });
  }

  public static byte[] copyFromBuffer(ByteBuffer buffer) {
    int len = buffer.position();
    byte[] arr = new byte[len];
    buffer.position(0);
    buffer.get(arr, 0, len);
    return arr;
  }
}


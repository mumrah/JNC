package net.tarpn.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class Util {

  public static String toHexDump(byte[] msg) {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    hexDump(msg, baos);
    try {
      baos.flush();
    } catch (IOException e) {
      // do nothing
    }
    byte[] output = baos.toByteArray();
    return new String(output, StandardCharsets.US_ASCII);
  }

  public static void hexDump(byte[] msg, OutputStream outputStream) {
    try {
      for (int j = 1; j < msg.length + 1; j++) {
        if (j % 8 == 1 || j == 0) {
          if (j != 0) {
            outputStream.write('\n');
          }
          outputStream.write(String.format("0%d\t|\t", j / 8).getBytes(StandardCharsets.US_ASCII));
        }
        outputStream.write(String.format("%02X", msg[j - 1]).getBytes(StandardCharsets.US_ASCII));
        //if (j % 4 == 0) {
        outputStream.write(' ');
        //}
      }
      outputStream.write('\n');
    } catch (IOException e) {
      // do nothing
    }
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

  public static byte[] ascii(String s) {
    return s.getBytes(StandardCharsets.US_ASCII);
  }

  public static String ascii(byte[] b) {
    return new String(b, StandardCharsets.US_ASCII);
  }

  public static byte[] copyFromBuffer(ByteBuffer buffer) {
    int len = buffer.position();
    byte[] arr = new byte[len];
    buffer.position(0);
    buffer.get(arr, 0, len);
    return arr;
  }

  public static <T> void queueProcessingLoop(
      Supplier<T> valueSupplier,
      Consumer<T> valueConsumer,
      BiConsumer<T, Throwable> errorConsumer) {
    while(!Thread.currentThread().isInterrupted()) {
      T value = valueSupplier.get();
      if(value != null) {
        try {
          valueConsumer.accept(value);
        } catch (Throwable t) {
          errorConsumer.accept(value, t);
        }
      } else {
        try {
          Thread.sleep(10);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
    }
  }
}

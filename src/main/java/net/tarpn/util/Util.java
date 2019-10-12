package net.tarpn.util;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class Util {

  public static String toHexDump(byte[] msg) {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      hexDump(msg, baos);
      baos.flush();
    } catch (IOException e) {
      // do nothing
    }
    byte[] output = baos.toByteArray();
    return new String(output, StandardCharsets.US_ASCII);
  }

  public static void hexDump(byte[] msg, OutputStream outputStream) throws IOException {
    int addr = 0;
    OutputStreamWriter writer = new OutputStreamWriter(outputStream);
    writer.write('\n');

    while (addr < msg.length) {
      StringBuilder sOffset = new StringBuilder(String.format("$%02x:", addr & 0xFF));
      StringBuilder text = new StringBuilder();
      for (int col = 0; col < 8; col++)
      {
        int val = msg[addr];
        sOffset.append(String.format(" %02x", val & 0xff));
        if (val < 0xFF) {
          char ch = (char)(val & 0xFF);
          if (Character.isLetterOrDigit(ch)) {
            text.append(ch);
          } else {
            text.append(".");
          }
        } else {
          text.append(".");
        }
        addr += 1;
        if (addr == msg.length) {
          break;
        }
      }
      writer.write(sOffset.toString());
      writer.write('\t');
      writer.write(text.toString());
      writer.write('\n');
    }
    writer.flush();
    outputStream.flush();
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

  public static String toHexString(int b) {
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

  public static <T> boolean queuePoll(
          Supplier<T> valueSupplier,
          Consumer<T> valueConsumer,
          BiConsumer<T, Throwable> errorConsumer) {
      T value = valueSupplier.get();
      if(value != null) {
        try {
          valueConsumer.accept(value);
          return true;
        } catch (Throwable t) {
          errorConsumer.accept(value, t);
          return true;
        }
      } else {
        return false;
      }
  }

  public static <T> void queueProcessingLoop(
      Supplier<T> valueSupplier,
      Consumer<T> valueConsumer,
      BiConsumer<T, Throwable> errorConsumer,
      Clock clock) {
    while(!Thread.currentThread().isInterrupted()) {
      boolean didPoll = queuePoll(valueSupplier, valueConsumer, errorConsumer);
      if (!didPoll) {
        try {
          clock.sleep(10);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
    }
  }

  public static void closeQuietly(Closeable closeable) {
    try {
      closeable.close();
    } catch (IOException e) {
      // do nothing
    }
  }
}


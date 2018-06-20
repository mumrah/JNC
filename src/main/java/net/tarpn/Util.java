package net.tarpn;

import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

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
}

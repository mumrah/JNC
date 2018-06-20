package net.tarpn;

import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Writer;

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
}

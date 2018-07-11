package net.tarpn;

/**
 * Collection of type safe functions for comparing bytes. Avoids issues with auto-boxing and
 * promotion to int
 */
public class ByteUtil {
  public static boolean lessThan(byte x, byte y) {
    return x < y;
  }

  public static boolean lessThanEq(byte x, byte y) {
    return x <= y;
  }

  public static boolean greaterThan(byte x, byte y) {
    return x > y;
  }

  public static boolean greaterThanEq(byte x, byte y) {
    return x > y;
  }

  public static boolean equals(byte x, byte y) {
    return x == y;
  }
}

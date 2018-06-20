package net.tarpn.packet.impl.ax25;

/**
 * The control byte for an I S or U frame
 */
public interface ControlByte {
  byte getMask();

  default byte asByte(boolean isPollOrFinal) {
    return (byte) (getMask() | (isPollOrFinal ? 0x10 : 0x00));
  }
}

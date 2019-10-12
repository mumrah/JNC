package net.tarpn.frame.impl;

public class KISS {

  interface KISSByte {
    int asInt();
    default byte asByte() {
      return (byte)asInt();
    }
  }

  public enum Protocol implements KISSByte {
    FEND(0xC0),
    FESC(0xDB),
    TFEND(0xDC),
    TFESC(0xDD);

    private final int value;

    Protocol(int value) {
      this.value = value;
    }

    @Override
    public int asInt() {
      return value;
    }

    public boolean equalsTo(int other) {
      return other == value;
    }

    public boolean equalsTo(byte other) {
      return (other & 0xFF) == (value & 0xFF);
    }
  }

  public enum Command implements KISSByte {

    Unknown(0xFE),
    Data(0x00),
    TxDelay(0x01),
    P(0x02),
    SlotTime(0x03),
    TxTail(0x04),
    FullDuplex(0x05),
    SetHardware(0x06),
    Return(0xFF);

    private final int value;

    Command(int value) {
      this.value = value;
    }

    @Override
    public int asInt() {
      return value;
    }

    public static Command fromInt(int value) {
      for(Command command : Command.values()) {
        if(command.asInt() == value) {
          return command;
        }
      }
      return Unknown;
    }
  }
}

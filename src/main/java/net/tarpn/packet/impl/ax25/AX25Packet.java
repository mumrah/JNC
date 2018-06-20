package net.tarpn.packet.impl.ax25;

import java.nio.charset.StandardCharsets;
import java.util.List;
import net.tarpn.packet.Packet;

public interface AX25Packet extends Packet {
  default String getDestination() {
    return getDestCall().toString();
  }

  default String getSource() {
    return getSourceCall().toString();
  }

  AX25Call getDestCall();
  AX25Call getSourceCall();

  default Command getCommand() {
    boolean destC = getDestCall().isCFlag();
    boolean sourceC = getSourceCall().isCFlag();
    if(destC) {
      if(sourceC) {
        return Command.LEGACY;
      } else {
        return Command.COMMAND;
      }
    } else {
      if(sourceC) {
        return Command.RESPONSE;
      } else {
        return Command.LEGACY;
      }
    }
  }

  List<AX25Call> getRepeaterPaths();
  byte getControlByte();
  FrameType getFrameType();

  interface SupervisoryFrame {
    boolean isPollOrFinalSet();
    byte getReceiveSequenceNumber();
    ControlType getControlType();

    enum ControlType {
      RR(0x01),
      RNR(0x05),
      REJ(0x09);

      private final byte nibble;

      ControlType(int nibble) {
        this.nibble = (byte)(nibble & 0x0F);
      }

      byte asByte(int nr, boolean isPollOrFinal) {
        return (byte) (nibble | (isPollOrFinal ? 0x10 : 0x00) | ((nr << 5) & 0xE0));
      }

      static ControlType fromControlByte(byte ctl) {
        for(ControlType type: ControlType.values()) {
          if((ctl & type.nibble) == type.nibble) {
            return type;
          }
        }
        throw new IllegalArgumentException("No known S Frame type for control " + Integer.toHexString(ctl & 0x0F));
      }
    }
  }

  interface UnnumberedFrame {
    boolean isPollFinalSet();
    ControlType getControlType();

    enum ControlType {
      SABM(0x2F), // Set Asynchronous Balanced Mode
      DISC(0x43), // Disconnect
      DM(0x0F),   // Disconnected Mode
      UA(0x63),   // Unnumbered Acknowledge
      FRMR(0x87), // Frame Reject
      UI(0x03);   // Unnumbered Information

      private final byte mask;

      ControlType(int mask) {
        this.mask = (byte)(mask & 0xFF);
      }

      public byte asByte(boolean isPollOrFinal) {
        return (byte) (mask | (isPollOrFinal ? 0x10 : 0x00));
      }

      public static ControlType fromControlByte(int ctl) {
        byte ctlByte = (byte)(ctl & 0xff);
        ctlByte = (byte)(ctlByte & ~(0x10)); // clear the p/f bit for enum
        for(ControlType type: ControlType.values()) {
          if(ctlByte == type.mask) {
            return type;
          }
        }
        throw new IllegalArgumentException("No known U Frame type for control " + Integer.toHexString(ctl & 0xFF));
      }
    }
  }

  interface InformationFrame extends HasInfo {
    boolean isPollBitSet();
    byte getReceiveSequenceNumber();
    byte getSendSequenceNumber();
  }

  interface HasInfo {
    byte[] getInfo();
    byte getProtocolByte();
    Protocol getProtocol();
    default String getInfoAsASCII() {
      return new String(getInfo(), StandardCharsets.US_ASCII)
          .replace("\r", "\\r")
          .replace("\n", "\\n")
          .replace("\t", "\\t")
          .replace("\b", "\\b")
          .replace("\f", "\\f");
    }
  }

  enum FrameType {
    I, S, U, UI;
  }

  enum Command {
    LEGACY,
    COMMAND,
    RESPONSE;

    void updateCalls(AX25Call dest, AX25Call source) {
      switch(this) {
        case LEGACY:
          break;
        case COMMAND:
          dest.setcFlag(true);
          source.setcFlag(false);
          break;
        case RESPONSE:
          dest.setcFlag(false);
          source.setcFlag(true);
          break;
      }
    }
  }

  enum Protocol {
    NETROM(0xCF),
    NO_LAYER3(0xF0);

    private final byte value;
    Protocol(int value) {
      this.value = (byte)(value & 0xFF);
    }

    public byte asByte() {
      return value;
    }

    public static Protocol valueOf(byte b) {
      for(Protocol protocol : Protocol.values()) {
        if(protocol.value == b) {
          return protocol;
        }
      }
      throw new IllegalArgumentException("Unknown Protocol type " + Integer.toHexString(b));
    }
  }
}

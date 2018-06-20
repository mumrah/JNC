package net.tarpn.frame;

import net.tarpn.packet.impl.ax25.AX25Packet.UnnumberedFrame.ControlType;
import org.junit.Assert;
import org.junit.Test;

public class ControlByteTest {

  @Test
  public void testUFrameControlBytes() {
    Assert.assertEquals(ControlType.fromControlByte(0x3f), ControlType.SABM);
    Assert.assertEquals(ControlType.fromControlByte(0x2f), ControlType.SABM);
    Assert.assertEquals(ControlType.fromControlByte(0x03), ControlType.UI);
    Assert.assertEquals(ControlType.fromControlByte(0x13), ControlType.UI);
    Assert.assertEquals(ControlType.fromControlByte(0x63), ControlType.UA);
    Assert.assertEquals(ControlType.fromControlByte(0x73), ControlType.UA);
    Assert.assertEquals(ControlType.fromControlByte(0x43), ControlType.DISC);
    Assert.assertEquals(ControlType.fromControlByte(0x53), ControlType.DISC);
    Assert.assertEquals(ControlType.fromControlByte(0x0F), ControlType.DM);
    Assert.assertEquals(ControlType.fromControlByte(0x1F), ControlType.DM);
    Assert.assertEquals(ControlType.fromControlByte(0x87), ControlType.FRMR);
    Assert.assertEquals(ControlType.fromControlByte(0x97), ControlType.FRMR);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testBadUFrameControl() {
    ControlType.fromControlByte(0xFE);
  }
}

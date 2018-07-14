package net.tarpn.frame;

import net.tarpn.packet.impl.ax25.AX25Call;
import net.tarpn.packet.impl.ax25.AX25State;
import net.tarpn.packet.impl.ax25.handlers.StateHelper;
import org.junit.Assert;
import org.junit.Test;

public class AX25StateTest {
  @Test
  public void testWindowSize() {
    AX25State state = new AX25State(
        "test",
        AX25Call.create("TEST", 0),
        AX25Call.create("TEST", 1),
        event -> {},
        linkPrimitive -> {});
    state.setAcknowledgeState((byte)0);
    state.incrementSendState();
    state.incrementSendState();
    state.incrementSendState();
    state.incrementSendState();
    state.incrementSendState();
    state.incrementSendState();
    Assert.assertFalse(state.windowExceeded());

    state.incrementSendState();
    Assert.assertTrue(state.windowExceeded());

    state.setAcknowledgeState((byte)7);
    Assert.assertFalse(state.windowExceeded());

    state.incrementSendState();
    state.incrementSendState();
    state.incrementSendState();
    state.incrementSendState();
    Assert.assertEquals(state.getSendState(), 11);
    Assert.assertEquals(state.getAcknowledgeState(), 7);
    Assert.assertFalse(state.windowExceeded());

    state.incrementSendState();
    state.incrementSendState();
    state.incrementSendState();
    Assert.assertEquals(state.getSendState(), 14);
    Assert.assertEquals(state.getAcknowledgeState(), 7);
    Assert.assertTrue(state.windowExceeded());
  }

  @Test
  public void testT1() {
    AX25State state = new AX25State(
        "test",
        AX25Call.create("TEST", 0),
        AX25Call.create("TEST", 1),
        event -> {},
        linkPrimitive -> {});

    state.resetRC();
    state.checkAndIncrementRC();
    state.checkAndIncrementRC();
    state.checkAndIncrementRC();
    state.checkAndIncrementRC();
    state.checkAndIncrementRC();


    System.err.println(state.getT1Timer().getTimeout());
    StateHelper.selectT1Value(state);
    System.err.println(state.getT1Timer().getTimeout());
    StateHelper.selectT1Value(state);
    System.err.println(state.getT1Timer().getTimeout());
    StateHelper.selectT1Value(state);
    System.err.println(state.getT1Timer().getTimeout());
  }
}

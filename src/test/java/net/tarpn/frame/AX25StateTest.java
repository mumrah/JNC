package net.tarpn.frame;

import net.tarpn.packet.impl.ax25.AX25Call;
import net.tarpn.packet.impl.ax25.AX25State;
import org.junit.Assert;
import org.junit.Test;

public class AX25StateTest {
  @Test
  public void testWindowSize() {
    AX25State state = new AX25State("test", AX25Call.create("TEST", 0), event -> {});
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
}

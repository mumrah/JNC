package net.tarpn.frame.impl;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import net.tarpn.frame.Frame;
import net.tarpn.frame.FrameHandler;
import net.tarpn.frame.FrameRequest;
import net.tarpn.frame.impl.KISS.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * KISS reference http://www.w1hkj.com/FldigiHelp/kiss_command_page.html
 */
public class KISSCommandHandler implements FrameHandler {

  private static final Logger LOG = LoggerFactory.getLogger(KISSCommandHandler.class);

  @Override
  public void onFrame(FrameRequest frameRequest) {
    Frame frame = frameRequest.getFrame();
    if(frame instanceof KISSFrame) {
      if(((KISSFrame) frame).getKissCommand() != Command.Data) {
        // only handle non-data commands here
        onKissCommand(((KISSFrame) frame), frameRequest);
        frameRequest.abort();
      }
    }
  }

  void onKissCommand(KISSFrame frame, FrameRequest frameRequest) {
    if(frame.getKissCommand() == Command.SetHardware) {
      String hwCmd = new String(frame.getData(), StandardCharsets.US_ASCII);
      final String resp;
      switch (hwCmd) {
        case "TNC:":
          resp = "TNC:JNC 1.0";
          break;
        case "MODEM:":
          resp = "MODEM:JNCModem";
          break;
        case "MODEML:":
          resp = "MODEML:JNCModem";
          break;
        case "MODEMBW:":
          resp = "MODEMBW:2000";
          break;
        default:
          resp = "";
          break;
      }

      if (resp.isEmpty()) {
        LOG.warn("Got unknown SET_HARDWARE request '" + hwCmd + "', ignoring");
      } else {
        LOG.info("Got SET_HARDWARE request '" + hwCmd + "', responding with '" + resp + "'");
        Frame response = new KISSFrame(
            frame.getPort(),
            Command.SetHardware,
            resp.getBytes(StandardCharsets.US_ASCII)
        );
        frameRequest.replyWith(response);
      }
    } else {
      LOG.info("Got KISS Command " + frame.getKissCommand().toString() + " -> " + (int)(frame.getData()[0]));
    }
  }
}

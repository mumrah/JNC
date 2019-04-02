package net.tarpn.app;

import net.tarpn.packet.PacketHandler;
import net.tarpn.packet.PacketRequest;
import net.tarpn.packet.impl.ax25.AX25Packet;
import net.tarpn.packet.impl.ax25.UIFrame;

import java.nio.charset.StandardCharsets;

public class ApplicationPacketHandlerAdaptor implements PacketHandler {
    private final Application<String> application;

    public ApplicationPacketHandlerAdaptor(Application<String> application) {
        this.application = application;
    }

    @Override
    public void onPacket(PacketRequest packetRequest) {
        if(packetRequest.getPacket() instanceof AX25Packet) {
            AX25Packet ax25Packet = (AX25Packet)packetRequest.getPacket();
            if(ax25Packet.getFrameType().equals(AX25Packet.FrameType.UI)) {
                UIFrame uiFrame = (UIFrame)ax25Packet;
                application.handle(uiFrame.getInfoAsASCII(), response -> {
                    packetRequest.replyWith(
                            UIFrame.create(ax25Packet.getSourceCall(), ax25Packet.getDestCall(),
                                    AX25Packet.Protocol.NO_LAYER3, response.getBytes(StandardCharsets.UTF_8)));
                }, () -> { /* TODO close */ });
            }
        }
    }
}

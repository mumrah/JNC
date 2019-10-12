package net.tarpn.netty;


import io.netty.util.AttributeKey;
import net.tarpn.packet.impl.ax25.AX25Call;

public class Attributes {
    public static final AttributeKey<Integer> PortNumber = AttributeKey.newInstance("PortNumber");
    public static final AttributeKey<AX25Call> NodeCall = AttributeKey.newInstance("NodeCall");
    public static final AttributeKey<String> NodeAlias = AttributeKey.newInstance("NodeAlias");


}

package net.tarpn.network.netrom;

public class NetRomCircuit {

  private final int circuitId;

  private State state;

  public NetRomCircuit(int circuitId) {
    this.circuitId = circuitId;
    this.state = State.DISCONNECTED;
  }

  public int getCircuitId() {
    return circuitId;
  }

  public State getState() {
    return state;
  }

  public void setState(State state) {
    this.state = state;
  }

  @Override
  public String toString() {
    return "NetRomState(" + circuitId + "){" +
        "state=" + state +
        '}';
  }

  public enum State {
    DISCONNECTED,
    AWAITING_CONNECTION,
    CONNECTED,
    AWAITING_RELEASE;
  }
}

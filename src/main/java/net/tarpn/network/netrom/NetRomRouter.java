package net.tarpn.network.netrom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import net.tarpn.packet.impl.ax25.AX25Call;

public class NetRomRouter {

  private final Map<AX25Call, Neighbor> neighbors;
  private final Map<AX25Call, Destination> destinations;

  public NetRomRouter() {
    this.neighbors = new HashMap<>();
    this.destinations = new HashMap<>();
  }

  public Map<AX25Call, Neighbor> getNeighbors() {
    return neighbors;
  }

  public Map<AX25Call, Destination> getDestinations() {
    return destinations;
  }

  @Override
  public String toString() {
    return "NetRomRouter{" +
        "neighbors=" + neighbors +
        ", destinations=" + destinations +
        '}';
  }

  /**
   * A neighboring node directly linked to this node
   */
  public static class Neighbor {
    private final AX25Call nodeCall;
    // TODO 2 Digitpeaters
    private final int port;
    private final int quality; // 0-255

    public Neighbor(AX25Call nodeCall, int port, int quality) {
      this.nodeCall = nodeCall;
      this.port = port;
      this.quality = quality;
    }

    public AX25Call getNodeCall() {
      return nodeCall;
    }

    public int getPort() {
      return port;
    }

    public int getQuality() {
      return quality;
    }

    @Override
    public String toString() {
      return "Neighbor{" +
          "nodeCall=" + nodeCall +
          ", port=" + port +
          ", quality=" + quality +
          '}';
    }
  }

  /**
   * Represents a "known" node in the network. These are reported by NODES
   */
  public static class Destination {
    private final AX25Call nodeCall;
    private final String nodeAlias;
    private final NavigableSet<DestinationRoute> neighbors;

    public Destination(AX25Call nodeCall, String nodeAlias) {
      this.nodeCall = nodeCall;
      this.nodeAlias = nodeAlias;
      this.neighbors = new TreeSet<>();
    }

    public AX25Call getNodeCall() {
      return nodeCall;
    }

    public String getNodeAlias() {
      return nodeAlias;
    }

    public Set<DestinationRoute> getNeighbors() {
      return neighbors.descendingSet();
    }

    /**
     * Remove all but the best routes for this destination
     * @param keep how many routes to keep
     */
    public void prune(int keep) {
      Set<DestinationRoute> keepers = neighbors.descendingSet()
          .stream()
          .limit(keep)
          .collect(Collectors.toSet());
      neighbors.clear();
      neighbors.addAll(keepers);
    }

    @Override
    public String toString() {
      return "Destination{" +
          "nodeCall=" + nodeCall +
          ", nodeAlias='" + nodeAlias + '\'' +
          ", neighbors=" + neighbors.descendingSet() +
          '}';
    }

    public static class DestinationRoute implements Comparable<DestinationRoute> {
      private final AX25Call neighbor;
      private final int quality; // 0-255, 255 best, 0 worst (never used)
      private int obsolescence; // 6

      public DestinationRoute(AX25Call neighbor, int quality) {
        this.neighbor = neighbor;
        this.quality = quality;
        this.obsolescence = 6;
      }

      public void decrementObsolescence() {
        obsolescence--; // TODO
      }

      public AX25Call getNeighbor() {
        return neighbor;
      }

      public int getQuality() {
        return quality;
      }

      public int getObsolescence() {
        return obsolescence;
      }

      @Override
      public String toString() {
        return "DestinationRoute{" +
            "neighbor=" + neighbor +
            ", quality=" + quality +
            ", obsolescence=" + obsolescence +
            '}';
      }

      @Override
      public int compareTo(DestinationRoute o) {
        return Integer.compare(this.getQuality(), o.getQuality());
      }
    }
  }
}

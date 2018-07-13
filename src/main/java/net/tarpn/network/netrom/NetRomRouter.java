package net.tarpn.network.netrom;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import net.tarpn.config.Configuration;
import net.tarpn.network.netrom.NetRomNodes.NodeDestination;
import net.tarpn.network.netrom.NetRomRouter.Destination.DestinationRoute;
import net.tarpn.packet.impl.ax25.AX25Call;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetRomRouter {

  private static final Logger LOG = LoggerFactory.getLogger(NetRomRouter.class);

  private final Configuration config;
  private final Map<AX25Call, Neighbor> neighbors;
  private final Map<AX25Call, Destination> destinations;

  public NetRomRouter(Configuration config) {
    this.config = config;
    this.neighbors = new HashMap<>();
    this.destinations = new HashMap<>();
  }

  public void updateNodes(AX25Call heardFrom, int heardOnPort, NetRomNodes nodes) {
    LOG.info("Got routing table from " + nodes.getSendingAlias());
    for(NetRomNodes.NodeDestination dest : nodes.getDestinationList()) {
      LOG.debug(dest.getDestNode() + "\t" + dest.getDestAlias() + "\t" + dest.getBestNeighborNode() + "\t" + Integer.toString((int)dest.getQuality() & 0xff));
    }

    Neighbor neighbor = neighbors.computeIfAbsent(heardFrom,
        call -> new Neighbor(call, heardOnPort, 255));

    Destination destination = destinations.computeIfAbsent(heardFrom,
        call -> new Destination(call, nodes.getSendingAlias())
    );

    // Add direct route to whoever send the NODES
    destination.getNeighbors().add(new DestinationRoute(heardFrom, 255));

    nodes.getDestinationList().stream()
        .filter(nodeDestination -> !nodeDestination.getDestNode().equals(config.getNodeCall()))
        .forEach(nodeDestination -> {
          final int routeQuality;
          if(nodeDestination.getBestNeighborNode().equals(config.getNodeCall())) {
            // Best neighbor is us, this is a "trivial loop", quality is zero
            routeQuality = 0;
          } else {
            int qualityProduct = nodeDestination.getQuality() * neighbor.getQuality();
            routeQuality = (qualityProduct + 128) / 256;
          }
          Destination neighborDest = destinations.computeIfAbsent(nodeDestination.getDestNode(),
              call -> new Destination(call, nodeDestination.getDestAlias())
          );
          neighborDest.getNeighbors().add(new DestinationRoute(neighbor.getNodeCall(), routeQuality));
        });
    LOG.info("New Routing table: " + this);
  }

  public NetRomNodes getNodes() {
    List<NodeDestination> destinations = new ArrayList<>();
    getDestinations().forEach((destCall, dest) -> {
      Optional<DestinationRoute> bestNeighbor = dest.getNeighbors().stream().findFirst();
      if(bestNeighbor.isPresent()) {
        // TODO configure thresholds
        if(bestNeighbor.get().getObsolescence() > 4 && bestNeighbor.get().getQuality() > 60) {
          new NodeDestination(dest.getNodeCall(), dest.getNodeAlias(), bestNeighbor.get().getNeighbor(), bestNeighbor.get().getQuality());
        }
      }
    });
    return new NetRomNodes(config.getAlias(), destinations);
  }

  /**
   * Return a list of potential neighbors to route a packet for this call to.
   * @param destCall
   * @return
   */
  public List<AX25Call> routePacket(AX25Call destCall) {
    Destination destination = destinations.get(destCall);
    if(destination != null) {
      LOG.info("Found routes to " + destCall + ": " + destination.getNeighbors());
      return destination.getNeighbors().stream()
          .map(DestinationRoute::getNeighbor)
          .collect(Collectors.toList());
      // loop through routes for this dest in order of quality
      // if there's an existing link to this neighbor, use it
      // if not, open a link and send the frame
    } else {
      return Collections.emptyList();
    }
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

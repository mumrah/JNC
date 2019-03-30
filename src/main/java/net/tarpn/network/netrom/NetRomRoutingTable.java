package net.tarpn.network.netrom;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.tarpn.config.NetRomConfig;
import net.tarpn.config.PortConfig;
import net.tarpn.network.netrom.NetRomNodes.NodeDestination;
import net.tarpn.network.netrom.NetRomRoutingTable.Destination.DestinationRoute;
import net.tarpn.packet.impl.ax25.AX25Call;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetRomRoutingTable {

  private static final Logger LOG = LoggerFactory.getLogger(NetRomRoutingTable.class);

  private final NetRomConfig config;
  private final Map<AX25Call, Neighbor> neighbors;
  private final Map<AX25Call, Destination> destinations;
  private final Function<Integer, PortConfig> portConfigGetter;

  public NetRomRoutingTable(NetRomConfig config, Function<Integer, PortConfig> portConfigGetter) {
    this.config = config;
    this.neighbors = new HashMap<>();
    this.destinations = new HashMap<>();
    this.portConfigGetter = portConfigGetter;
  }

  /**
   * Process an incoming NODES payload and update our routing table
   */
  public void updateNodes(AX25Call heardFrom, int heardOnPort, NetRomNodes nodes) {
    LOG.info("Got routing table from " + nodes.getSendingAlias());

    PortConfig portConfig = portConfigGetter.apply(heardOnPort);
    int defaultQuality = portConfig.getInt("port.quality", 255);
    int defaultObs = config.getInitialObs();

    Neighbor neighbor = neighbors.computeIfAbsent(heardFrom,
        call -> new Neighbor(call, heardOnPort, defaultQuality));

    Destination destination = destinations.computeIfAbsent(heardFrom,
        call -> new Destination(call, nodes.getSendingAlias())
    );

    // Add direct route to whoever send the NODES
    destination.getNeighborMap().put(heardFrom,
        new DestinationRoute(heardFrom, heardFrom, defaultQuality, defaultObs));

    // Add destination routes for every node we learned about
    nodes.getDestinationList().stream()
        .filter(nodeDestination -> !nodeDestination.getDestNode().equals(config.getNodeCall()))
        .forEach(nodeDestination -> {
          final int routeQuality;
          if(nodeDestination.getBestNeighborNode().equals(config.getNodeCall())) {
            // Best neighbor is us, this is a "trivial loop", quality is zero
            routeQuality = 0;
          } else {
            // Otherwise compute this route's quality based on the spec
            int qualityProduct = nodeDestination.getQuality() * neighbor.getQuality();
            routeQuality = (qualityProduct + 128) / 256;
          }

          // Only add high quality routes to our routing table
          if(routeQuality > config.getInt("netrom.nodes.quality.min", 0)) {
            Destination neighborDest = destinations.computeIfAbsent(nodeDestination.getDestNode(),
                call -> new Destination(call, nodeDestination.getDestAlias())
            );
            DestinationRoute destRoute = neighborDest.getNeighborMap().computeIfAbsent(neighbor.getNodeCall(),
                newCall -> new DestinationRoute(nodeDestination.getDestNode(), neighbor.getNodeCall(), routeQuality, defaultObs));
            destRoute.setObsolescence(defaultObs);
            destRoute.setQuality(routeQuality);
          } else {
            if(routeQuality > 0) {
              LOG.warn(
                  "Learned about " + neighbor.getNodeCall() + ", but quality was too low to add "
                      + "to our routing table (" + routeQuality + ")");
            }
          }
        });
    LOG.info("New Routing table: " + this);
  }

  /**
   * Decrement the obsolescence count for each route and remove those which have reached zero. Also
   * remove any neighbors which no longer have any routes.
   */
  public void pruneRoutes() {
    destinations.forEach(((ax25Call, destination) -> {
      destination.getNeighborMap().values().forEach(DestinationRoute::decrementObsolescence);
      destination.getNeighborMap().entrySet().removeIf(entry -> entry.getValue().getObsolescence() <= 0);
    }));

    Set<AX25Call> noRoutes = destinations.entrySet()
        .stream()
        .filter(entry -> entry.getValue().getSortedNeighbors().isEmpty())
        .map(Entry::getKey)
        .collect(Collectors.toSet());

    noRoutes.forEach(ax25Call -> {
      LOG.info(ax25Call + " went away");
      destinations.remove(ax25Call);
      neighbors.remove(ax25Call);
    });
  }

  /**
   * Convert our routing table to a NODES payload, only including routes whose obsolescence count
   * is above "netrom.obs.min"
   * @return
   */
  public NetRomNodes getNodes() {
    List<NodeDestination> destinations = new ArrayList<>();
    getDestinations().forEach((destCall, dest) -> {
      dest.getSortedNeighbors()
          .stream()
          .filter(neighbor -> neighbor.getObsolescence() >= config.getMinObs())
          .findFirst()
          .ifPresent(bestNeighbor -> destinations.add(
              new NodeDestination(dest.getNodeCall(), dest.getNodeAlias(),
                  bestNeighbor.getNextHop(), bestNeighbor.getQuality())));
    });
    return new NetRomNodes(config.getNodeAlias(), destinations);
  }

  /**
   * Return a list of potential neighbors to route a packet for this call to.
   * @param destCall
   * @return
   */
  public List<AX25Call> routePacket(AX25Call destCall) {
    Destination destination = destinations.get(destCall);
    if(destination != null) {
      List<AX25Call> routes = destination.getSortedNeighbors()
          .stream()
          .map(DestinationRoute::getNextHop)
          .collect(Collectors.toList());
      LOG.info("Found routes to " + destCall + ": " + routes);
      return routes;
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

  public String toPrettyString() {
    StringBuilder builder = new StringBuilder("NET/ROM routing table:\n");
    destinations.forEach((ax25Call, destination) -> {
      builder.append(destination).append("\n");
    });
    return builder.toString();
  }

  /**
   * A neighboring node directly linked to this node, it has a port and a quality determined by the
   * "port.quality" configuration
   */
  public static class Neighbor {
    private final AX25Call nodeCall;
    // TODO 2 Digipeaters
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
   * Represents a "known" node in the network. Each destination includes up to three routes to reach
   * it. A route might be direct in the case of a neighbor, or it might be through a sequence of
   * other nodes. In the latter case, we don't care about the whole sequence just the next hop.
   */
  public static class Destination {
    private final AX25Call nodeCall;
    private final String nodeAlias;
    private final Map<AX25Call, DestinationRoute> neighborMap;

    public Destination(AX25Call nodeCall, String nodeAlias) {
      this.nodeCall = nodeCall;
      this.nodeAlias = nodeAlias;
      this.neighborMap = new HashMap<>();
    }

    public AX25Call getNodeCall() {
      return nodeCall;
    }

    public String getNodeAlias() {
      return nodeAlias;
    }

    public Map<AX25Call, DestinationRoute> getNeighborMap() {
      return neighborMap;
    }

    public List<DestinationRoute> getSortedNeighbors() {
      return neighborMap.values()
          .stream()
          .sorted(Comparator.comparingInt(DestinationRoute::getQuality))
          .collect(Collectors.toList());
    }

    @Override
    public String toString() {
      return "Destination{" +
          "nodeCall=" + nodeCall +
          ", nodeAlias='" + nodeAlias + '\'' +
          ", neighborMap=" + getSortedNeighbors() +
          '}';
    }

    /**
     * The next hop in a route to reach a destination. Includes quality which is computed based on
     * the quality of the neighbor we learned about this route from, as well as an "obsolescence"
     * count which is periodically decremented.
     */
    public static class DestinationRoute {
      private final AX25Call destination;
      private final AX25Call nextHop;
      private int quality; // 0-255, 255 best, 0 worst (never used)
      private int obsolescence;

      public DestinationRoute(AX25Call destination, AX25Call nextHop, int quality, int obsolescence) {
        this.destination = destination;
        this.nextHop = nextHop;
        this.quality = quality;
        this.obsolescence = obsolescence;
      }

      public AX25Call getDestination() {
        return destination;
      }

      public AX25Call getNextHop() {
        return nextHop;
      }

      public int getQuality() {
        return quality;
      }

      public void setQuality(int newQuality) {
        this.quality = newQuality;
      }

      public void decrementObsolescence() {
        obsolescence--;
      }

      public void setObsolescence(int newObsolescence) {
        this.obsolescence = newObsolescence;
      }

      public int getObsolescence() {
        return obsolescence;
      }

      @Override
      public String toString() {
        return "DestinationRoute{" +
            "neighbor=" + nextHop +
            ", quality=" + quality +
            ", obsolescence=" + obsolescence +
            '}';
      }

      @Override
      public boolean equals(Object o) {
        if (this == o) {
          return true;
        }
        if (!(o instanceof DestinationRoute)) {
          return false;
        }
        DestinationRoute that = (DestinationRoute) o;
        return Objects.equals(getDestination(), that.getDestination()) &&
            Objects.equals(getNextHop(), that.getNextHop());
      }

      @Override
      public int hashCode() {
        return Objects.hash(getDestination(), getNextHop());
      }
    }
  }
}

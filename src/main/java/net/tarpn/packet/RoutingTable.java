package net.tarpn.packet;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class RoutingTable {

  private final List<RouteEntry> routeEntryList = new ArrayList<>();

  public void addRouteEntry(String address, String port, int quality) {
    routeEntryList.add(new RouteEntry(address, port, quality));
  }

  public Optional<RouteEntry> getEntryForAddress(String address) {
    return routeEntryList.stream()
        .filter(entry -> entry.getAddress().equalsIgnoreCase(address))
        .max(Comparator.comparing(RouteEntry::getQuality));
  }

  public List<RouteEntry> getAddressesForPort(String port) {
    return routeEntryList.stream()
        .filter(entry -> entry.getPort().equalsIgnoreCase(port))
        .collect(Collectors.toList());
  }

  public static class RouteEntry {
    private final String address;
    private final String port;
    private final int quality;

    public RouteEntry(String address, String port, int quality) {
      this.address = address;
      this.port = port;
      this.quality = quality;
    }

    public String getAddress() {
      return address;
    }

    public String getPort() {
      return port;
    }

    public int getQuality() {
      return quality;
    }
  }

}

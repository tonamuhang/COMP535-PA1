package socs.network.node;

public class Link {

  RouterDescription router1;
  RouterDescription router2;
  short weight=0;

  public Link(RouterDescription r1, RouterDescription r2) {
    router1 = r1;
    router2 = r2;
  }

  public Link(RouterDescription r1, RouterDescription r2, short weight) {
    router1 = r1;
    router2 = r2;
    this.weight = weight;
  }

  public boolean equals(Link link) {
    return router2.simulatedIPAddress.equals(link.router2.simulatedIPAddress);
  }
}

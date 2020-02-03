package socs.network.node;

import com.sun.xml.internal.bind.v2.TODO;
import socs.network.message.SOSPFPacket;
import socs.network.util.Configuration;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;


public class Router {

  protected LinkStateDatabase lsd;

  RouterDescription rd = new RouterDescription();

  //assuming that all routers are with 4 ports
  Link[] ports = new Link[4];

  public Router(Configuration config) {
    rd.simulatedIPAddress = config.getString("socs.network.router.ip");
    rd.processPortNumber = config.getShort("socs.network.router.port");

    lsd = new LinkStateDatabase(rd);
  }

  /**
   * output the shortest path to the given destination ip
   * <p/>
   * format: source ip address  -> ip address -> ... -> destination ip
   *
   * @param destinationIP the ip adderss of the destination simulated router
   */
  private void processDetect(String destinationIP) {

  }

  /**
   * disconnect with the router identified by the given destination ip address
   * Notice: this command should trigger the synchronization of database
   *
   * @param portNumber the port number which the link attaches at
   */
  private void processDisconnect(short portNumber) {

  }

  /**
   * attach the link to the remote router, which is identified by the given simulated ip;
   * to establish the connection via socket, you need to indentify the process IP and process Port;
   * additionally, weight is the cost to transmitting data through the link
   * <p/>
   * NOTE: this command should not trigger link database synchronization
   */
  // TODO: Attach
  private void processAttach(String processIP, short processPort,
                             String simulatedIP, short weight) {

    RouterDescription remote = new RouterDescription(processIP, processPort, simulatedIP);


    // Identify the router with simulated ip in the simulated network space only
    if(simulatedIP.equals(this.rd.simulatedIPAddress)){
      System.out.println("Router can't attach to itself");
      System.exit(1);
    }

    // Check if the link is already established.
    for(Link port : this.ports){
      if(port != null){
        if(port.router2.simulatedIPAddress.equals(simulatedIP)){
          System.out.println("Link already established");
          return;
        }
      }
    }

    // Create the attachment
    for(int i = 0; i < 4; i++){
      if(this.ports[i] == null){
        this.ports[i] = new Link(this.rd, remote);
        return;
      }
    }

    System.out.println("Attach failed :Ports are all taken");
  }

  /**
   * broadcast Hello to neighbors
   */
  // TODO: Start
  private void processStart() {

      System.out.println("START");
      if(ports.length == 0){
        System.out.println("No routers connected");
        return;
      }

      Socket client = null;

      // Try to create a socket for every connected router
      for(Link link : ports){
        try {
          client = new Socket(link.router2.processIPAddress, link.router2.processPortNumber);
        }
        catch (Exception e){
          System.out.println("oh noo");
        }
      }

      SOSPFPacket packet = new SOSPFPacket();

  }

  /**
   * attach the link to the remote router, which is identified by the given simulated ip;
   * to establish the connection via socket, you need to indentify the process IP and process Port;
   * additionally, weight is the cost to transmitting data through the link
   * <p/>
   * This command does trigger the link database synchronization
   */
  private void processConnect(String processIP, short processPort,
                              String simulatedIP, short weight) {

  }

  /**
   * output the neighbors of the routers
   */
  // TODO: Neighbors
  private void processNeighbors() {
    int i = 1;
    for(Link link : ports){
      if(link.router2.status == RouterStatus.TWO_WAY){
        System.out.println("IP Address of the neighbor" + i + link.router2.simulatedIPAddress);
        i++;
      }
    }
  }

  /**
   * disconnect with all neighbors and quit the program
   */
  private void processQuit() {

  }

  public void terminal() {
    try {
      InputStreamReader isReader = new InputStreamReader(System.in);
      BufferedReader br = new BufferedReader(isReader);
      System.out.print(">> ");
      String command = br.readLine();
      while (true) {
        if (command.startsWith("detect ")) {
          String[] cmdLine = command.split(" ");
          processDetect(cmdLine[1]);
        } else if (command.startsWith("disconnect ")) {
          String[] cmdLine = command.split(" ");
          processDisconnect(Short.parseShort(cmdLine[1]));
        } else if (command.startsWith("quit")) {
          processQuit();
        } else if (command.startsWith("attach ")) {
          String[] cmdLine = command.split(" ");
          processAttach(cmdLine[1], Short.parseShort(cmdLine[2]),
                  cmdLine[3], Short.parseShort(cmdLine[4]));
        } else if (command.equals("start")) {
          processStart();
        } else if (command.equals("connect ")) {
          String[] cmdLine = command.split(" ");
          processConnect(cmdLine[1], Short.parseShort(cmdLine[2]),
                  cmdLine[3], Short.parseShort(cmdLine[4]));
        } else if (command.equals("neighbors")) {
          //output neighbors
          processNeighbors();
        } else {
          //invalid command
          break;
        }
        System.out.print(">> ");
        command = br.readLine();
      }
      isReader.close();
      br.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

}

package socs.network.node;

import socs.network.message.LSA;
import socs.network.message.LinkDescription;
import socs.network.message.SOSPFPacket;
import socs.network.util.Configuration;

import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.Map;

public class Router {

  RouterDescription rd = new RouterDescription();

  //assuming that all routers are with 4 ports
  Link[] ports = new Link[4];
  Socket[] clientSocket = new Socket[4];
  private Server server;
  LinkStateDatabase lsd ;

  public Router(Configuration config) {
    rd.simulatedIPAddress = config.getString("socs.network.router.ip");
    String port = config.getString("socs.network.router.port");
    short p = Short.valueOf(port);
    rd.processPortNumber = p;
    rd.processIPAddress = "localhost";

    lsd = new LinkStateDatabase(rd);

    System.out.println(p);
    server = new Server(this);
    new Thread(server).start();





  }

  /**
   * output the shortest path to the given destination ip
   * <p/>
   * format: source ip address  -> ip address -> ... -> destination ip
   *
   * @param destinationIP the ip adderss of the destination simulated router
   */
  private void processDetect(String destinationIP) {
    if (destinationIP.equals(rd.simulatedIPAddress)) {
      System.out.println("Cannot detect to yourself!");
    }else{
      lsd.getShortestPath(destinationIP);
    }
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
      return;
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
    Link link = new Link(this.rd, remote, weight);

    if(this.addLink(link) == 0) {
      System.out.println("Attached to router " + link.router2.simulatedIPAddress);
    }

  }

  /**
   * broadcast Hello to neighbors
   */
  // TODO: Start
  private void processStart() {


    System.out.println("START");
    if (ports.length == 0) {
      System.out.println("No routers connected");
      return;
    }

    for(int i = 0; i < this.ports.length; i++){
      Link link = this.ports[i];
      if(link != null && link.router2.status != RouterStatus.TWO_WAY){
        SOSPFPacket packet = new SOSPFPacket(link.router1.processIPAddress, link.router1.processPortNumber,
                link.router1.simulatedIPAddress, link.router2.simulatedIPAddress, (short) 0,
                "", "", null, link.weight);

        try{
          Socket client = new Socket(link.router2.processIPAddress, link.router2.processPortNumber);
          clientSocket[i] = client;
          ObjectOutputStream output = new ObjectOutputStream(client.getOutputStream());
          ObjectInputStream input = new ObjectInputStream(client.getInputStream());

          // Send hello packet to the attached router
          output.writeObject(packet);

          // Blocked until the router receive a packet back from the connected router
          SOSPFPacket received = (SOSPFPacket)input.readObject();

          // Check if the received packet is a hello packet
          if(received.sospfType == 0){
            System.out.println("received HELLO from " + received.srcIP + ";");
            link.router2.status = RouterStatus.TWO_WAY;
            System.out.println("set " + received.srcIP + " state to TWO_WAY;");



          }
          // Acknowledge the packet is received
          output.writeObject(packet);

          // Update lsa
          lsaUpdate(this.ports[i].router2.simulatedIPAddress, this.ports[i].router2.processPortNumber,
                  this.ports[i].weight);


          input.close();
          output.close();

        }
        catch (Exception e){
          System.out.println(e);
        }
      }
    }


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
      if(link != null && link.router2.status == RouterStatus.TWO_WAY){
        System.out.println("IP Address of the neighbor " + i + "\n" + link.router2.simulatedIPAddress);
        i++;
      }
    }
  }

  /**
   * disconnect with all neighbors and quit the program
   */
  private void processQuit() {
    System.out.println(lsd.toString());
  }



  public void broadcastLSP(Vector<LSA> lsaVector){
    for(Link link : this.ports){
      if(link == null){
        continue;
      }
      if(link.router2.status == RouterStatus.TWO_WAY){
        SOSPFPacket packet = new SOSPFPacket(link.router1.processIPAddress, link.router1.processPortNumber,
                link.router1.simulatedIPAddress, link.router2.simulatedIPAddress, (short) 1, "", "",
                lsaVector, link.weight);

        sendPacket(link.router2, packet);

      }


    }
  }


  public void sendPacket(RouterDescription rd, SOSPFPacket packet){
    try{
      Socket client = new Socket(rd.processIPAddress, rd.processPortNumber);
      ObjectOutputStream out = new ObjectOutputStream(client.getOutputStream());

      out.writeObject(packet);
      out.close();
    }
    catch (Exception e){
      e.printStackTrace();
    }
  }

  public void forwardPacket(SOSPFPacket packet){
    for(Link link : this.ports){
      if(link == null){
        continue;
      }
      if(link.router2.status == RouterStatus.TWO_WAY && !link.router2.simulatedIPAddress.equals(packet.srcIP)){
        SOSPFPacket packet1 = new SOSPFPacket(link.router1.processIPAddress,
                link.router1.processPortNumber, link.router1.simulatedIPAddress,
                link.router2.simulatedIPAddress, (short) 1, "", "", packet.lsaArray,
                link.weight);

        sendPacket(link.router2, packet1);
      }
    }
  }

  public void lsaUpdate(String linkID, short portNum, short weight){
    LinkDescription ld = new LinkDescription();
    ld.linkID = linkID;
    ld.portNum = portNum;
    ld.tosMetrics = weight;

    LSA lsa = lsd._store.get(this.rd.simulatedIPAddress);
    lsa.links.add(ld);
    lsa.lsaSeqNumber ++;

    Vector<LSA> lsaVector = new Vector<LSA>();

    for(Map.Entry<String, LSA> s : this.lsd._store.entrySet()){
      lsaVector.add(s.getValue());
    }


    broadcastLSP(lsaVector);

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

  /**
   * Add a link to this Router's port list
   * @param link
   */
  public synchronized int addLink(Link link){

    if(link != null && !linkExist(link)){
      for(int i = 0; i < this.ports.length; i++){
        if(this.ports[i] == null){
          ports[i] = link;
          return 0;
        }
        else if(i == 3){
          System.out.println("Attach failed :Ports are all taken");
        }
      }




    }
    return -1;
  }

  /**
   * CHeck if a given link exists in the list of ports
   * @param link
   * @return True if exists, false if not
   */
  public synchronized boolean linkExist(Link link){
    for(Link l : this.ports){
      if(l != null && l.equals(link)){
        return true;
      }
    }

    return false;
  }

}
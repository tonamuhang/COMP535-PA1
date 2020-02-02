package socs.network.node;

import socs.network.message.SOSPFPacket;
import socs.network.util.Configuration;

import java.io.*;
import java.net.Socket;


public class Router {

    protected LinkStateDatabase lsd;

    RouterDescription rd = new RouterDescription();

    //assuming that all routers are with 4 ports
    Link[] ports = new Link[4];
    Socket[] clientSocket = new Socket[4];
    private Server server;

    public Router(Configuration config) {
        rd.simulatedIPAddress = config.getString("socs.network.router.ip");
        rd.processPortNumber = config.getShort("socs.network.router.port");
        rd.processIPAddress = "localhost";

        server = new Server(this);
        new Thread(server).start();

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
        if (ports.length == 0) {
            System.out.println("No routers connected");
            return;
        }

        for (int i = 0; i < 4; i++) {
            if (null == ports[i])
                continue;
            // if already neighbors, no need to send hello again
            if (ports[i].router2.status == RouterStatus.TWO_WAY)
                continue;
            // create a hello packet
            SOSPFPacket packet = new SOSPFPacket(ports[i].router1.processIPAddress, ports[i].router1.processPortNumber,
                    ports[i].router1.simulatedIPAddress, ports[i].router2.simulatedIPAddress, (short) 0,
                    "", "", null);
            try {
                // create a client socket
                clientSocket[i] = new Socket(ports[i].router2.processIPAddress, ports[i].router2.processPortNumber);
                ObjectOutputStream out = new ObjectOutputStream(clientSocket[i].getOutputStream());
                // send first hello packet
                out.writeObject(packet);
                ObjectInputStream in = new ObjectInputStream(clientSocket[i].getInputStream());
                // blocking operation
                SOSPFPacket received = (SOSPFPacket) in.readObject();
                if (received.sospfType == 0) {
                    System.out.println("received HELLO from " + received.srcIP + ";");
                    ports[i].router2.status = RouterStatus.TWO_WAY;
                    System.out.println("set " + received.srcIP + " state to TWO_WAY;");
                } else {
                    System.err.println("Error in received packet!");
                }
                // send second hello packet
                out.writeObject(packet);
                in.close();
                out.close();
            } catch (Exception e) {
                if (e instanceof IOException)
                    System.err.println("Port cannot be used");
                else if (e instanceof ClassNotFoundException)
                    System.err.println("In stream object class not found");
                else
                    System.err.println(e.getMessage());
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

package socs.network.node;

import socs.network.message.LSA;
import socs.network.message.SOSPFPacket;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

public class Server implements Runnable{
    private ServerSocket serverSocket;
    private Router router;
    private boolean on = true;

    /**
     * Constructor for Server class
     * @param router the router instance which has the server running
     */
    public Server(Router router){
        this.router = router;
        try {
//            this.serverSocket = new ServerSocket(router.rd.processPortNumber);
            this.serverSocket = new ServerSocket();
            serverSocket.bind(new InetSocketAddress(router.rd.processIPAddress, router.rd.processPortNumber));
        } catch (IOException e) {
            System.out.println("Port cannot be used");
        }
    }

    public void run() {
        while (on) {
            try {
//                System.out.println("A");
                Socket clientSocket = serverSocket.accept();
                ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
                ObjectOutputStream out= new ObjectOutputStream(clientSocket.getOutputStream());

                try {
                    SOSPFPacket received = (SOSPFPacket) in.readObject();
                    // hello packet
//                    if (received.sospfType == 0) {
                    System.out.println("");
                    System.out.println("received HELLO from " + received.srcIP + ";");

                    // check if the target router has already been attached
                    for (int i = 0; i < 4; i++) {
                        if (null != router.ports[i] && router.ports[i].router2.simulatedIPAddress.equals(received.dstIP)) {
                            System.err.println("This router has already been attached!");
                            return;
                        }
                    }
                    // create a RouterDescription for the remote router
                    RouterDescription remote_rd = new RouterDescription(received.srcProcessIP,
                            received.srcProcessPort, received.srcIP);
                    // create a link of these two routers
                    Link link = new Link(router.rd, remote_rd);
                    // put it into ports[]
                    int i;
                    for (i = 0; i < 4; i++) {
                        if (null == router.ports[i] || router.ports[i].router2.simulatedIPAddress.equals(received.srcIP)) {
                            router.ports[i] = link;
                            router.ports[i].router2.status = RouterStatus.INIT;
                            break;
                        }
                    }
                    // no more free port
                    if (i == 4) {
                        System.err.println("All ports are occupied, link cannot be established.");
                        return;
                    }
                    System.out.println("set " + received.srcIP + " state to INIT;");
                    SOSPFPacket sent = new SOSPFPacket(router.rd.processIPAddress, router.rd.processPortNumber,
                            router.rd.simulatedIPAddress, received.srcIP, (short) 0,
                            "", "", null);
                    out.writeObject(sent);
                    received = (SOSPFPacket) in.readObject();
                    if (received.sospfType == 0) {
                        System.out.println("received HELLO from " + received.srcIP + ";");
                        router.ports[i].router2.status = RouterStatus.TWO_WAY;
                        System.out.println("set " + received.srcIP + " state to TWO_WAY;");
                    } else {
                        // TODO: Expecting another HELLO!
                        System.err.println("Error in received packet!");
                    }
//                }
                    in.close();
                    out.close();
                } catch (ClassNotFoundException cnfe) {
                    System.out.println("Object class not found");
                }

            } catch (Exception e) {
                if (e instanceof IOException)
                    System.out.println("Fail to accept");
            }
        }

        try {
            serverSocket.close();
            System.out.println("Server Stopped");
        } catch (IOException ioe) {
            System.out.println("Error Found stopping server socket");
            System.exit(-1);
        }
    }

    class ClientServiceThread extends Thread {
        private Socket clientSocket;
        private ObjectInputStream in;
        private ObjectOutputStream out;

        /**
         * Constructor for ClientServiceThread
         * @param s the client socket to listen
         */
        public ClientServiceThread(Socket s) {
            super();
            clientSocket = s;
        }



        /**
         * Method invoked by Thread.start()
         */
        @Override
        public void run() {
            try {
                System.out.println("B");
                in = new ObjectInputStream(clientSocket.getInputStream());
                out = new ObjectOutputStream(clientSocket.getOutputStream());
                try {
                    SOSPFPacket received = (SOSPFPacket) in.readObject();
                    // hello packet
//                    if (received.sospfType == 0) {
                    System.out.println("");
                    System.out.println("received HELLO from " + received.srcIP + ";");

                    // check if the target router has already been attached
                    for (int i = 0; i < 4; i++) {
                        if (null != router.ports[i] && router.ports[i].router2.simulatedIPAddress.equals(received.dstIP)) {
                            System.err.println("This router has already been attached!");
                            return;
                        }
                    }
                    // create a RouterDescription for the remote router
                    RouterDescription remote_rd = new RouterDescription(received.srcProcessIP,
                            received.srcProcessPort, received.srcIP);
                    // create a link of these two routers
                    Link link = new Link(router.rd, remote_rd);
                    // put it into ports[]
                    int i;
                    for (i = 0; i < 4; i++) {
                        if (null == router.ports[i] || router.ports[i].router2.simulatedIPAddress.equals(received.srcIP)) {
                            router.ports[i] = link;
                            router.ports[i].router2.status = RouterStatus.INIT;
                            break;
                        }
                    }
                    // no more free port
                    if (i == 4) {
                        System.err.println("All ports are occupied, link cannot be established.");
                        return;
                    }
                    System.out.println("set " + received.srcIP + " state to INIT;");
                    SOSPFPacket sent = new SOSPFPacket(router.rd.processIPAddress, router.rd.processPortNumber,
                            router.rd.simulatedIPAddress, received.srcIP, (short) 0,
                            "", "", null);
                    out.writeObject(sent);
                    received = (SOSPFPacket) in.readObject();
                    if (received.sospfType == 0) {
                        System.out.println("received HELLO from " + received.srcIP + ";");
                        router.ports[i].router2.status = RouterStatus.TWO_WAY;
                        System.out.println("set " + received.srcIP + " state to TWO_WAY;");
                    } else {
                        // TODO: Expecting another HELLO!
                        System.err.println("Error in received packet!");
                    }
//                }
                    in.close();
                    out.close();
                } catch (ClassNotFoundException cnfe) {
                    System.out.println("Object class not found");
                }
            } catch (IOException ioe) {
                System.out.println("client socket streaming failed");
            }


        }
    }
}

package socs.network.node;

import socs.network.message.SOSPFPacket;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class Server implements Runnable {
    private ServerSocket serverSocket;
    private Router router;
    private boolean on = true;

    /**
     * Constructor for Server class
     *
     * @param router the router instance which has the server running
     */
    public Server(Router router) {
        this.router = router;
        try {
            this.serverSocket = new ServerSocket();
            serverSocket.bind(new InetSocketAddress(router.rd.processIPAddress, router.rd.processPortNumber));
        } catch (IOException e) {
            System.out.println("Port cannot be used");
            return;
        }
    }

    public void run() {
        while (on) {
            try {
                Socket socket = serverSocket.accept();
                ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());
                ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
                try {
                    SOSPFPacket receivedPacket = (SOSPFPacket) inputStream.readObject();
                    // hello packet
                    System.out.println("");
                    System.out.println("received HELLO from " + receivedPacket.srcIP + ";");

                    // check if the target router has already been attached
                    for (Link port : router.ports) {
                        if (null != port && port.router2.simulatedIPAddress.equals(receivedPacket.dstIP)) {
                            System.err.println("This link cannot be attached again!");
                            return;
                        }
                    }
                    // create a RouterDescription for the remote router
                    RouterDescription remote_rd = new RouterDescription(receivedPacket.srcProcessIP,
                            receivedPacket.srcProcessPort, receivedPacket.srcIP);
                    // create a link of these two routers
                    Link link = new Link(router.rd, remote_rd);
                    // put it into ports[]
                    int i;
                    for (i = 0; i < 4; i++) {
                        if (null == router.ports[i] || router.ports[i].router2.simulatedIPAddress.equals(receivedPacket.srcIP)) {
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
                    System.out.println("set " + receivedPacket.srcIP + " state to INIT;");
                    SOSPFPacket responsePacket = new SOSPFPacket(router.rd.processIPAddress, router.rd.processPortNumber,
                            router.rd.simulatedIPAddress, receivedPacket.srcIP, (short) 0,
                            "", "", null);
                    outputStream.writeObject(responsePacket);
                    receivedPacket = (SOSPFPacket) inputStream.readObject();
                    if (receivedPacket.sospfType == 0) {
                        System.out.println("received HELLO from " + receivedPacket.srcIP + ";");
                        router.ports[i].router2.status = RouterStatus.TWO_WAY;
                        System.out.println("set " + receivedPacket.srcIP + " state to TWO_WAY;");
                    } else {
                        // TODO: Expecting another HELLO!
                        System.err.println("Error inputStream received packet!");
                    }
                    inputStream.close();
                    outputStream.close();
                } catch (ClassNotFoundException cnfe) {
                    System.out.println("Object class not found");
                }

            } catch (Exception e) {
                System.out.println("Cannot accept");
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

}

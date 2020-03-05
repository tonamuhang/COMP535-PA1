package socs.network.node;

import socs.network.message.LSA;
import socs.network.message.SOSPFPacket;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;

public class Server implements Runnable {
    private ServerSocket serverSocket;
    private Router router;
    private boolean on = true;
    private SOSPFPacket received = null;
    ObjectInputStream inputStream = null;
    ObjectOutputStream outputStream = null;

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
                Socket client = serverSocket.accept();
                this.inputStream = new ObjectInputStream(client.getInputStream());
                this.outputStream = new ObjectOutputStream(client.getOutputStream());

                // Blocked until received packet from the other router
                this.received = (SOSPFPacket)this.inputStream.readObject();
                if(this.received.sospfType == 0){
                    setInit();
                    this.received = (SOSPFPacket)this.inputStream.readObject();
                    if(this.received.sospfType == 0) {
                        setTwoway();
                    }

                    this.router.lsaUpdate(this.received.srcIP,
                            this.received.srcProcessPort, this.received.weight);
                }
                else if(this.received.sospfType == 1){
                    Vector<LSA> lsaVector = this.received.lsaArray;


                    for(LSA lsa : lsaVector){
                        if(this.router.lsd._store.containsKey(lsa.linkStateID)){
                            if(lsa.lsaSeqNumber > router.lsd._store.get(lsa.linkStateID).lsaSeqNumber){
                                this.router.lsd._store.put(lsa.linkStateID, lsa);
                                this.router.forwardPacket(this.received);

                            }
                            else{
                                this.router.lsd._store.put(lsa.linkStateID, lsa);
                                this.router.forwardPacket(this.received);
                            }
                        }
                    }
                }
            }
            catch (Exception e){
                System.out.println(e);
            }
        }


        try {
            serverSocket.close();
            System.out.println("Server Stopped");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void setInit(){
        try {
            System.out.println("received HELLO from " + this.received.srcIP);

            RouterDescription rd = new RouterDescription(this.received.srcProcessIP,
                    this.received.srcProcessPort, this.received.srcIP);

            Link link = new Link(router.rd, rd, this.received.weight);

            if (this.router.linkExist(link)) {
                System.err.println("This link cannot be attached again!");
                return;
            } else {
                this.router.addLink(link);
            }

            System.out.println("set " + this.received.srcIP + " state to INIT");
            SOSPFPacket response = new SOSPFPacket(this.router.rd.processIPAddress, this.router.rd.processPortNumber,
                    this.router.rd.simulatedIPAddress, this.received.srcIP, (short) 0,
                    "", "", null, this.received.weight);

            outputStream.writeObject(response);
        }
        catch (Exception e){
            System.out.println(e);
        }
    }

    public void setTwoway(){
        try {

            System.out.println("received HELLO from " + this.received.srcIP);

            for(Link link : this.router.ports){
                if(link != null && link.router2.simulatedIPAddress.equals(this.received.srcIP)){
                    link.router2.status = RouterStatus.TWO_WAY;
                    System.out.println("set " + this.received.srcIP + " state to TWO_WAY;");
                    return;
                }
            }

            System.out.println("Set " + this.received.srcIP + " to TWO_WAY failed");


        }
        catch (Exception e){
            System.out.println(e);
        }

    }

}

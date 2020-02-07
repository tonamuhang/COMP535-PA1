package socs.network.node;
import socs.network.message.SOSPFPacket;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;


public class Client implements Runnable{

    private Socket socket = null;
    private ObjectInputStream input = null;
    private ObjectOutputStream output = null;
    private Router src = null;

    private RouterDescription srcrd = null;
    private RouterDescription dstrd = null;

    public Client(Router src, Link link){
        try{
            this.src = src;
            this.srcrd = src.rd;
            this.dstrd = link.router2;
            this.socket = new Socket(dstrd.processIPAddress, dstrd.processPortNumber);
            input = new ObjectInputStream(System.in);
            output = new ObjectOutputStream(socket.getOutputStream());
        }
        catch (UnknownHostException e){
            System.out.println(e);
        }
        catch (IOException e){
            System.out.println(e);
        }
    }

    public void run() {
        SOSPFPacket received;
        System.out.println("Connecting to " + socket.getRemoteSocketAddress() +
                "on port " + socket.getPort());


        try {
            received = (SOSPFPacket) input.readObject();
            if(received.sospfType != 0){
                System.out.println("Unknown packet");
            }
            else{
                System.out.println("Received Hello from " + received.srcIP);
                src.rd.status = RouterStatus.INIT;
                System.out.println("Set " + src.rd.simulatedIPAddress + " to INIT");

                sendHello();
                src.rd.status = RouterStatus.TWO_WAY;
                System.out.println("Set " + src.rd.simulatedIPAddress + " to TWO_WAY");
            }

        }
        catch (ClassNotFoundException e){
            e.printStackTrace();
        }
        catch (IOException e){
            e.printStackTrace();
        }

    }



    private void sendHello() throws IOException{
        RouterDescription srcrd = this.srcrd;
        RouterDescription dstrd = this.dstrd;

        // Create the hello msg packet
        SOSPFPacket msg = new SOSPFPacket(srcrd.processIPAddress,
                (short)srcrd.processPortNumber, srcrd.simulatedIPAddress, dstrd.simulatedIPAddress,
                (short)0, "", "", null
                );

        output.writeObject(msg);
    }

}

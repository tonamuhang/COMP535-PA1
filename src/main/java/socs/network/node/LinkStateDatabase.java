package socs.network.node;

import socs.network.message.LSA;
import socs.network.message.LinkDescription;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class LinkStateDatabase {

    //linkID => LSAInstance
    HashMap<String, LSA> _store = new HashMap<String, LSA>();

    private RouterDescription rd = null;

    // Weighted graph used to compute shortest path
    private WeightGraph wg;
    private HashSet<String> settled;
    private HashSet<String> unsettled;
    //      it costs $value to reach $key node from source node
    private HashMap<String, Integer> nodeAndDistance;
    //      in terms of path from source node to $key node, $value node is $key's predecessor step
    private HashMap<String, String> destinationAndPredecessor;


    public LinkStateDatabase(RouterDescription routerDescription) {
        rd = routerDescription;
        LSA l = initLinkStateDatabase();
        _store.put(l.linkStateID, l);
    }

    /**
     * output the shortest path from this router to the destination with the given IP address
     */
    public void getShortestPath(String destinationIP) {
        String source = rd.simulatedIPAddress;
        wg = new WeightGraph(this);
        settled = new HashSet<String>();
        unsettled = new HashSet<String>();

        nodeAndDistance = new HashMap<String, Integer>();
        destinationAndPredecessor = new HashMap<String, String>();
        nodeAndDistance.put(source, 0);
        unsettled.add(source);

        while (unsettled.size() > 0) {
            String node = getNearestNeighbor(unsettled);
            settled.add(node);
            unsettled.remove(node);
            if (node.equals(destinationIP)){
              break;
            }
            findOtherNeighbors(node);
        }

        String predecessor = destinationAndPredecessor.get(destinationIP);
        if (predecessor == null) {
            System.out.println("no path to " + destinationIP);
            return;
        }
        String current = destinationIP;
        String result = current;
        while (predecessor != null) {
            result = predecessor + " ->(" + wg.edges[wg.find(predecessor)][wg.find(current)] + ") " + result;
            current = predecessor;
            predecessor = destinationAndPredecessor.get(predecessor);
        }
        System.out.println(result);
    }

    public void findOtherNeighbors(String nodeIp) {
        List<String> neighbors = new ArrayList<String>();
        int index = wg.find(nodeIp);
        for (int i = 0; i < wg.edges.length; i++) {
            if (wg.edges[index][i] >= 0 && !settled.contains(wg.myIp[i])) {
                neighbors.add(wg.myIp[i]);
            }
        }
        for (String s : neighbors) {
            if (nodeAndDistance.get(s) != null && nodeAndDistance.get(nodeIp) != null &&
                    nodeAndDistance.get(s) > nodeAndDistance.get(nodeIp) + wg.edges[wg.find(nodeIp)][wg.find(s)]) {
                nodeAndDistance.put(s, nodeAndDistance.get(nodeIp) + wg.edges[wg.find(nodeIp)][wg.find(s)]);
                destinationAndPredecessor.put(s, nodeIp);
                unsettled.add(s);
            }
        }
    }

    public String getNearestNeighbor(HashSet<String> nodes) {
        if (nodes == null || nodes.size() == 0) {
            return null;
        }
        Integer shortestWeight = Integer.MAX_VALUE;
        String shortestNode = null;
        for (String node : nodes) {
            Integer currentWeight = nodeAndDistance.get(node);
            if (currentWeight != null && currentWeight < shortestWeight) {
                shortestNode = node;
            }
        }
        return shortestNode;
    }

    //initialize the linkstate database by adding an entry about the router itself
    private LSA initLinkStateDatabase() {
        LSA lsa = new LSA();
        lsa.linkStateID = rd.simulatedIPAddress;
        lsa.lsaSeqNumber = Integer.MIN_VALUE;
        LinkDescription ld = new LinkDescription();
        ld.linkID = rd.simulatedIPAddress;
        ld.portNum = -1;
        ld.tosMetrics = 0;
        lsa.links.add(ld);
        return lsa;
    }


    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (LSA lsa : _store.values()) {
            sb.append(lsa.linkStateID).append("(" + lsa.lsaSeqNumber + ")").append(":\t");
            for (LinkDescription ld : lsa.links) {
                sb.append(ld.linkID).append(",").append(ld.portNum).append(",").
                        append(ld.tosMetrics).append("\t");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

}

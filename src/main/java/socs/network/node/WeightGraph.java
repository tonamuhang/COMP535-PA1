package socs.network.node;

import socs.network.message.LSA;
import socs.network.message.LinkDescription;

import java.util.Iterator;
import java.util.Map;

public class WeightGraph {
    int n;
    short[][] edges;
    String[] myIp;
    LSA[] myLSA;

    public WeightGraph(LinkStateDatabase lsd) {
        n = lsd._store.size();
        edges = new short[n][n];
        myIp = new String[n];
        myLSA = new LSA[n];
        clear();
        init(lsd);
    }

    /**
     * Initialize the graph using lsd
     * @param lsd
     */
    private void init(LinkStateDatabase lsd) {

        Iterator it = lsd._store.entrySet().iterator();
        for (int i = 0;it.hasNext(); i++) {
            Map.Entry pair = (Map.Entry)it.next();
            myIp[i] = (String) pair.getKey();
            myLSA[i] = (LSA) pair.getValue();
        }

        for (int i = 0; i < myIp.length; i++) {
            for (LinkDescription ld : myLSA[i].links) {
                int j = find(ld.linkID);
                if (j == -1) continue;
                else edges[i][j] = ld.tosMetrics;
            }
        }
    }

    /**
     * Find the index of IP in myID
     * @param IP
     * @return
     */
    public int find(String IP) {
        for (int i = 0; i < myIp.length; i++) {
            if (myIp[i].equals(IP)) return i;
        }
        return -1;
    }

    /**
     * clear edges and myIp
     */
    private void clear() {
        for (int i = 0; i < n; i++) {
            myIp[i] = "";
            for (int j = 0; j < n; j++){
                edges[i][j] = -1;
            }
        }
    }
}

package chord;

import KeyManager.Mapper;
import common.GUI.MainWindow;
import common.Hasher;
import java.io.IOException;
import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Enumeration;
import java.util.Random;
import java.util.StringTokenizer;

/**
 * Class Node represents a chord node in the ring
 *
 * @author DHT-Chord Team
 */
public class Node extends UnicastRemoteObject implements Runnable, Serializable, ChordInterface {

    //-------------------|| VARIABLES ||------------------------
    private MulticastServer ms;
    private int tcpPort;
    private InetAddress nodeaddress;
    private int proccessId;
    Thread thdMS;
    private ChordInterface[] successor; //Contains the next 3 sucessors
    private ChordInterface predecessor;
    private int nodeKey;
    private String rmiaddress;
    MainWindow window;
    Mapper mapper;
    InetAddress myIP;
    private FingerTable fingers;
    private long time; //Stores time at startup

    //==================================/ CONSTRUCTORS \================================================
    /**
     * Constructor.
     *
     * @param window
     * @param mapper
     * @throws RemoteException
     * @throws UnknownHostException
     */
    public Node(MainWindow window, Mapper mapper) throws RemoteException, UnknownHostException {
        super();
        window.getUserText().append("Welcome to DHT");
        time = System.currentTimeMillis();
        successor = new ChordInterface[3];

        this.mapper = mapper;
        setProccessId();
        this.window = window;

        myIP = getCurrentEnvironmentNetworkIp();
        try //Rebind node to rmiregistry 
        {
            Naming.rebind("rmi:/" + myIP + ":1099/Chord-" + this.getProccessId(), this);
            printActivity(">set my RMI Service\n");
        } catch (RemoteException | MalformedURLException e) {
            System.err.println("chord.Node:RMI Service binding failed: " + e);
            e.printStackTrace();
        }
        //Create a MulticastServer but not starting it
        ms = new MulticastServer(this);
        thdMS = new Thread(ms);

        bootstrap();

        fingers = new FingerTable(this, window);


    }//node constructor

//=====================================|| BOOTSTRAP ||========================================
    /**
     * Does the bootstrap of a Node in the Chord.
     */
    private void bootstrap() {
        try {
            window.setChord(this);
            //Initialization of successor array
            successor[0] = this;
            successor[1] = null;
            successor[2] = null;
            predecessor = null;

            //find my proccessID and let MulticastServer know it
            setProccessId();
            ms.setProccessId(proccessId);

            //create a socket at a random TCP port to receive a responce
            ServerSocket s = new ServerSocket(0);
            ServerRbl srb = new ServerRbl(s, this); //a temporary ServerRbl, will only receive one responce
            nodeaddress = getCurrentEnvironmentNetworkIp();
            tcpPort = s.getLocalPort();
            setRmiaddress(nodeaddress);//, tcpPort);
            nodeKey = common.Hasher.myCode(getRmiaddress());
            window.setTitle("Node of code:" + nodeKey);
            s.setSoTimeout(1000);
            ms.sendMulticast(s.getLocalPort());
            String responderRMIAdress = srb.HearResponce(s);

            if (responderRMIAdress != null) {
                try {
                    ChordInterface responder = (ChordInterface) Naming.lookup(responderRMIAdress);
                    ChordInterface succ = responder.FindSuccessor(nodeKey);

                    setSuccessor(succ);
                    succ.notifyP(this);
                } catch (NotBoundException | MalformedURLException | RemoteException ex) {
                    ex.printStackTrace();
                }
            }

            thdMS.start(); //starting multicast server

        } catch (IOException ex) {
            ex.printStackTrace();
        }
        printVars(); //Update GUI

        try {
            mapper.connect(this);  //Connect mapper with keys
        } catch (RemoteException | UnknownHostException ex) {
            ex.printStackTrace();
        }
    }//bootSrap

    //=====================================||STABILIZE - NOTIFY  ||====================================================
    /**
     * Stabilizes the Chord ring. Takes care of successor list and predecessor
     * of Node.
     */
    private void stabilize() {
        ChordInterface x;

        try {
            // if real successpr has no predecessor
            // then do successor.predecessor = this;
            x = successor[0].getPredecessor();
            boolean flag = false;
            if (successor[0].getNodeKey() == this.nodeKey) {
                flag = true;
            }
            if ((x == null) && !flag) {
                
                successor[0].notifyP(this);
                return;
            }

            if (x == null) {
                
                return;
            }

            int xkey = x.getNodeKey();
            // if successor.predecessor > this;
            // then make him my successor
            if (Hasher.isBetween(this.nodeKey, xkey, successor[0].getNodeKey())) {
                System.out.println("stabilize() :: 3");
                this.setSuccessor(x);
                successor[0].notifyP(this);
            }

        } catch (RemoteException ex) {
        }//catch   RemoteException
        catch (NullPointerException e) {
            e.printStackTrace();
        }//catch NullPointerException

    }//stabilize()

    /**
     * Takes care of a Node's exit from the Chord ring.
     */
    public synchronized void leave() {
        window.getUserText().append("Waiting for Chord to stabilize..\nBYE!\n");
        try {    //if i am the only node, exit
            fingers.updater.interrupt();
            if (successor[0].getNodeKey() == this.getNodeKey()) {
                thdMS.interrupt();
                System.exit(0);
            }//else if there are 2 Node in the Chord ring
            else if (successor[0].getNodeKey() == predecessor.getNodeKey()) //if there are 2 nodes and one exits
            {
                predecessor.setPredecessor(null);  //Take care of the other node's references
                predecessor.setSuccessor2(successor[0]);
                thdMS.interrupt();
                mapper.leave();//Send my keys to the other Nodes
                try {

                    Thread.sleep((long) 3000); //Wait for Chord to stabilize and exit
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }

                System.exit(0);
            } else //if ring has more than 2 nodes and one exits
            {
                predecessor.setSuccessor(successor[0]);  //Take care of the other node's references
                successor[0].setPredecessor(predecessor);
                thdMS.interrupt();
                mapper.leave(); //Send my keys to the other Nodes
                try {
                    Thread.sleep((long) 3000); //Wait for Chord to stabilize and exit
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
                System.exit(0);
            }

        } catch (RemoteException ex) {
            ex.printStackTrace();
        }

    }
    //-----------------checkPredecessor-------------

    /**
     * If predecessor is dead, make it null.
     */
    public synchronized void checkPredecessor() {
        if (predecessor == null) {
            return;
        }
        try {
            predecessor.getNodeKey();
        } catch (RemoteException ex) {
            predecessor = null;
            mapper.setPredKey(0);
            fingers.fixfingers();
        }
    }

    /**
     * Check my successor for failures.
     */
    public synchronized void checkSuccessors() {

        try {//===/begin checking 1st successor\================

            successor[0].getNodeKey();//check successor0
            successor[1] = successor[0].getSuccessors(0);//if successor0 ok
            successor[2] = successor[0].getSuccessors(1);

        } catch (RemoteException ex) {//apotyxia successor 0
            try {

                successor[1].getNodeKey();//check successor1
                successor[0] = successor[1];//if successor1 ok
                System.out.print("1");
                successor[1] = successor[1].getSuccessors(0);
                System.out.print("2");
                successor[2] = successor[1].getSuccessors(1);
                System.out.print("3");
                fingers.reset();
            } catch (RemoteException ex1) {//concurrent failure of successor 1
                try {
                    System.out.print("4");
                    successor[0] = successor[2];//check if successor2 ok
                    successor[1] = successor[2].getSuccessors(0);
                    successor[2] = successor[2].getSuccessors(1);
                    fingers.reset();
                } catch (RemoteException ex2) { //concurrent failure of all successors
                    successor[0] = successor[1] = successor[2] = this;
                    fingers.reset();
                }
                //fingers.reset();
            }
        }//catch 0===\end checking 1st successor/================

    }//check Successors

    //------------------notifyP------------
    /**
     * Notify about a change in Predecessor.
     *
     * @param n
     */
    @Override
    public synchronized void notifyP(ChordInterface n) { //notify about a change in Predecessor

        try {
            //if i am alone, make who called me predecessor and successor
            boolean flag = false;
            try {
                if (successor[0].getNodeKey() == this.nodeKey) {
                    flag = true;
                }
            } catch (Exception e) {
            }

            if ((predecessor == null) && flag) {
                System.out.println("notifyP() :: 1");
                this.setPredecessor(n);
                this.setSuccessor(n);
            } //if i have no predecessor, make him who called me
            else if (predecessor == null) {
                System.out.println("notifyP() :: 2");
                this.setPredecessor(n);
            }// 
            else if (Hasher.isBetween(predecessor.getNodeKey(), n.getNodeKey(), this.getNodeKey())) {
                System.out.println("notifyP() :: 3");
                this.setPredecessor(n);
            }
        } catch (RemoteException ex) {
            System.err.println("notifyP remote exception");
        } //catch
        catch (NullPointerException ex) {
            System.err.println("notifyP NullPointer exception");
        }  //catch

    }//notifyP

    //------------------notifyS------------
    /**
     * Notify about a change in Successor. NOT USED !!
     *
     * @param n
     */
    @Override
    public synchronized void notifyS(ChordInterface n) { //notify about a change in Successor
        try {
            if (Hasher.isBetween(this.nodeKey, n.getNodeKey(), successor[0].getNodeKey())) {
                successor[0].notifyP(this);
                this.setSuccessor(n);
            }
        } catch (RemoteException ex) {
            System.err.println("notifyS remote exception");
        }

    }//notifyS

    /**
     * Set Nodes RMI address.
     *
     * @param nodeaddress
     */
    public void setRmiaddress(InetAddress nodeaddress) {//, int TCPPort) {
        this.rmiaddress = "rmi:/" + nodeaddress.toString() + ":1099" /* + TCPPort */ + "/Chord-" + this.proccessId;

    }

    //=====================================||  FIND SUCCESSOR  ||====================================================
    /**
     * Finds a successor of a key k using a nodes n finger table.
     *
     * @param k
     * @return
     * @throws RemoteException
     */
    @Override
    public ChordInterface FindSuccessor(int k) throws RemoteException {
        int succkey = this.successor[0].getNodeKey();

        if ((successor[0].getNodeKey() == this.nodeKey)) {
            printActivity(">FindSuccessor:" + k + " returned myself");
            return this;
        }

        if (Hasher.isBetween(this.nodeKey, k, succkey)) {
            printActivity(">FindSuccessor:" + k + " returned my successor (" + succkey + ")");
            return this.successor[0];
        } else {
            ChordInterface ntemp;
            ntemp = fingers.checkprecNode(k);
            if (ntemp == null) {
                ntemp = this;
            }
            if (ntemp.getNodeKey() == this.nodeKey) {
                return this;
            } else {
                return ntemp.FindSuccessor(k);
            }
        }
    }//Find Successor

    //=======================================|| GETTERS-SETTERS  ||=====================================================
    /**
     *
     * @return
     */
    public String getRmiaddress() {
        return rmiaddress;
    }

    /**
     * Sets nodes processID from JVM.
     */
    private void setProccessId() {
        StringTokenizer st = new StringTokenizer(ManagementFactory.getRuntimeMXBean().getName(), "@");
        this.proccessId = new Integer(st.nextToken());
    }

    /**
     *
     * @return
     */
    public int getProccessId() {
        return proccessId;
    }

    /**
     *
     * @return
     */
    public int getTCPPort() {
        return tcpPort;
    }

    /**
     * Get i'th successor.
     *
     * @param i
     * @return
     */
    @Override
    public ChordInterface getSuccessors(int i) {
        return successor[i];
    }

    /**
     *
     * @return
     */
    public MulticastServer getMS() {
        return ms;
    }

    /**
     *
     * @return
     */
    @Override
    public ChordInterface getPredecessor() {
        return predecessor;
    }

    /**
     *
     * @return
     */
    @Override
    public InetAddress getIP() {
        return this.myIP;
    }

    /**
     *
     * @param predecessor
     * @throws RemoteException
     */
    @Override
    public synchronized void setPredecessor2(ChordInterface predecessor) throws RemoteException {
        this.predecessor = predecessor;
    }

    /**
     * Set my predecessor and call mapper.reDistributeKeys().
     *
     * @param predecessor
     * @throws RemoteException
     */
    @Override
    public synchronized void setPredecessor(ChordInterface predecessor) throws RemoteException {
        if (this.predecessor != null) {
            this.predecessor = predecessor;
        } else {
            this.predecessor = predecessor;
        }
        if (mapper.isConnected()) {
            mapper.reDistributeKeys();
        }
    }

    /**
     *
     * @param successor
     * @throws RemoteException
     */
    @Override
    public synchronized void setSuccessor(ChordInterface successor) throws RemoteException {
        this.successor[0] = successor;
    }

    /**
     *
     * @param successor
     * @throws RemoteException
     */
    @Override
    public void setSuccessor2(ChordInterface successor) throws RemoteException {
        this.successor[0] = this;
    }

    /**
     * Sets node key and prints message to GUI.
     *
     * @param nodeKey
     */
    public void setNodeKey(int nodeKey) {
        this.nodeKey = nodeKey;
        printVars();
    }

    /**
     *
     * @return @throws RemoteException
     */
    @Override
    public int getNodeKey() throws RemoteException {
        return nodeKey;
    }

    /**
     *
     * @return
     */
    public InetAddress getNodeaddress() {
        return nodeaddress;
    }

    /**
     *
     * @param nodeaddress
     */
    public void setNodeaddress(InetAddress nodeaddress) {
        this.nodeaddress = nodeaddress;
    }

    /**
     * Returns local ip address, code by jguru.
     *
     * @return
     * @throws UnknownHostException
     */
    public static InetAddress getCurrentEnvironmentNetworkIp() throws UnknownHostException {
        Enumeration<NetworkInterface> netInterfaces = null;
        try {
            netInterfaces = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            e.printStackTrace();
        }

        while (netInterfaces.hasMoreElements()) {
            NetworkInterface ni = netInterfaces.nextElement();
            Enumeration<InetAddress> address = ni.getInetAddresses();
            while (address.hasMoreElements()) {
                InetAddress addr = address.nextElement();
                if (!addr.isLoopbackAddress() && addr.isSiteLocalAddress() && !(addr.getHostAddress().indexOf(":") > -1)) {
                    return addr;
                }
            }
        }
        try {
            return InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            return InetAddress.getLocalHost();
        }
    }

    /**
     *
     * @return
     */
    public int getPredecessorKey() {
        int key = 0;
        if (predecessor == null) {
            return 0;
        }
        try {
            if (predecessor.getNodeKey() == this.nodeKey) {
                predecessor = null;
                return 0;
            }
            //System.out.println("my pred:" + predecessor.getNodeKey());
            return predecessor.getNodeKey();
        } catch (RemoteException ex) {
            System.err.println("get predecessorkey remote exception");
        }
        return key;
    }

    //==========================================|| GUI METHODS ||===================================================
    /**
     * Prints at GUI the Successor list, the predecessor and the rmi address of
     * the Node.
     */
    public void printVars() {
        String s = "\n";
        s += "thisNodekey:\t\t" + nodeKey + "\n";

        try {
            s += "Succesor[0]Key:\t" + successor[0].getNodeKey() + "\n\n";
            if (successor[1] != null) {
                s += "Succesor[1]Key:\t" + successor[1].getNodeKey() + "\n";
            } else {
                s += "SuccesorKey:\t ---- \n";
            }
            if (successor[2] != null) {
                s += "Succesor[2]Key:\t" + successor[2].getNodeKey() + "\n";
            } else {
                s += "SuccesorKey:\t ---- \n";
            }
            if (predecessor != null) {
                s += "PredescessorKey:\t" + predecessor.getNodeKey() + "\n";
            } else {
                s += "No predecessor\n";
            }
        } catch (RemoteException ex) {
            System.err.println("chord printVars remote exception");
        }
        s += "RMI url: " + rmiaddress + "\n";
        window.getChordVarsText().setText(s);
    }//printVars

    /**
     * Updates the text area of the GUI with the Node's chord activity.
     *
     * @param a
     */
    public void printActivity(String a) {
        window.getChordActivityText().append(a + "\n");
    }

    //=========================================|| RUN  ||==========================================================
    public void run() {
        while (true) {
            checkPredecessor();
            checkSuccessors();
            stabilize();
            long temp = (System.currentTimeMillis() - time) / 1000; //calculate elapsed time
            window.getjTextArea1().setText("    Running time: " + temp + " sec\n");
            try {
                Random r = new Random();
                Thread.sleep(500 * r.nextInt(2));
            } catch (InterruptedException ex) {
            } catch (Exception e) {
                System.err.println("Node.run():" + e.getClass());
            }

        }//while (true)

    }//run

    /**
     * Print finger table.
     */
    public void printFingers() {
        fingers.printFingerTable();
    }
}
package KeyManager;

import FileApplication.MaybeAnApp;
import chord.ChordInterface;
import chord.Node;
import common.FileLocation;
import common.GUI.MainWindow;
import common.Hasher;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Iterator;
import java.util.Map;
import java.util.Stack;
import java.util.TreeMap;

/**
 * Class responsible to hold and distribute keys to the Chord ring.
 * @author DHT-Chord Team
 */
public class Mapper extends java.rmi.server.UnicastRemoteObject implements RemoteMapper, Runnable {

    //-----------|| VARIABLES ||-----------------------
    TreeMap<Integer, FileLocation> map;
    MaybeAnApp FileApp;
    MainWindow window;
    int nodeKey;
    Stack<Entry> toDistribute;
    int predKey;
    Node chord;
    Thread manager;

    /*
     * Executes after constructor.
     */
    private void init() throws UnknownHostException, RemoteException {

        FileApp.setMapper(this);
        predKey = 0;
        toDistribute = readNodeEntries();
        //System.out.println("Mapper.init() :: toDistribute="+toDistribute);
    }

//==================================/ CONSTRUCTORS \================================================
    /**
     * Constructs a Mapper Object that is no connected to a Node
     *
     * @param upperApp the "FileApplication.MaybeAnApp" application that manages
     * the files of the node
     * @param window the MainWindow of the node
     * @throws RemoteException
     */
    public Mapper(MaybeAnApp upperApp, MainWindow window) throws RemoteException, UnknownHostException {
        super();
        this.window = window;
        this.FileApp = upperApp;
        this.window = window;
        init();

    }//STD Constructor

    /**
     * connects this Mapper Object to a Node.
     *
     * @param node node the "chord.Node" that this Mapper Object will be
     * connected to
     * @throws RemoteException
     * @throws UnknownHostException
     */
    public void connect(Node node) throws RemoteException, UnknownHostException {
        map = new TreeMap();
        this.chord = node;
        this.nodeKey = node.getNodeKey();
        if (chord.getPredecessor() != null) {
            predKey = chord.getPredecessor().getNodeKey();
        }
        bind(this.nodeKey);
        window.setMapper(this);
        manager = new Thread(this);
        manager.start();
    }

    //=================================/REMOTE SERVICES\============================
    /**
     * adds an entry of the CHORD system to this Mapper.
     *
     * @param e the Entry Object that will be added to this Mappers' map (an
     * entry that belongs to the portion of the DHT that the Mapper should keep
     * @throws RemoteException
     */
    @Override
    synchronized public void addEntry(Entry e) throws RemoteException {
        if (this.map.containsKey(e.getHash())) {
            return;
        }
        //System.out.println("Mapper.addEntry() :: entry="+e.getHash()+","+e.getLocation());
        this.map.put(e.getHash(), e.getLocation());
        printAct(">added entry:" + e.hash);
    }

    /**
     * removes an entry from this MApper
     *
     * @param key the key of an Entry that is going to be removed
     */
    @Override
    synchronized public void removeKey(Integer key) {
        this.map.remove(key);
    }

    /**
     * Gets a key (for which this node is accounting for) and looks it up to
     * find its "FileLocation".
     *
     * @param key represents a key that will be looked-up to find it's
     * FileLocation
     * @return the FileLocation of the key that was looked-up, null if not found
     * @throws RemoteException
     */
    @Override
    public common.FileLocation lookup(int key) throws RemoteException {
        common.FileLocation fl = map.get(key);
        if (fl == null) {
            printAct(">requested of key: " + key + ", returned null");
        } else {
            printAct(">requested of key: " + key + ", returned: " + fl);
        }
        return fl;
    }

    /**
     * Add elements of TreeMap toAdd to current elements map.
     *
     * @param toAdd
     */
    @Override
    synchronized public void addMap(TreeMap toAdd) {
        this.map.putAll(toAdd);
        printAct("added a whole map with keys from:" + toAdd.firstKey() + " up to:" + toAdd.lastKey());
    }

    //=================/LOCAL METHODES WITH REMOTE ACCESS\=================
    /**
     * Distributes to the CHORD system all the Entries stored in the
     * "toDistribute" Stack that this Mapper holds.
     *
     */
    void distributeKeys() {
        // System.out.println("Mapper.distributeKeys() :: 1");
        while (!toDistribute.empty()) {
            Entry e = toDistribute.pop();
            sendEntry(e);
            printAct(">sent Entry " + e.hash);

        }//while
    }//distributeKeys

    /**
     * Sends to the accounting Mapper in the system one Entry
     *
     * @param e
     */
    void sendEntry(Entry e) {
        // System.out.println("Mapper.sendEntry()");
        RemoteMapper dest = findMapper(e.getHash());
        try {
            if (predKey != 0) {
                removeKey(e.getHash());
            }
            dest.addEntry(e);
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }

    }

    //--------|reDistributeKeys------
    /**
     * Goes through all the entries stored in this Mapper and adds those for
     * which this Mapper is not accountant for in the "toDistribute Stack so
     * they can be later (automatically) distributed in the system.
     *
     */
    public void reDistributeKeys() {
        predKey = chord.getPredecessorKey();
        if (predKey == 0) {
            return;
        }
        if (map.isEmpty()) {
            return;
        }

        Iterator it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, FileLocation> entry = (Map.Entry) it.next();
            //If I am not a successor of this Entry, put in in toDistribute Stack
            if (!Hasher.isBetween(predKey, entry.getKey(), nodeKey)) {
                toDistribute.push(new Entry(entry));
            }
        }

    }//reDistributeKeys

    /**
     * Re-distributes the keys of the files physically stored in this node.
     */
    public void reDistributeMyKeys() {
        toDistribute = readNodeEntries();
    }

    /**
     * Searches in the system for the mapper accounting for the key "k".
     *
     * @param k the key for which it finds a Mapper accountant for
     * @return the remote-Mapper accounting for the key "k", null if it fails
     */
    RemoteMapper findMapper(int k) {

        RemoteMapper rm = null;
        try {
            ChordInterface c = this.chord.FindSuccessor(k);
            rm = (RemoteMapper) Naming.lookup("rmi:/" + c.getIP() + ":1099/Keys-" + c.getNodeKey());
        } catch (RemoteException ex) {
            System.err.println("findMapper remote exception");
        } catch (NotBoundException en) {
            System.err.println("findMapper notBoundException");
        } catch (MalformedURLException em) {
            em.printStackTrace();
        }
        return rm;
    }

    //---------| find File |----------------
    /**
     * Finds the FileLocation of the file corresponding to "k" key.
     *
     * @param k the key of the file that will be found
     * @return the FileLocation of the file corresponding to "k" key, null if it
     * is not in the system
     */
    public FileLocation findFile(int k) {
        RemoteMapper rm = findMapper(k);
        FileLocation fl = null;
        try {
            return rm.lookup(k);
        } catch (RemoteException ex) {
            System.err.println("Mapper.findFile remote Exception");
        }

        return fl;
    }

    /**
     * Removes from the system all of the "Entry" objects stored in a stack.
     *
     * @param rs the Stack of Entries that are going to be removed
     */
    void unDistributeKeys(Stack<Entry> rs) {
        while (!rs.empty()) {
            Integer key = rs.pop().getHash();
            RemoteMapper rm = findMapper(key);
            try {
                rm.removeKey(key);
            } catch (RemoteException ex) {
                ex.printStackTrace();
            }
        }//while
    }//unDistributeKeys

    /**
     * Removes from the system all the Entries of the files physically stored in
     * the Node.
     */
    public void unDistributeMyKeys() {
        unDistributeKeys(readNodeEntries());
    }

    /**
     * Interrupts the Mappers "manager" thread and adds all of the Entries
     * stored in the map to the map of its successor's Mapper.
     */
    synchronized public void leave() {
        manager.interrupt();
        manager = null;
        RemoteMapper rm = null;
        try {
            ChordInterface c = this.chord.getSuccessors(0);
            rm = (RemoteMapper) Naming.lookup("rmi:/" + c.getIP() + ":1099/Keys-" + c.getNodeKey());
            //===parsing the whole map
            rm.addMap(this.map);
        } catch (RemoteException ex) {
            ex.printStackTrace();
            //trying again for the 2nd successor
            try {
                ChordInterface c = this.chord.getSuccessors(1);
                rm = (RemoteMapper) Naming.lookup("rmi:/" + c.getIP() + ":1099/Keys-" + c.getNodeKey());
                rm.addMap(this.map);
            } catch (RemoteException e) {
                e.printStackTrace();
            } catch (NotBoundException en) {
                en.printStackTrace();
            } catch (MalformedURLException em) {
                em.printStackTrace();
            }
        } catch (NotBoundException en) {
            en.printStackTrace();
        } catch (MalformedURLException em) {
            em.printStackTrace();
        }

        unDistributeMyKeys(); //Give my keys back
    }//leave

    /**
     * Checks if the nodes that physically store every key that this Mapper
     * keeps are alive.
     *
     */
    synchronized void checkAlive() {
        printAct("==>Checking all my Entries if alive ");
        Iterator it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, FileLocation> entry = (Map.Entry) it.next();
            FileLocation location = entry.getValue();
            try {
                InetSocketAddress server = new InetSocketAddress(location.getIP(), location.getPort());
                Socket s = new Socket();
                s.connect(server, 200); //connect with to the AppliCation with a timeout
                ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
                out.writeInt(2);      //request 2 = file request
                out.writeInt(5); //"random" port, specific for this request
                out.close();
                s.close();
            } catch (java.net.SocketTimeoutException ex) {
                this.removeKey(entry.getKey());
            } catch (IOException ex) {
                this.removeKey(entry.getKey());
            }
        }
        printAct("==>Checked all my Entries if alive ");
    }//checkAlive

    //===============================/LOCAL METHODES\=====================
    //----------|readNodeHashes|----------
    /**
     * Returns in a Stack the Entries of all the files physically stored in the
     * Node (it gets the keys from the "FileApp").
     *
     * @return a Stack the Entries of all the files physically stored in the
     * Node.
     */
    public Stack<Entry> readNodeEntries() {
        FileLocation thisNode = null;
        Stack<Entry> nodeEntries = new Stack<Entry>();
        try {
            int port = FileApp.getMyPort();
            thisNode = new FileLocation(port);
        } catch (UnknownHostException ex) {
            System.err.println("Mapper.read node entries uknown host exception");
        }
        Stack<Integer> nodeHashes = FileApp.getHashes();
        while (!nodeHashes.empty()) {
            nodeEntries.push(new Entry(nodeHashes.pop(), thisNode));
        }//while (!nodeHashes.empty()
        printAct(">scanned my node's files");
        return nodeEntries;
    }//readNodeHashes

    /**
     * Binds this Mapper Object to an RMI url, that is dependant on the nodeKey
     * (used as the key of the node this mapper is connected to)
     *
     * @param nodeKey the key that will be used to bind this Mapper to an RMI url.
     * @throws UnknownHostException
     * @throws RemoteException
     */
    void bind(int nodeKey) throws UnknownHostException, RemoteException {

        java.util.StringTokenizer st = new java.util.StringTokenizer(java.lang.management.ManagementFactory.getRuntimeMXBean().getName(), "@");
        @SuppressWarnings("static-access")
        String ip = Node.getCurrentEnvironmentNetworkIp().toString();
        try {
            Naming.rebind("rmi:/" + ip + ":1099/Keys-" + nodeKey, this);
        } catch (MalformedURLException ex) {
            ex.printStackTrace();
        }
    }

    //---------|setPredKey|------------------
    /**
     * Sets the key of the predecending node. All the keys not between the
     * predecessor an this node will be redistributed in the system.
     *
     * @param predKey the key of the predecending node ("predecessor")
     */
    synchronized public void setPredKey(int predKey) {
        this.predKey = predKey;
    }

    /**
     * checks if this "Mapper" Object is connected to a "Node" Object
     *
     * @return true if connected false if not
     */
    public boolean isConnected() {
        return (chord != null);
    }

//===============================/GUI UPDATER METHODES\=====================
    /**
     * Replaces in the window the text showing the Entries this Mapper is
     * accounting for.
     */
    public void printMap() {
        String s = "";
        Stack st = new Stack();
        st.addAll(map.entrySet());
        while (!st.isEmpty()) {
            s += st.pop().toString() + "\n";
        }
        window.getDHTText().setText(s);
    }
    //----------|printAct|-------------------

    /**
     * Adds a string to the Activity text of the Key Level in the window
     *
     * @param s the String that will be added
     */
    void printAct(String s) {
        window.getKeyActivityText().append(s + "\n");
    }
    //----------|printInfo|-------------------

    /**
     * Replaces in the window the text showing general information about this
     * Mapper.
     */
    public void printInfo() {
        String s = "";
        if (map.isEmpty()) {
            s += "not keeping any entries\n";
        } else {
            s += "This Node stores a range of " + map.size() + " keys :\n from:" + map.firstKey() + "  up to: " + map.lastKey() + "\n";
        }
        s += "node key:\t" + nodeKey + "\n";
        s += "previous key:\t" + predKey + "\n";

        window.getKeysInfoText().setText(s);
    }

    //================/run\=======================
    /**
     * Does the "houseKeeping" of the Mapper. Sends all entries that are to be
     * distributed from this Node to the System (for whatever reason)
     * ReDistributes periodically the keys physically stored in this Node to the
     * System Checks what entries this Mapper keeps are not in its
     * responsibility and should be sent to the according Mapper (end sends
     * them) checks if the nodes that store the actual Files the keys correspond
     * to are alive.
     */
    public void run() {
        int i = 0;
        try {
            while (!isConnected()) {
                Thread.sleep(100);
            }
        } catch (InterruptedException ex) {
            Thread.dumpStack();
        }

        while (true) {
            try {
                if (i % 20 == 0) {
                    // System.out.println("Mapper.run() :: 1");
                    reDistributeMyKeys(); //runs less than once in 5sec
                }
                if (i % 10 == 0) {
                    //System.out.println("Mapper.run() :: 2");
                    reDistributeKeys();    //runs less than 0,75 per second
                }
                if (i % 5 == 0) {
                    //System.out.println("Mapper.run() :: 3");
                    distributeKeys();                   //runs less than 4 times a second
                }
                if (i % 20 == 0) {
                    //System.out.println("Mapper.run() :: 4");
                    checkAlive();//runs less than once in 5ssec
                }
                if (i % 100 == 0) {              //runs less than once in 25sec sees updates in directory files
                    //System.out.println("Mapper.run() :: 5");
                    FileApp.changeDirectrory(FileApp.getDirectory());
                }

                i++;

                Thread.sleep(250);
            } catch (InterruptedException ex) {
                System.err.println("Mapper.run(): Interrupted exception");
                Thread.dumpStack();
            } catch (Exception e) {
                System.err.println("Mapper.run() threw exception " + e.getClass());
                e.printStackTrace();
            }


        }//while(true)

    }///run
}//Mapper


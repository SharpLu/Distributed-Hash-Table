package chord;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.rmi.RemoteException;
import java.util.StringTokenizer;

/**
 * Provides TCP connectivity. Acts as a server and client.
 *
 * @author DHT-Chord Team
 */
public class ServerRbl implements Runnable {

    private int destinationPort;
    private InetAddress destinationAddr;
    private ServerSocket ss;
    private Node node;
    private String addr;
    private String procid;
    private String rmiaddress;
    private String robj;

    /**
     * Constructor
     *
     * @param node
     * @throws RemoteException
     */
    public ServerRbl(Node node) throws RemoteException {
        super();
        this.node = node;

    }//constructor

    ServerRbl(ServerSocket ss, Node node) {
        this.node = node;
        this.ss = ss;
    }

    ServerRbl(int tcpPort, InetAddress clientAddr, Node node) {
        this.destinationPort = tcpPort;
        this.destinationAddr = clientAddr;
        this.node = node;

    }

    ServerRbl(int tcpPort, InetAddress clientAddr) {
        this.destinationPort = tcpPort;
        this.destinationAddr = clientAddr;

    }

    public void run() {
        if (destinationAddr != null) {
            try {

                Socket s = new Socket();
                s.connect(new InetSocketAddress(destinationAddr, destinationPort), 10000);

                ObjectOutputStream responseObj = new ObjectOutputStream(s.getOutputStream());
                robj = s.getLocalAddress().toString() + "\n" + new Integer(node.getProccessId()).toString();

                responseObj.writeObject(robj);
                s.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }


    }//run()

    public String HearResponce(ServerSocket ss) {
        String addrLocal;
        String procidLocal;
        InputStream inStream;
        
        try {
            ss.setSoTimeout(1000);
            Socket incoming = ss.accept();
            inStream = incoming.getInputStream();
            ObjectInputStream in = new ObjectInputStream(inStream);
            try {
                String response = (String) in.readObject();

                //Tokenizing to get ip kai proccessid
                StringTokenizer st = new StringTokenizer(response);
                addrLocal = st.nextToken("\n");
                procidLocal = st.nextToken("\n");
                String temp = "rmi:/" + addrLocal + "/Chord-" + procidLocal;
                node.window.getChordActivityText().append("got responce:" + temp + "\n");
                return temp;

            } catch (ClassNotFoundException ex) {
               ex.printStackTrace();
            } catch (RemoteException ex) {
                ex.printStackTrace();
            }
            inStream.close();
        } catch (SocketTimeoutException ste) {
            node.window.getChordActivityText().append("No one is here! Creating my Chord\n");
            return null;
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        node.window.getChordActivityText().append("got no responce");
        return null;
    }//HearResponce

    public synchronized String getAddr() {
        return addr;
    }

    public synchronized void setAddr(String addr) {
        this.addr = addr;
    }

    public synchronized String getClientRmiaddress() {
        return rmiaddress;
    }

    public synchronized void setClientRmiaddress(String rmiaddress) {
        this.rmiaddress = rmiaddress;
    }

    public synchronized String getProcid() {
        return procid;
    }

    public synchronized void setProcid(String procid) {
        this.procid = procid;
    }

    public synchronized String getRobj() {
        return robj;
    }
}

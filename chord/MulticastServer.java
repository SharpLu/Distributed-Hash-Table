package chord;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implements the multicast server required to find ring nodes for bootstrap.
 *
 * @author DHT-Chord Team
 */
public class MulticastServer implements Runnable {

    public static final int MCAST_PORT = 11111;
    public static final String MCAST_ADDR = "225.4.5.6";
    public static final long SLEEP_TIME = 5000;
    private int tcpPort;
    private InetAddress clientAddr;
    private int proccessId;
    private Node node;

    /*
     * Constructor.
     */
    MulticastServer(Node node) {
        super();
        this.node = node;
    }

    /**
     *
     * @param proccessId
     */
    public void setProccessId(int proccessId) {
        this.proccessId = proccessId;
    }

    /**
     *
     * @return
     */
    public int getTCPPort() {
        return tcpPort;
    }

    /**
     * Sends Multicast to the specified ip
     *
     * @param tcpPort
     */
    void sendMulticast(int tcpPort) {
        this.tcpPort = tcpPort;
        try {
            MulticastSocket ms = new MulticastSocket();
            byte[] buf = intToByteArray(tcpPort);
            DatagramPacket packet = new DatagramPacket(buf, buf.length, InetAddress.getByName(MCAST_ADDR), MCAST_PORT);

            ms.send(packet);
            ms.close();
            node.window.getChordActivityText().append(">sent Multicast, port=" + tcpPort + "\n");

        } catch (IOException ioe) {
            ioe.printStackTrace();
            System.err.println("sendMulticast");
        }

    }

    /**
     * Receives Multicast.
     *
     * @throws InterruptedException
     */
    void receiveMulticast() throws InterruptedException {
        try {
            MulticastSocket ms = new MulticastSocket(MCAST_PORT);
            ms.joinGroup(InetAddress.getByName(MCAST_ADDR));
            byte[] buf = new byte[1024];

            DatagramPacket initPack = new DatagramPacket(buf, buf.length);
            ms.receive(initPack);

            int clientTCPPort = byteToInt(initPack.getData());//msd * 100 + lsd;
            clientAddr = initPack.getAddress();
            if (clientTCPPort != tcpPort) {
                node.printActivity(">Received Multicast by clien: " + clientTCPPort + " and Addr: " + clientAddr.toString());
                Thread thd = new Thread(new ServerRbl(clientTCPPort, clientAddr, node));
                thd.start();
            }


        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    /**
     * Converts data to be read from multicast.
     *
     * @param b
     * @return
     */
    private static int byteToInt(byte[] b) {
        int val = 0;
        for (int i = b.length - 1, j = 0; i >= 0; i--, j++) {
            val += (b[i] & 0xff) << (8 * j);
        }
        return val;
    }

    /**
     * Converts data to be sent by multicast.
     *
     * @param value
     * @return
     */
    private static byte[] intToByteArray(int value) {
        byte[] b = new byte[4];
        for (int i = 0; i < 4; i++) {
            int offset = (b.length - 1 - i) * 8;
            b[i] = (byte) ((value >>> offset) & 0xFF);
        }
        return b;
    }

    /**
     * Executes receive multicast.
     */
    public void run() {
        node.window.getChordActivityText().append(">my MC server set\n");
        try {
            while (true) {
                receiveMulticast();
            }
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }

    }
}

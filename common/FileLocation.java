package common;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Represents a file location with IP and Port.
 *
 * @author DHT-Chord Team
 */
public class FileLocation implements java.io.Serializable {

    InetAddress IP;
    int port;

    /**
     * Constructor.
     *
     * @param IP
     * @param port
     */
    public FileLocation(InetAddress IP, int port) {
        this.IP = IP;
        this.port = port;
    }

    /**
     * Constructor.
     *
     * @param port
     * @throws UnknownHostException
     */
    public FileLocation(int port) throws UnknownHostException {
        this.port = port;
        this.IP = chord.Node.getCurrentEnvironmentNetworkIp();
    }

    //Getters Setters
    public InetAddress getIP() {
        return IP;
    }

    /**
     *
     * @param IP
     */
    public void setIP(InetAddress IP) {
        this.IP = IP;
    }

    /**
     *
     * @return
     */
    public int getPort() {
        return port;
    }

    /**
     *
     * @param port
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     *
     * @return
     */
    @Override
    public String toString() {
        String s = "";
        s += "Location\tIP:" + IP + " port:" + port;
        return s;
    }
}//FileLocation

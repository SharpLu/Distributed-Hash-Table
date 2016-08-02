package chord;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Remote interface for Chord nodes.
 *
 * @author DHT-Chord team
 */
public interface ChordInterface extends Remote {

    public ChordInterface FindSuccessor(int k) throws RemoteException;

    public int getNodeKey() throws RemoteException;

    public ChordInterface getSuccessors(int i) throws RemoteException;

    public void setSuccessor(ChordInterface successor) throws RemoteException;

    public void setSuccessor2(ChordInterface successor) throws RemoteException;

    public void setPredecessor(ChordInterface predecessor) throws RemoteException;

    public ChordInterface getPredecessor() throws RemoteException;

    public void setPredecessor2(ChordInterface predecessor) throws RemoteException;

    public java.net.InetAddress getIP() throws RemoteException;

    public void notifyP(ChordInterface n) throws RemoteException;

    public void notifyS(ChordInterface n) throws RemoteException;
}

import FileApplication.MaybeAnApp;
import KeyManager.Mapper;
import chord.Node;
import common.GUI.*;
import java.net.UnknownHostException;
import java.rmi.RemoteException;

/**
 * Main class of Application. Contains main method.
 *
 * @author DHT-Chord Team
 */
public class Main {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws RemoteException, UnknownHostException, InterruptedException {


        //=========chosing a Directory======
        DirChoserDialog dc = new DirChoserDialog(null, true);
        dc.setTitle("===Chose a Directory to start Sharing====");
        dc.setVisible(true);
        java.io.File dir;
        while (null == (dir = dc.getFile())) {
            dc.repaint();
            Thread.sleep(250);
        }
        dc = null;
        MainWindow mw = new MainWindow();
        MaybeAnApp app1 = new MaybeAnApp(dir, mw);

        Mapper mapper = new Mapper(app1, mw);
        Node node = new Node(mw, mapper);
        Thread nodeThread = new Thread(node);

        Thread windowThread = new Thread(mw);
        windowThread.start();
        nodeThread.start();


    }
}

package FileApplication;

import KeyManager.Mapper;
import common.FileLocation;
import common.GUI.MainWindow;
import common.Hasher;
import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Initiates the mapper, requestServer and mainWindow.
 * @author DHT-Chord Team
 */
public class MaybeAnApp {
//----------------------/ Class Fields \--------------------------------------------------------------------------

    private int myPort;  // The port of this client (Random)
    private MainWindow window;
    Thread windowThread;
    private DirManipulator dirMan;
    Thread reqServer;
    Mapper mapper;

    //====================/ Constructor \==================================
    /**
     * Constructs an application that uses a given directory and is managed by
     * given "MainWindow".
     * @param dir The Directory this Application will use
     * @param window the MainWindow of this node
     */
    public MaybeAnApp(File dir, MainWindow window) {
        this.window = window;
        window.setFileApp(this);
        ServerSocket hearingSocket = null;
        try {
            hearingSocket = new ServerSocket(0);
        } catch (IOException e) {
            System.err.println("Could not listen ");
            System.exit(1);
        }//catch
        myPort = hearingSocket.getLocalPort();
        window.getServerText().append("Server Listening on port: " + myPort + " \nWatching directory: " + dir + "\n");
        window.getDirText().setText(dir.toString());
        dirMan = new DirManipulator(dir);

        reqServer = new Thread(new RequestServer(this, hearingSocket, window.getServerText()));
        reqServer.start();
        window.setVisible(true);
        windowThread = new Thread(window);
        windowThread.start();

    }//constructor

    /**
     * Changes the directory that this application uses and uses the Mapper of
     * the node to un-distribute the old keys and distribute the new ones.
     *
     * @param dir the new Directory
     */
    public void changeDirectrory(File dir) {
        mapper.unDistributeMyKeys();
        dirMan.setDirectrory(dir);
        mapper.reDistributeMyKeys();
        window.getServerText().append(">Changed watch directory\n");

    }

    //=======================/requests Methods\==================================
    
    /**
     * Finds the key of a given string (filename) and sends a File 
     * Request to the given IP and Port, for that key. 
     * It saves the file to the "file" File.
     * @param srvrAddr the IP the request will be sent to
     * @param srvrPort the port the request will be sent to
     * @param filename the FileName for which a the request is sent for
     * @param file the file that will be created, where the received file will
     * be saved
     */
    void sendFileRequest(InetAddress srvrAddr, int srvrPort, String filename, File file) {
        int fileCode = Hasher.myCode(filename);
        sendFileRequest(srvrAddr, srvrPort, fileCode, file);

    }//sendFileRequest (requesting a File)

    /**
     * Sends a File Request to the givenIP and Port, for the given key. 
     * It saves the file to the "file" File. For the transfer, a different Thread is used
     * @param srvrAddr srvrAddr the IP the request will be sent to
     * @param srvrPort srvrPort the port the request will be sent to
     * @param fileCode filename the FileName for which a the request is sent for
     * @param file file the file that will be created, where the received file
     * will be saved.
     */
    void sendFileRequest(InetAddress srvrAddr, int srvrPort, int fileCode, File file) {
        int request = 1;

        //will now create a FileReceiverServant hearing on a new Port so that it can receive a file in autonomy
        ServerSocket receivingSocket = null;
        try {
            receivingSocket = new ServerSocket(0); // with 0 will create a Socket on any free port!
        } catch (IOException ioe) {
            System.err.println("File Application: Could not create a ServerSocket to receive a file");
        }
        int receiverPort = receivingSocket.getLocalPort();// keepin the port of the receivingSocket

        FileReceiverServant frs = new FileReceiverServant(receivingSocket, file, this);
        Thread receiver = new Thread(frs);
        receiver.start();
        //------we have now created and started an autonomous Servant that will receive the File "file" when the responding application snds it

        //=========/ sending the request \==================================
        try {
            Socket s = new Socket(srvrAddr, srvrPort);
            ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
            out.writeInt(request);      //request 1 = file request
            out.writeInt(receiverPort); //"random" port, specific for this request
            out.writeInt(fileCode);     //the "myHash" of the file
            out.close();
            s.close();

        } catch (IOException ioe) {
            window.getClientText().append("Clould not Connect to address:" + srvrAddr + " port:" + srvrPort + "to send a file Request" + "\n");
        }//catch

    }//sendFileRequest (requesting a code)

    /**
     * Returns the file corresponding to the given code.
     *
     * @param code the code to search for
     * @return a file corresponding to the given code, null if it is not in the
     * directory of the application
     */
    public File mapCode(int code) {
        return dirMan.getFile(code);
    }

    /**
     * Returns a Stack of Integers containing all of the keys 
     * corresponding to the files inside the directory the 
     * application uses.
     *
     * @return Stack of Integers (codes of all the files within "dir")
     */
    public Stack<Integer> getHashes() {
        Stack<Integer> set = new Stack();
        set.addAll(dirMan.fileMap.keySet());
        return set;
    }//getHashes

    /**
     * Getter for the TCP port to which the RequestServer of this Application is
     * hearing.
     *
     * @return
     */
    public int getMyPort() {
        return myPort;
    }

    public File getDirectory() {
        return dirMan.dir;
    }

    /**
     * Sets the mapper the application will use to find files inside the system
     * an publish its own files.
     *
     * @param mapper a Mapper Object
     */
    public void setMapper(Mapper mapper) {
        this.mapper = mapper;
    }

    /**
     * Finds and downloads to the file F the file corresponding 
     * to "code".
     *
     * @param f where the file will be downloaded to
     * @param code the hashcode of the file to download
     */
    public void download(File f, int code) {
        long startTime = System.currentTimeMillis();
        FileLocation fl = mapper.findFile(code);
        if (fl == null) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                Logger.getLogger(MaybeAnApp.class.getName()).log(Level.SEVERE, null, ex);
            }
            fl = mapper.findFile(code);
        }//if fl==null
        if (fl == null) {
            printUserAct("> File Not Found in the system\n");
        }//if (fl==null) again
        else {
            long endTime = System.currentTimeMillis()-startTime;
            printUserAct("> File Found, sending request...");
            
            sendFileRequest(fl.getIP(), fl.getPort(), code, f);
            printUserAct("Download took "+endTime+" milliseconds.");
        }//else

    }//download

    /**
     * Prints a line to the ServerActivity text field in the MainWindow.
     *
     * @param s the string to print
     */
    void printSrvrAct(String s) {
        window.getServerText().append(">" + s + "\n");
    }

    /**
     * Prints a line to the ClientActivity text field in the MainWindow.
     *
     * @param s the string to print
     */
    void printClientAct(String s) {
        window.getClientText().append(">" + s + "\n");
    }

    /**
     * Prints a line to the UserActivity text field in the MainWindow.
     *
     * @param s the string to print
     */
    void printUserAct(String s) {
        window.getUserText().append(s + "\n");
    }
}//MaybeAnApp


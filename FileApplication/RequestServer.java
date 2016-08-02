package FileApplication;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import javax.swing.JTextArea;

/**
 * Listens to Requests from other nodes and executes appropriate functions.
 * @author DHT-Chord Team
 */
public class RequestServer implements Runnable {

    ServerSocket hearingSocket;
    JTextArea log;
    MaybeAnApp father;

    /**
     * Constructor
     *
     * @param father
     * @param hearingSocket
     * @param log
     */
    public RequestServer(MaybeAnApp father, ServerSocket hearingSocket, JTextArea log) {
        this.hearingSocket = hearingSocket;
        this.log = log;
        this.father = father;
    }

    /**
     * 
     */
    public void run() {
        while (true) {
            receiveRequest();
        }

    }//run

    /**
     * Method to Receive a request for a file.
     */
    void receiveRequest() {
        int request = -1;
        try {
            Socket s = hearingSocket.accept();
            ObjectInputStream in = new ObjectInputStream(s.getInputStream());
            request = in.readInt();
            int clientPort = in.readInt();
            InetAddress cIP = s.getInetAddress();


            switch (request) {
                case 1: { //sendFile
                    int fileCode = in.readInt();
                    log.append(">received a request for code " + fileCode + "\n");
                    in.close();
                    s.close();
                    sendFile(cIP, clientPort, fileCode);
                }//case 1
                case 2: {//isAlive
                    //log.append("isAliveRequest from" + cIP + "\n");
                    in.close();
                    s.close();
                }

                    
            }//switch

        } catch (IOException ioe) {
            log.append("Clould not hear request " + ioe.getClass() + "\n");
            ioe.printStackTrace();
        }//catch
    }//receiveRequest()

    //============================/sendFile\========================================
    void sendFile(InetAddress clientAddr, int clientPort, File file) {
        log.append(">Sending file: " + file.getAbsolutePath() + " to: " + clientAddr + ":" + clientPort+"\n");
        FileSenderServant fs = new FileSenderServant(clientAddr, clientPort, file, log);
        Thread sender = new Thread(fs);
        sender.start();
    }//sendFile

    void sendFile(InetAddress clientAddr, int clientPort, int code) {
        File file = father.mapCode(code);
        if (file != null) {
            sendFile(clientAddr, clientPort, file);
        } else {
            log.append("requested of me a file I don't have. (code=" + code + ") from:" + clientAddr + ":" + clientPort + "\n");
        }
    }//sendFile - code
}//RequestServer

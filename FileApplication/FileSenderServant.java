package FileApplication;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import javax.swing.JTextArea;

/**
 * A runnable class responsible for sending a file via TCP.    
 * @author DHT-Chord Team
 */
public class FileSenderServant implements Runnable {

    
    public static int bufferSize = 1024;
    File file;
    InetAddress clientAddr;
    int clientPort;
    JTextArea log;

    /**
     * Constructor.
     * @param clientAddr the address to which the file will be sent to
     * @param clientPort the port to which the file will be sent to
     * @param file the file thar will be sent
     * @param log a JTextArea for printing activity
     */
    public FileSenderServant(InetAddress clientAddr, int clientPort, File file, JTextArea log) {
        this.file = file;
        this.clientAddr = clientAddr;
        this.clientPort = clientPort;
        this.log = log;
    }//Constructor

    /**
     * Performs the upload.
     */
    public void run() {
        Socket receiverServer = null;
        byte[] mybytearray = new byte[bufferSize];
        log.append("attempting to send file: +" + file.getAbsolutePath() + "\n");

        try {
            receiverServer = new Socket(clientAddr, clientPort);

            BufferedInputStream in =
                    new BufferedInputStream(
                    new FileInputStream(file));

            BufferedOutputStream out =
                    new BufferedOutputStream(receiverServer.getOutputStream());


            log.append("Sending " + file.length() + " B\n");
            int len = 0;
            while ((len = in.read(mybytearray)) > 0) {
                out.write(mybytearray, 0, len);
            }//while

            in.close();
            out.flush();
            out.close();
            receiverServer.close();

            log.append("file: +" + file.getAbsolutePath() + " is sent\n");
        } catch (IOException e) {
            log.append("senfile: failed to send to client: IP=" + clientAddr + " port=" + clientPort + "\n");
        }//catch

    }//run()
}//FileServant


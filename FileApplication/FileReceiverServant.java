package FileApplication;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * A runnable class that receives a single file via TCP.
 * @author DHT-Chord Team
 */
public class FileReceiverServant implements Runnable {

    public static int bufferSize = 1024;
    MaybeAnApp father;
    File file;
    ServerSocket hearingSocket;

    /**
     * Default Constructor.
     *
     * @param hearingSocket the socket throw where the file will be sent
     * @param file the file that will be created locally
     * @param father the application that uses this servant
     */
    public FileReceiverServant(ServerSocket hearingSocket, File file, MaybeAnApp father) {
        this.father = father;
        this.file = file;
        this.hearingSocket = hearingSocket;
    }

    /**
     * Receives one file from the " hearingSocket" that someone is sending
     * there.
     */
    public void run() {

        long start = System.currentTimeMillis();
        father.printClientAct("starting to receive file " + file.getName());
        try {
            Socket sock = hearingSocket.accept();

            // receiving the file
            byte[] mybytearray = new byte[bufferSize];

            BufferedInputStream in =
                    new BufferedInputStream(sock.getInputStream());

            BufferedOutputStream out =
                    new BufferedOutputStream(new FileOutputStream(file));
            int len = 0;
            while ((len = in.read(mybytearray)) > 0) {
                out.write(mybytearray, 0, len);
            }
            in.close();
            out.flush();
            long end = System.currentTimeMillis();
            out.close();
            sock.close();
            father.printClientAct("Transfer lasted: " + (end - start) + " msec");
            father.printUserAct("---downloaded " + file.getName() + "----");

        } catch (IOException e) {
            father.printClientAct("could not receive file");
        }


    }//run
}//FileReceiverServant


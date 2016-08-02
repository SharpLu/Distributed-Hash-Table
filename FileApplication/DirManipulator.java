package FileApplication;

import common.Hasher;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * A Class to Manipulate The Contents of a Directory. Reads filenames, attaches
 * the according Codes and maps the Codes to absolute FilePaths.
 * 
 * @author DHT-Chord Team
 */
public class DirManipulator {

    File dir;                   //The directory that this Instance manipulates
    String[] listOfFiles;       //An array of all filenames in this directory
    int[] listOfCodes;          //An array, parallel to the "listOfFiles" array with the equalevent codes of the filenames
    Map<Integer, File> fileMap;  //A map of Code-> File

//=======================-/Constructor-\====================================
    /**
     * Set the directory this Object Manipulates.
     *
     * @param directory
     */
    DirManipulator(File directory) {
        dir = directory;
        update();
    }//end String Constructor

    //=======================-/update methods\-===============================
    
    //====Update listOfFiles, listOfCodes arrays and the fileMap
    /**
     * Updates the list of files, the list of codes and the map.
     */
    public void update() {
        updateListOfFiles();
        updateListOfCodes();
        updateFileMap();
    }//end update()

    /**
     * Updates the list of files of current node.
     */
    private void updateListOfFiles() {
        listOfFiles = dir.list();
    }//updateListOfFiles

    /**
     * Updates the list of hashes of the current node.
     */
    private void updateListOfCodes() {
        listOfCodes = new int[listOfFiles.length];
        for (int i = 0; i < listOfFiles.length; i++) {
            listOfCodes[i] = Hasher.myCode(listOfFiles[i]);
            //System.out.println("DirManipulator.updateListOfCodes() :: " + listOfFiles[i] + "=" + listOfCodes[i]);
        }
    }//end updateListOfCodes()

    /**
     * Map with hashes and file references.
     */
    private void updateFileMap() {
        fileMap = new HashMap<Integer, File>();
        for (int i = 0; i < listOfFiles.length; i++) {
            File file = new File(dir + "/" + listOfFiles[i]);
            fileMap.put(listOfCodes[i], file);
        }//For All Files

    }//end updateFileMap()

    //=========================-/ Accessor Methods \-==========================
    /**
     * 
     * @return a String array with all the filenames in the directory
     */
    public String[] getListOfFiles() {
        return listOfFiles;
    }

    /**
     *
     * @return an integer array, parallel to "listOfFiles" array with the codes
     * corresponding to the filenames
     */
    public int[] getListOfCodes() {
        return listOfCodes;
    }

    /**
     *
     * @return a map of code->file
     */
    public Map<Integer, File> getFileMap() {
        HashMap<Integer, File> map = new HashMap<Integer, File>();
        map.putAll(fileMap);  // Deep Copy
        return map;
    }

    /**
     * Gives the File corresponding to a code.
     *
     * @param code
     * @return
     */
    public File getFile(int code) {
        return fileMap.get(code);
    }

    /**
     * changes the directory this object "manipulates".
     *
     * @param dir
     */
    public void setDirectrory(File dir) {
        this.dir = dir; //Set the directory this Object Manipulates
        update();
    }
}// end DirManipulator

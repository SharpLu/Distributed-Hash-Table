package KeyManager;

import common.FileLocation;

/**
 * Represents a file in the system.
 *
 * @author DHT-Chord Team
 */
public class Entry implements java.io.Serializable {

    int hash;
    FileLocation location;

    /**
     * Constructor
     *
     * @param hash
     * @param location
     */
    public Entry(int hash, FileLocation location) {
        this.hash = hash;
        this.location = location;
    }

    /**
     * Constructor
     *
     * @param e
     */
    public Entry(java.util.Map.Entry<Integer, FileLocation> e) {
        this.hash = e.getKey();
        this.location = e.getValue();
    }
    //Getters Setters

    /**
     * 
     * @return 
     */
    public int getHash() {
        return hash;
    }

    /**
     * 
     * @param hash 
     */
    public void setHash(int hash) {
        this.hash = hash;
    }

    /**
     * 
     * @return 
     */
    public FileLocation getLocation() {
        return location;
    }

    /**
     * 
     * @param location 
     */
    public void setLocation(FileLocation location) {
        this.location = location;
    }

    /**
     * 
     * @return 
     */
    @Override
    public String toString() {
        return hash + ":" + location;
    }
}

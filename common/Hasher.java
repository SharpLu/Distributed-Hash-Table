package common;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @author DHT-Chord Team
 *
 * A Class to Offer a 160bit SHA-1 Hash function and a 32bit Integer Hash
 * Function. Only Static Methods, no need to make an Instance.
 */
public class Hasher {

//====================-/myHash\-=============================
    public static int myCode(String filename) {
        byte[] sha1Hash = SHA1(filename);
        int hash = SHA1ToInt(sha1Hash);
        if (hash == 0) {
            return 1;
        } else {
            return hash;
        }
    }//end myHash

    //=====================-/ SHA1 \-==============================
    /**
     *
     * @param text
     * @return
     */
    public static byte[] SHA1(String text) {
        byte[] digest = null;
        try {
            byte[] theTextToDigestAsBytes = text.getBytes("8859_1"/* encoding */);
            MessageDigest md = MessageDigest.getInstance("SHA");
            md.update(theTextToDigestAsBytes);
            // md.update( int ) processes only the low order 8-bits. It actually expects an unsigned byte.
            digest = md.digest();
        } catch (UnsupportedEncodingException e) {
            System.out.print("Unsupported Encoding ");
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e1) {
            System.out.print("No Such algorithm ");
            e1.printStackTrace();
        }
        return digest;
    }//end SHA1

    //===============-/ Convertion Methods (private) \-========================
    /**
     * Convert last 4 bytes (4x8 bits) of SHA1 Byte array into an integer (32
     * bit).
     *
     * @param sha1Hash
     * @return
     */
    private static int SHA1ToInt(byte[] sha1Hash) {
        byte[] low4bytes = new byte[4];
        int j = 0;


        for (int i = (sha1Hash.length - 4); i < sha1Hash.length; i++) {
            low4bytes[j] = sha1Hash[i];
            j++;
        }
        return byteToInt(low4bytes);
    }//SHA1ToInt

    /**
     * Convert a byte array into an integer.
     *
     * @param b
     * @return
     */
    private static int byteToInt(byte[] b) {  //retrieved from web :-)
        int val = 0;
        for (int i = b.length - 1, j = 0; i >= 0; i--, j++) {
            val += (b[i] & 0xff) << (8 * j);
        }
        return val;
    }

    /**
     * Checks if k is between thisKey and succKey in the ring.
     *
     * @param thisKey
     * @param k
     * @param succKey
     * @return
     */
    public static boolean isBetween(int thisKey, int k, int succKey) {

        if ((thisKey < k) && (k < succKey)) {
            return true;
        }
        if ((thisKey < k) && (succKey < thisKey)) {
            return true;  //if I am the end of the ring and k is before the end of the keys
        }
        // if I am the end of the ring (hence my successor has a smaller key), but
        // k is after the end of the keys, hence k must have a smaller value than my successor.
        if ((succKey < thisKey) && (k < succKey)) {
            return true;
        } else {
            return false;
        }
    }//isBetween
}

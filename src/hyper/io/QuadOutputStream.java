package hyper.io;

import java.io.*;
import org.freehep.util.io.*;

/** With this class you can see data as a stream of <b>quads</b>.
  * A quad is a quaternari simbol, thus, just a pair of bits.
  * We define those simbols as: {ZERO, ONE, PLUS, MINUS}, that
  * is {0,1,+,-}, and, as bits, {00,01,10,11}. <p>
  * This representation was thought to match the requeriments
  * of <code>IndexCoding</code>.
  * @see hyper.coding.IndexCoding
  * @author David Gavilan
  * @version 1.0 12/03/2002
  */
public class QuadOutputStream
    extends BitOutputStream {
    	
    public static final int ZERO=0, ONE=1, PLUS=2, MINUS=3;

    public QuadOutputStream(OutputStream out) {
        super(out);
    }
    
    /**
     * Writes an unsigned value of n-quads to the output stream.
     * So, if we want to write "14", that it's 1110 in binary, we
     * will output 01 01 01 00.
     * @param value the value to output
     * @param n in how many quads
     */
    public void writeUQuads(long value, int n)
        throws IOException {

        if (n == 0) return;
        if (bitPos == 0) bitPos = 8;

        int bitNum = n;

        while (bitNum > 0) {
            while ((bitPos > 0) && (bitNum > 0)) {
  
                long or = (value & (1L << (bitNum - 1)));
                int shift = bitPos-bitNum-1;
	            if (shift < 0) {
	 	           or >>= -shift;
	            } else {
 	               or <<= shift;
  	          }
   	         bits |= or;
	
                bitNum--;
                bitPos-=2;
  
			} // while
            if( bitPos == 0 ) {
                write(bits);
                bits = 0;
                if (bitNum > 0) bitPos = 8;
            }
        }
    }
    
    /**
      * Writes just one quad to the output stream (2 bits).
      * @param quad a number in {0,1,2,3} == {ZERO, ONE, PLUS, MINUS}
      */
    public void writeQuad(int quad) throws IOException {
    	writeUBits(quad,2);
    }

}    
   
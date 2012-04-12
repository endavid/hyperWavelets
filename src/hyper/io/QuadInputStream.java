package hyper.io;

import java.io.*;
import org.freehep.util.io.*;

/** With this class you can see data as a stream of <b>quads</b>.
  * A quad is a quaternari simbol, thus, just a pair of bits.
  * We define those simbols as: {ZERO, ONE, PLUS, MINUS}, that
  * is {0,1,+,-}, and, as bits, {00,01,10,11}. <p>
  * This representation was thought to match the requeriments
  * of <code>IndexDecoding</code>.
  * @see hyper.coding.IndexDecoding
  * @author David Gavilan
  * @version 1.0 12/03/2002
  */
public class QuadInputStream
    extends BitInputStream {
    	
    public QuadInputStream(InputStream in) {
    	super(in);
    }

    /**
      * Reads just one quad from the input stream (2 bits).
      * @return a number in {0,1,2,3} == {ZERO, ONE, PLUS, MINUS}
      */    
    public int readQuad() throws IOException {
    	return (int)readUBits(2);
    }

}    	
import java.io.*;
import hyper.coding.*;
import hyper.io.*;
import org.freehep.util.io.*;

/**
  * Arithmetic Decoding
  */
public class DeAriMe {
	public static void main(String args[]) {
		try {
			
			BitInputStream bis = new BitInputStream(new FileInputStream(args[0]));
		    QuadOutputStream qos = new QuadOutputStream(new FileOutputStream(args[1]));
		    
			ArithmeticDecoding adc = new ArithmeticDecoding(bis,qos);
			adc.decode();
			bis.close();
			qos.close();
			
        } catch (Exception e) {
				System.err.println("DeAriMe: "+e);
		}
	}
}


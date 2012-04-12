import java.io.*;
import hyper.coding.*;
import hyper.io.*;
import org.freehep.util.io.*;

/**
  * Arithmetic Coding
  */
public class AriMe {
	public static void main(String args[]) {
		try {
			
			QuadInputStream qis = new QuadInputStream(new FileInputStream(args[0]));
		    BitOutputStream bos = new BitOutputStream(new FileOutputStream(args[1]));
		    
			ArithmeticCoding ac = new ArithmeticCoding(qis,bos);
			ac.code();
			qis.close();
			bos.close();
		/*	
			int high = Integer.parseInt(args[0]);
			System.out.println("bit 15 a 1?: " + ((high & 0x8000) == 0x8000));
			System.out.println("C1: " + ~high);
			System.out.println("XOR 0x4000 = " + (high^0x4000));
			*/
        } catch (Exception e) {
				System.err.println("AriMe: "+e);
		}
	}
}

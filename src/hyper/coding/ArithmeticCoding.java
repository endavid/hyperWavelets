package hyper.coding;

import java.util.Vector;
import java.io.*;
import hyper.io.*;
import org.freehep.util.io.*;

/** Class implementing the Arithmetic Coding process.
  * Now, it just implements coding of 4-simbol codewords.
  * @author Cristina Fernandez, David Gavilan
  */
public class ArithmeticCoding {
	
	/** Input Stream is made up with these 4 simbols: {0,1,+,-}
	  * @see hyper.io.QuadOutputStream
	  */
	protected QuadInputStream qis;
	/** We just write bits as output */
	protected BitOutputStream bos;
	protected int low;
	protected int high;
	protected long underflow;	
	protected double[] prob;
	
	
	/** This is the default constructor, but it's not declared public, as we also need
	  * that the user defines an input and an output.
	  */
	protected ArithmeticCoding(int nsimbols) {
		prob = new double[nsimbols+1]; // habra que inicializar despues las tablas!
		low = 0;
		high = 0xffff;
		underflow = 0;
		switch (nsimbols) {
			case 3:
				iniTableProb(new double[] {0.4, 0.4, 0.2});
				break;
			case 4:
				iniTableProb(new double[] {0.35, 0.35, 0.15, 0.15});
				break;
			default:
				iniTableProb(nsimbols);
		}		
	}
	
	
	/** Default -> quad => 4 simbols */
	public ArithmeticCoding(QuadInputStream qis, BitOutputStream bos) {
		this(qis,bos,4);
	}
	
	/** Public constructor with default probabilities inicialization.
	  * @param qis Input. A={0,1,+,-}
	  * @param bos Output. A={0,1}
	  */
	public ArithmeticCoding(QuadInputStream qis, BitOutputStream bos,int nsimbols) {
		this(nsimbols);
		this.qis = qis;
		this.bos = bos;
	}

	/** Public constructor with another accumulated probabilities table.
	  * @param qis Input. A={0,1,+,-}
	  * @param bos Output. A={0,1}
	  * @param p are the simbol probabilities (not the accumulated ones!)
	  */
	public ArithmeticCoding(QuadInputStream qis, BitOutputStream bos, double[] p) {
		this(p.length);
		this.qis = qis;
		this.bos = bos;
		iniTableProb(p);
	}

	
	public void code() throws IOException {
		try {
			for (;;) { // hasta fin de fichero
				int simbol = qis.readQuad();
				encodeSymbol(simbol);
			}
		} catch (EOFException e) {
			flushCoding();
			// fin de fichero
		} 
	}
	
	/** de momento, empezamos con simbolos equiprobables */
	protected void iniTableProb(int nsimbols) {
		prob[0]=0f;
		for (int i=1;i<=nsimbols;i++) prob[i]=prob[i-1]+1f/(float)nsimbols;
	}

	/** We calculate accumulated probabilities given initial simbol probabilities. */
	protected void iniTableProb(double[] p) {
		prob[0]=0f;
		for (int i=0;i<p.length;i++) prob[i+1]=prob[i]+p[i];
	}
	
	/**
	  * @param simbol a quad, that is, something between 0 and 3 (simbol 1 to 4)
	  */
	protected void encodeSymbol(int simbol) throws IOException {
	    long range = (long) ( high-low ) + 1;
 	   high = low + (int)((double)range * prob[simbol+1] );
   	 low = low + (int)( (double)range * prob[simbol]);

   	 //System.out.println("e: high = "+high+ " low = "+low+" range = "+range+" simbol:"+simbol);
   	 recalculateHL();
   	 //System.out.println("encoded: high = "+high+ " low = "+low);
	}

	
	
	protected void recalculateHL() throws IOException {
	    for ( ; ; )
 	   {
  	      if ( ( high & 0x8000 ) == ( low & 0x8000 ) )
   	     {
    	        bos.writeBitFlag((high & 0x8000)==0x8000);
     	       while ( underflow > 0 )
      	      {
       	         bos.writeBitFlag((~high & 0x8000)==0x8000);
        	        underflow--;
         	   }
        	}
	        else if ( ( (low & 0x4000)==0x4000 ) && ( (high & 0x4000)==0 ))
 	       {
  	          underflow += 1;
   	         low &= 0x3fff;
    	        high |= 0x4000;
     	   }
	        else
 	           return ;
 	       // al desplazar bits, como solo nos interesan 2 bytes, hay q aplicar una mascara
 	       // para cargarse los bits que se van yendo hacia la izquierda 	           
  	      low <<= 1; low &= 0xffff;
   	     high <<= 1; high &= 0xffff;
    	    high |= 1;
    	}

	}
	
	protected void flushCoding() throws IOException {
 	   bos.writeBitFlag((low & 0x4000)==0x4000);
  	  underflow++;
   	 while ( underflow-- > 0 ) {
    	    bos.writeBitFlag((~low & 0x4000)==0x4000);
		}
		// 4 bits mas, por algun rollo del decoder que no entiendo ...
		bos.writeUBits(0,4);
	}

}

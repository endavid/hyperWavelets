package hyper.coding;

import java.util.Vector;
import java.io.*;
import hyper.io.*;
import org.freehep.util.io.*;

/** Class implementing the Arithmetic Decoding process.
  * Now, it just implements decoding to 4-simbol codewords.
  * @author Cristina Fernandez, David Gavilan
  */
  
public class ArithmeticDecoding {
	
	/** Input Stream is made up with these 4 simbols: {0,1,+,-}
	  * @see hyper.io.QuadOutputStream
	  */
	protected QuadOutputStream qos;
	/** We just write bits as output */
	protected BitInputStream bis;
	protected int low;
	protected int high;
	protected int code;
	/** Message length */
	protected long nquads;
	protected double[] prob;  
		/** This is the default constructor, but it's not declared public, as we also need
	  * that the user defines an input and an output.
	  */
	protected ArithmeticDecoding(int nsimbols) {
		prob = new double[nsimbols+1]; // habra que inicializar despues las tablas!
		low = 0;
		high = 0xffff;
		switch (nsimbols) {
			case 3:
				iniTableProb(new double[] {0.4, 0.4, 0.2});
				break;
			case 4:
				iniTableProb(new double[] {0.25, 0.25, 0.25, 0.25});
				break;
			default:
				iniTableProb(nsimbols);
		}		
	}
	
	
	/** Default -> quad => 4 simbols */
	public ArithmeticDecoding(BitInputStream bis, QuadOutputStream qos) throws IOException {
		this(bis,qos,4);
	}
	
	/** Public constructor with default probabilities inicialization.
	  * @param qis Input. A={0,1}
	  * @param bos Output. A={0,1,+,-}
	  */
	public ArithmeticDecoding(BitInputStream bis, QuadOutputStream qos,int nsimbols) throws IOException {
		this(nsimbols);
		this.bis = bis;
		this.qos = qos;
		
		//nquads = bis.readUBits(64);
		
		// llegir valor
		code = (int) bis.readUBits(16);
	}

	/** Public constructor with another accumulated probabilities table.
	  * @param qis Input. A={0,1,+,-}
	  * @param bos Output. A={0,1}
	  * @param p are the simbol probabilities (not the accumulated ones!)
	  */
	public ArithmeticDecoding(BitInputStream bis, QuadOutputStream qos, double[] p) throws IOException	{
		this(p.length);
		this.bis = bis;
		this.qos = qos;

		iniTableProb(p);
		//nquads = bis.readUBits(64);
		code = (int) bis.readUBits(16);		
	}

	
	public void decode() throws IOException {
		try {
			for (;;) { // hasta fin de simbolos (<nquads)
				decodeSymbol();		
			}
		} catch (EOFException e) {
			//flushCoding();
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
	protected void decodeSymbol() throws IOException {
	    long range = (long) ( high-low ) + 1;
        double probab = ((double)code - (double)low +1f)/(double)range;
        //System.out.println("decode: code = "+code+" range = "+range+" prob = "+probab);
        int i = 1;
        if (probab < 1f) {
	        while(prob[i] < probab) i++;	        
	    } else i = prob.length-1;
        //System.out.println("decode: simbol(i) = "+i);
        recalculateHLC(i,range);
        qos.writeQuad(i-1); // simbol 1 == quad(00), simbol 2 == quad(01) ...        
	}

	
	
	protected void recalculateHLC(int simbol, long range) throws IOException {
 	   high = low + (int)((double)range * prob[simbol] );
   	 low = low + (int)( (double)range * prob[simbol-1]);
   	 for ( ; ; )
 	   {
 	   	// If the MSDigits match, the bits will be shifted out
  	      if ( ( high & 0x8000 ) == ( low & 0x8000 ) )
   	     {
        	}
	        else if ( ( (low & 0x4000)==0x4000 ) && ( (high & 0x4000)==0 ))
 	       {
   	         low &= 0x3fff;
    	        high |= 0x4000;
    	        code ^=0x4000;
     	   }
	        else
 	           return ;
 	       // al desplazar bits, como solo nos interesan 2 bytes, hay q aplicar una mascara
 	       // para cargarse los bits que se van yendo hacia la izquierda
  	      low <<= 1; low &= 0xffff;
   	     high <<= 1; high &= 0xffff;
    	    high |= 1;
    	    code <<=1; code &= 0xffff;

    	    code |= (int)bis.readUBits(1);    	    
    	}

	}
	
}

package hyper.coding;

import java.util.Vector;
import javax.media.jai.*;
import javax.media.jai.iterator.*;
import java.io.*;
import hyper.io.*;

/** An implementation of Index Coding algorithm that handles JAI <code>PlanarImages</code>.
  * @author David Gavilan
  * @version 1.0 12/03/2002
  */
public class IndexCodingJAI extends IndexCoding {
	
	/** Creates an instance of the algorithm.
	  * It outputs image dimensions and then the initial threshold to the stream.
	  * @param pim the image we are going to code
	  * @param level number of levels of wavelet transform.
	  * @param qos the output stream	  
	  */	
	public IndexCodingJAI(PlanarImage pim, int level, QuadOutputStream qos) throws IOException {
		
		int bands = pim.getSampleModel().getNumBands(); 
		int height = pim.getHeight(), tileHeight=pim.getTileHeight(); 
		int width = pim.getWidth(), tileWidth=pim.getTileWidth();
		int size=width*height;
    
		int subBands = 3*level+1;

		// first we save the image dimensions (short-type should be enough)
		qos.writeUBits(width,16);
		qos.writeUBits(height,16);
		//qos.writeUBits(tileWidth,16);
		//qos.writeUBits(tileHeight,16);
		qos.writeUBits(bands,8);
			
		ICS = new Vector(size);
		SCS = new Vector(size);
		TPS = new Vector(size>>1);
		S = new Vector(size>>1);
		
		this.qos = qos;

		RandomIter iter = RandomIterFactory.create(pim, null);
		
		for (int band=0;band<bands;band++) {
			for(int y=0;y<height;y++){
				for (int x=0;x<width;x++) {
					ICS.add(new Integer(iter.getSample(x,y,band)));
				}
			}
		}
		
		T = findThreshold();
		
		this.qos.writeUBits(T,8);

	}
}

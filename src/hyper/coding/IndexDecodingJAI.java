package hyper.coding;

import java.awt.*;
import java.awt.image.*;
import java.awt.image.renderable.*;
import javax.media.jai.*;
import java.awt.color.*;
import java.awt.Transparency;
import javax.media.jai.iterator.*;
import java.io.*;
import java.util.Vector;
import java.util.BitSet;
import hyper.io.*;

/** An implementation of Index Decoding algorithm that builds up JAI <code>PlanarImages</code>.
  * @author David Gavilan
  * @version 1.0 12/03/2002
  */
public class IndexDecodingJAI extends IndexDecoding {

	/** Image attributes */
	private int width, height, bands;
	/** Output Image */
	private TiledImage outImage;

	/** Decoding instance.
	  * @param level number of levels of wavelet transform.
	  * @param qis Input stream.
	  */
	public IndexDecodingJAI(int level, QuadInputStream qis) throws IOException {
		//int tileWidth,tileHeight;
		// we read the image dimensions
		width = (int)qis.readUBits(16);
		height = (int)qis.readUBits(16);
		//tileWidth = (int)din.readUBits(16);
		//tileHeight = (int)din.readUBits(16);      
		bands = (int)qis.readUBits(8);

		size = width*height*bands;

		IS = new Vector(size);
		bitSet = new BitSet(size);
		this.qis = qis;
		T = (int)qis.readUBits(8);
	}

	/** Decodes an image */	
	public PlanarImage decodeJAI() throws IOException {


		// ------------------------------------------- construimos la imagen		
		int tileWidth=width, tileHeight=height;
		
		int[] order = new int[bands];
		for (int i=0;i<bands;i++) order[i]=i;
		ComponentSampleModel csm = new ComponentSampleModel(
  	      DataBuffer.TYPE_SHORT, tileWidth, tileHeight,
			tileWidth*bands, bands, order);

		for (int i=0;i<bands;i++) order[i]=16;
		ColorSpace cs;
		switch (bands) {
		case 1:
	    	cs = ColorSpace.getInstance(ColorSpace.CS_GRAY);
	    	break;
		case 3:
	   	 cs = ColorSpace.getInstance(ColorSpace.CS_sRGB);
	    	break;
		default:
	    	// F = 15 component
	    	cs = ColorSpace.getInstance(ColorSpace.TYPE_FCLR);
		}
		ComponentColorModel ccm = new ComponentColorModel(
        	cs, order,
			false, false, Transparency.OPAQUE, DataBuffer.TYPE_SHORT);
		
		outImage = new TiledImage(0, 0, 
           width, height, 0, 0, 
	   	csm, ccm);
	   	
		// --------------------------------------------------- decodificamos
		
		decode(); // ya redefinimos aqui el setValue, asi que todo el proceso es igual
							
		return outImage;
	}
	
	
	protected void setValue(int pos, int sign, int tt){		
		int s = (sign==QuadOutputStream.PLUS)?1:-1;

		int b = (int)(pos/(width*height));
		pos -= b*width*height;
		int y = (int)(pos / width);
		int x = pos % width;
				
	    outImage.setSample(x,y,b,s*tt);
	}
	
	protected void setValue(int pos,int val){

		int b = (int)(pos/(width*height));
		pos -= b*width*height;
		int y = (int)(pos / width);
		int x = pos % width;
				
	    outImage.setSample(x,y,b,val);

	}

	public int getValue(int pos){
		int b = (int)(pos/(width*height));
		pos -= b*width*height;
		int y = (int)(pos / width);
		int x = pos % width;
		
	    return outImage.getSample(x,y,b);
	}

	
}
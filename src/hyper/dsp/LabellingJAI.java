/**
 * LabellingJAI
 * 21-Nov-2001
 * @author David Gavilan
 */

package hyper.dsp;

import java.awt.*;
import java.awt.image.*;
import java.awt.image.renderable.*;
import javax.media.jai.*;
import java.awt.color.*;
import java.awt.Transparency;
import java.util.Vector;
import javax.media.jai.iterator.*;
import java.io.*;
import org.freehep.util.io.*;

/**
  * Class that performs the labelling of lattices on quantized JAI images.
  * <P>It uses its parent's methods to code/decode each lattice.
  * @author David Gavilan, Joan Serra
  " @see <a href="http://java.freehep.org/lib/freehep/api/index.html">FreeHep API</a>
  */
public class LabellingJAI extends Labelling {

    public LabellingJAI() {
    }

    public LabellingJAI(String filename) throws FileNotFoundException {
		super(filename);
    }

    /** Assuming bidimensional lattices, we label (index+norm) each channel
      * separately in subband order (each subband being the result of the
      * wavelet transformation)
      * @param in the input <code>PlanarImage</code>
      * @param level number of levels of the wavelet transform
      * @param lattices a <code>Vector</code> containing the type of lattice per subband
      * @see hyper.dsp.WaveletOpImage to learn more about subbands and order convention
      * @see hyper.dsp.ParamLattice to learn about Lattices      
      */
    public void imageLabelling(PlanarImage in, int level, Vector[] lattices) 
    throws IOException {
	int bands = in.getSampleModel().getNumBands(); 
	int height = in.getHeight(), tileHeight=in.getTileHeight(); 
	int width = in.getWidth(), tileWidth=in.getTileWidth();
    
	int subBands = 3*level+1;

	// first we save the image dimensions (short-type should be enough)
	dout.writeUBits(width,16);
	dout.writeUBits(height,16);
	dout.writeUBits(tileWidth,16);
	dout.writeUBits(tileHeight,16);
	dout.writeUBits(bands,8);

	//short[][] indexTable = new short[2][bands *
	//				    numberOfVectors(level,width,height,
	//						    lattices)];

	System.out.println("Labelling - #vectors: "+
		     numberOfVectors(level,width,height,lattices));
	// used to access the source image
	RandomIter iter = RandomIterFactory.create(in, null);
  
	int idx=0;
	//int ix = 0;
       
	// we send each tile separately
	for (int tileYOff = 0; tileYOff<height; tileYOff+=tileHeight)
	for (int tileXOff = 0; tileXOff<width; tileXOff+=tileWidth)
	// from the time being, we process each channel separately
	for (int band=0; band < bands; band++) { 
	    int subh = tileHeight >> level;
	    int subw = tileWidth >> level;
	    // subband 0
	    idx=0;
	    int lw,lh;
	    ParamLattice pml = (ParamLattice)lattices[band].get(idx);
	    // we don't label subband 0 if it's not necessary,
	    // so it won't require 9 bytes, just 1
	    if (pml.isBasic()) {
	      for (int j=0;j<subh;j++)
	       for (int i=0;i<subw;i++)
		 dout.write((byte)iter.getSample(i+tileXOff,j+tileYOff,band));
	    } else {
	       lw=pml.getWidth(); lh=pml.getHeight();	    
	       for (int j=0;j<subh;j+=lh)
		for (int i=0;i<subw;i+=lw) {
		    short[] v=vectorFromLattice(iter,
						i+tileXOff,
						j+tileYOff,
						lw,lh,band);
		    // we calculate the index and distance
		    long b=indexAlterLong(v);
		    int m=distanceL1(v);
		    dout.writeUBits(m,8);
		    // dimensio i norma determinen un nombre minim de bits (max index)		    
		    dout.writeUBits(b,maxBits(pml.getSize(),m));
		}
	    }
	    //System.out.println("ix: "+ix);
	    idx++;

	    // the other subbands
	    for (int lev=0;lev<level;lev++) {
		// upper right
		pml = (ParamLattice)lattices[band].get(idx);
		lw=pml.getWidth(); lh=pml.getHeight();
		for (int j=0;j<subh;j+=lh)
		    for (int i=0;i<subw;i+=lw) {
			short[] v=vectorFromLattice(iter,
						    tileXOff+i+subw,
						    tileYOff+j,
						    lw,lh,band);
			// we calculate the index and distance
			//indexTable[0][ix]=(short)index(v);
			//indexTable[1][ix++]=(short)distanceL1(v);
		    long b=indexAlterLong(v);
		    int m=distanceL1(v);
		    dout.writeUBits(m,8);
		    dout.writeUBits(b,maxBits(pml.getSize(),m));
			//if (mm==195) 
			//    System.out.println("bb: "+kk+"mm: "+mm+"v: "+verV(v));
		    }
		//System.out.println("ix: "+ix);
		idx++;

		// bottom left
		pml = (ParamLattice)lattices[band].get(idx);
		lw=pml.getWidth(); lh=pml.getHeight();
		for (int j=0;j<subh;j+=lh)
		    for (int i=0;i<subw;i+=lw) {
			short[] v=vectorFromLattice(iter,
						    tileXOff+i,
						    tileYOff+j+subh,
						    lw,lh,band);
			// we calculate the index and distance
			//indexTable[0][ix]=(short)index(v);
			//indexTable[1][ix++]=(short)distanceL1(v);
		    long b=indexAlterLong(v);
		    int m=distanceL1(v);
		    dout.writeUBits(m,8);
		    dout.writeUBits(b,maxBits(pml.getSize(),m));
		    }
		//System.out.println("ix: "+ix);
		idx++;
	    	
		// bottom right
		pml = (ParamLattice)lattices[band].get(idx);
		lw=pml.getWidth(); lh=pml.getHeight();
		for (int j=0;j<subh;j+=lh)
		    for (int i=0;i<subw;i+=lw) {
			short[] v=vectorFromLattice(iter,
						    tileXOff+i+subw,
						    tileYOff+j+subh,
						    lw,lh,band);
			// we calculate the index and distance			
			//indexTable[0][ix]=(short)index(v);
			//indexTable[1][ix++]=(short)distanceL1(v);
		    long b=indexAlterLong(v);
		    int m=distanceL1(v);
		    dout.writeUBits(m,8);
		    dout.writeUBits(b,maxBits(pml.getSize(),m));
		    }
		//System.out.println("ix: "+ix);
		idx++;

		subw<<=1; subh<<=1;
	    }

	}

	//return indexTable;
    } // end imageLabelling

    /** We decode each (norm,index) pair to rebuild the image. <p>
      * Due to some bug, it can freeze your app when decoding wrong coded vectors.
      * This problem arises because of <code>vector</code> function
      * @param din the <code>DataInput</code> file
      * @param level number of levels of the wavelet transform
      * @param lattices a list of lattices parameters
      * @return a <code>PlanarImage</code> containing the decoded image
      *         (ready to dequantize)
      * @see hyper.dsp.Labelling#vector
      */
    public PlanarImage imageDecoding(BitInputStream din,
				     int level, Vector[] lattices) 
	throws IOException {

	int width,height,bands,tileWidth, tileHeight;        
	// we read the image dimensions
	width = (int)din.readUBits(16);
	height = (int)din.readUBits(16);
	tileWidth = (int)din.readUBits(16);
	tileHeight = (int)din.readUBits(16);      
	bands = (int)din.readUBits(8);

	System.out.println("decoding "+width+"x"+height+"x"+bands
			   +" ("+tileWidth+"x"+tileHeight+") ...");

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
		
	TiledImage outImage = new TiledImage(0, 0, 
           width, height, 0, 0, 
	   csm, ccm);

  	int idx=0;	

	// we receive each tile separately
	for (int tileYOff = 0; tileYOff<height; tileYOff+=tileHeight)
	for (int tileXOff = 0; tileXOff<width; tileXOff+=tileWidth)
	// from the time being, we process each channel separately
	for (int band=0; band < bands; band++) { 
	    int subh = tileHeight >> level;
	    int subw = tileWidth >> level;
	    // subband 0
	    idx=0;
	    //System.out.println("Decoding subband 0");
	    int lw,lh;
	    ParamLattice pml = (ParamLattice)lattices[band].get(idx);
	    // if it's basic, just read data, don't decode
	    if (pml.isBasic()) {
	      for(int j=0;j<subh;j++)
		for (int i=0;i<subw;i++) {
		    int m = (int)din.readUBits(8);
		    outImage.setSample(tileXOff+i,tileYOff+j,band,m);
		}		
	    } else {
	      lw=pml.getWidth(); lh=pml.getHeight();	   
	      for (int j=0;j<subh;j+=lh)
		for (int i=0;i<subw;i+=lw) {
		    int m=(int)din.readUBits(8);
		    long b = din.readUBits(maxBits(pml.getSize(),m));

		    short[] v=vector(m,b,lw*lh);
		    //System.out.println("decoded v: "+verV(v));
		    expandVector(outImage,tileXOff+i,tileYOff+j,lw,lh,band,v);
		}
	    }
	    idx++;
	    
	    // the other subbands
	    for (int lev=0;lev<level;lev++) {
		// upper right
		//System.out.println("Decoding subband "+idx);
		pml = (ParamLattice)lattices[band].get(idx);
		lw=pml.getWidth(); lh=pml.getHeight();
		for (int j=0;j<subh;j+=lh)
		    for (int i=0;i<subw;i+=lw) {
			    int m=(int)din.readUBits(8);
			    long b = din.readUBits(maxBits(pml.getSize(),m));
		    	
			//System.out.println("v("+m+","+b+")"); 		
			short[] v=vector(m,b,lw*lh);
			//System.out.println("decoded v: "+verV(v));
			expandVector(outImage,tileXOff+i+subw,
				     tileYOff+j,lw,lh,band,v);
		    }
		idx++;

		// bottom left
		//System.out.println("Decoding subband "+idx);
		pml = (ParamLattice)lattices[band].get(idx);
		lw=pml.getWidth(); lh=pml.getHeight();
		for (int j=0;j<subh;j+=lh)
		    for (int i=0;i<subw;i+=lw) {
			    int m=(int)din.readUBits(8);
			    long b = din.readUBits(maxBits(pml.getSize(),m));
			short[] v=vector(m,b,lw*lh);
			expandVector(outImage,tileXOff+i,
				     tileYOff+j+subh,lw,lh,band,v);
		    }
		idx++;
	    	
		// bottom right
		//System.out.println("Decoding subband "+idx);
		pml = (ParamLattice)lattices[band].get(idx);
		lw=pml.getWidth(); lh=pml.getHeight();
		for (int j=0;j<subh;j+=lh)
		    for (int i=0;i<subw;i+=lw) {
			    int m=(int)din.readUBits(8);
			    long b = din.readUBits(maxBits(pml.getSize(),m));
		    	
			short[] v=vector(m,b,lw*lh);
			expandVector(outImage,tileXOff+i+subw,
				     tileYOff+j+subh,lw,lh,band,v);

		    }
		idx++;

		subw<<=1; subh<<=1;
	    }
	}
    
	return outImage;      
  	
    } // end imageDecoding

    /** Flattens a 2D w*h lattice into a 1D wh vector */
    protected short[] vectorFromLattice(RandomIter it, int x, int y,
					int w, int h, int band) {
	short[] v=new short[w*h];
	int k=0;

	for (int j=0;j<h;j++)
	    for (int i=0;i<w;i++)
		v[k++]=(short)it.getSample(i+x,j+y,band);		

	return v;
    }

    protected void expandVector(TiledImage out,
				int x, int y, int w, int h,
				int band, short[] v) {
	//System.out.println("aagg! : "+verV(v));
      
	int k=0;
	for (int j=0;j<h;j++)
	    for (int i=0;i<w;i++)
		out.setSample(x+i,y+j,band,v[k++]);
//		out.setSample(i,j,band,(short)128);	
    }
	


    public static int numberOfVectors(int level, int width,int height,
				      Vector[] lattices) {	
	int total=0;
	for (int k=0;k<lattices.length;k++) {

	    int subw=width>>level;
	    int subh=height>>level;
	    // subband 0
	    int idx=0;
	    ParamLattice pml = (ParamLattice)lattices[k].get(idx++);
	    int lw=pml.getWidth(), lh=pml.getHeight();
	    int sum=vectorsPerSubBand(subw,subh,lw,lh);	

	    // the other subbands
	    for (int lev=0;lev<level;lev++) {
		// upper right
		pml=(ParamLattice)lattices[k].get(idx++);
		sum+=vectorsPerSubBand(subw,subh,pml.getWidth(),pml.getHeight());
		// bottom left
		pml=(ParamLattice)lattices[k].get(idx++);
		sum+=vectorsPerSubBand(subw,subh,pml.getWidth(),pml.getHeight());
		// bottom right
		pml=(ParamLattice)lattices[k].get(idx++);
		sum+=vectorsPerSubBand(subw,subh,pml.getWidth(),pml.getHeight());

		subw<<=1; subh<<=1;
	    }
	    
	    total+=sum;
	}

	return total;
    }

    public static int vectorsPerSubBand(int width, int height, int lw, int lh) {
	return (width/lw)*(height/lh);
    }

    public static long read7B(DataInput din) throws IOException {
	byte b,c,d,e,f,g,h;
	b=din.readByte();
	c=din.readByte();
	d=din.readByte();
	e=din.readByte();
	f=din.readByte();
	g=din.readByte();
	h=din.readByte();
	return (((long)(b & 0xff) << 48) |
		((long)(c & 0xff) << 40) |
		((long)(d & 0xff) << 32) |
		((long)(e & 0xff) << 24) |
		((long)(f & 0xff) << 16) |
		((long)(g & 0xff) << 8) |
		((long)(h & 0xff)));
    }

}

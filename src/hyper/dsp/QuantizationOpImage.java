package hyper.dsp;

import java.awt.*;
import java.awt.image.*;
import java.awt.color.*;
import javax.media.jai.*;
import java.util.Map;
import java.util.Vector;

/**
  * OpImage implementation for "Quantization" operator.
  *
  * <p> Performs a quantization of a float image, regarding the algorism chosen.
  * <p><UL>
  * <LI> If the algorism is <code>UNIFORM</code>, it just multiplies each pixel by
  *      some scale value.
  * <LI> In <code>SBUNIFORM</code>, that value depends on the subband (regarding
  *      Wavelet Tranform). We'll need a Vector with Ingeter values.
  * <LI> In <code>LATTICE</code>, we'll receive a Vector of
  *      <code>ParamLattice</code>s defining a Lattice per subband with different
  *      size and scale values. The quantization algorism will change depending
  *      on the Lattice type.
  * </UL>
  * @author David Gavilan
  */
public class QuantizationOpImage extends PointOpImage {
   
   public static final int UNKNOWN=0, UNIFORM = 1, SBUNIFORM = 2, LATTICE = 3;
   
   protected int algorism;
   protected int level;
   protected Vector[] coefs;

   /**
     * Layouts the destination image as a short-type image with the same
     * attributes as the source image
     * @param src the source image
     */
   protected static final ImageLayout setLayoutS(RenderedImage src){
    int bands = src.getSampleModel().getNumBands(); 
    int height = src.getHeight(), tileHeight=src.getTileHeight(); 
    int width = src.getWidth(), tileWidth=src.getTileWidth();

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

    ImageLayout il = new ImageLayout(
		   src.getTileGridXOffset(),
                   src.getTileGridYOffset(),
                   src.getTileWidth(),
		   src.getTileHeight(),
		   csm, ccm);


    return il;
   }
   
    public QuantizationOpImage(RenderedImage s, ImageLayout il, Map c, boolean b) {
	super(s,il,c,b);
    }
   public QuantizationOpImage(RenderedImage source,ImageLayout layout,
          Map config, String algorism, int level, Vector[] coefs) {
	  
      //super(source, layout, config, true);
      super(source, setLayoutS(source), config, true);
      if (algorism.equals("uniform")) this.algorism=UNIFORM;
      else if (algorism.equals("sbuniform")) this.algorism=SBUNIFORM;
      else if (algorism.equals("lattice")) this.algorism=LATTICE;
      else this.algorism=UNKNOWN;
      
      this.level = level;
      this.coefs = coefs;
   }
   
   public void computeRect(Raster sources[], WritableRaster dest,
          Rectangle destRect) {
      
      RasterFormatTag[] formatTags = getFormatTags();
      
     // get destination accessor
     RasterAccessor dst = new RasterAccessor(dest, destRect, formatTags[1],
         getColorModel());

     /*
     The data type of the RasterAccessor is determined to be the highest precision
     data type among the set of all sources and the destination. Therefore given
     FLOAT sources and a SHORT destination, the RasterAccessor will have type FLOAT.
     Consequently your computeRect() implementation will need to use floating point
     processing.
     */	

     // source accessor
     RasterAccessor src = new RasterAccessor(sources[0],
        mapDestRect(destRect, 0), formatTags[0],
	getSourceImage(0).getColorModel());

     if (src.getDataType() != DataBuffer.TYPE_FLOAT) {
        throw new IllegalArgumentException("Float datatype sources only.");
     }

     // get destination dimensions (of a TILE!)
     int dwidth = dst.getWidth();
     int dheight = dst.getHeight();
     int dnumBands = dst.getNumBands();

     // get destination data array references and strides
     float dstDataArrays[][] = dst.getFloatDataArrays();
     //short dstDataArrays[][] = dst.getShortDataArrays();
     int dstBandOffsets[] = dst.getBandOffsets();
     int dstPixelStride = dst.getPixelStride();
     int dstScanlineStride = dst.getScanlineStride();
     
     // get source data array references and strides     
     float srcDataArrays[][] = src.getFloatDataArrays();
     int srcBandOffsets[] = src.getBandOffsets();
     int srcPixelStride = src.getPixelStride();
     int srcScanlineStride = src.getScanlineStride();
     
     /*
     System.out.println("WaveletOpImage: dst:"+dwidth+"x"+dheight+"x"+
        dnumBands+", src: " + src.getWidth()+"x"+src.getHeight()+"x"+
	src.getNumBands());
     System.out.println("WaveletOpImage: dstStr:"+dstPixelStride+"x"+
        dstScanlineStride+", srcStr: " +srcPixelStride+"x"+srcScanlineStride);
     */

     for (int k=0;k<dnumBands;k++) {
       float dstData[] = dstDataArrays[k];
       float srcData[] = srcDataArrays[k];
       int srcScanlineOffset = srcBandOffsets[k];
       int dstScanlineOffset = dstBandOffsets[k];
       
       switch (algorism) {
         case UNIFORM:
            uniform(srcData, dstData,
               srcScanlineOffset, dstScanlineOffset,
	       srcPixelStride, dstPixelStride,
	       srcScanlineStride, dstScanlineStride,
	       dwidth, dheight);
	    break;
         case SBUNIFORM:
            sbuniform(srcData, dstData,
               srcScanlineOffset, dstScanlineOffset,
	       srcPixelStride, dstPixelStride,
	       srcScanlineStride, dstScanlineStride,
	       dwidth, dheight, k);
	    break;
         case LATTICE:
            lattice(srcData, dstData,
               srcScanlineOffset, dstScanlineOffset,
	       srcPixelStride, dstPixelStride,
	       srcScanlineStride, dstScanlineStride,
	       dwidth, dheight, k);
	    break;	    
       }    
	       
     }
     
     
     if (dst.isDataCopy()) {
       dst.clampDataArrays();
       dst.copyDataToRaster();
     }
	 
   }
   
   /** Uniform Quantization */
   protected void uniform(float srcData[], float dstData[],
           int srcScanlineOffset, int dstScanlineOffset,
           int srcPixelStride, int dstPixelStride,
	   int srcScanlineStride, int dstScanlineStride,
	   int dwidth, int dheight) {

       float val = (float)level;
       float half = val / 2f;
       for (int j = 0; j<dheight;j++) {
         int srcPixelOffset = srcScanlineOffset;
	 int dstPixelOffset = dstScanlineOffset;
	 
	 for (int i = 0; i<dwidth; i++) {
	 
	   dstData[dstPixelOffset] = (short)((srcData[srcPixelOffset]+half)/val);
	   
	   srcPixelOffset += dstPixelStride;
	   dstPixelOffset += dstPixelStride;
	   
	 }
	 srcScanlineOffset += srcScanlineStride;
	 dstScanlineOffset += dstScanlineStride;	 
       }	   
   }
   
   
   /** SubBand Uniform Quantization */
   protected void sbuniform(float srcData[], float dstData[],
           int srcScanlineOffset, int dstScanlineOffset,
           int srcPixelStride, int dstPixelStride,
	   int srcScanlineStride, int dstScanlineStride,
	   int dwidth, int dheight, int band) {

    int subBands = 3*level+1;	   
    if (coefs == null) iniSBCoefs();
    else if (coefs[0].size() < subBands) iniSBCoefs();
    else iniSBCoefs(coefs);

    float val, half;
    int height = dheight >> level;
    int firsty = 0, k=0, lev=0, idx=0;
    for (int jl=0; jl<=level; jl++) {
       
       for (int j = firsty; j<height;j++) {
         int srcPixelOffset = srcScanlineOffset;
	 int dstPixelOffset = dstScanlineOffset;
	 
	 int width = dwidth >> level;
	 int firstx = 0;
	 for (int il = 0; il<=level; il++) {
	  lev=(il>jl)?il:jl;
	  k=(il>jl)?0:(il<jl)?1:2;
	  idx = lev==0?0:3*(lev-1)+k+1;
 	  val = ((Float)coefs[band].get(idx)).floatValue(); half = val/2f;
	  for (int i = firstx; i<width; i++) {
	 
	   dstData[dstPixelOffset] = (srcData[srcPixelOffset]+half)/val;
	   
	   srcPixelOffset += dstPixelStride;
	   dstPixelOffset += dstPixelStride;
	   
	  }
	  firstx = width;
	  width <<=1;

	 }
	 srcScanlineOffset += srcScanlineStride;
	 dstScanlineOffset += dstScanlineStride;	 
       }
       firsty = height;
       height <<=1;
    }
   }

   /** Lattice Quantization */
   protected void lattice(float srcData[], float dstData[],
           int srcScanlineOffset, int dstScanlineOffset,
           int srcPixelStride, int dstPixelStride,
	   int srcScanlineStride, int dstScanlineStride,
	   int dwidth, int dheight,int band) {

    int subBands = 3*level+1;	   
    if (coefs == null) iniLCoefs();
    else if (coefs[0].size() < subBands) iniLCoefs();
    else iniLCoefs(coefs);

    int height = dheight >> level;
    int width = dwidth >> level;
//    int firsty = 0, firstx=0, k=0, lev=0, idx=0;

    // subband 0
    int idx=0;
    ParamLattice pml = (ParamLattice)coefs[band].get(idx);
    subBandLattice(
	   srcData, dstData,
           srcScanlineOffset, dstScanlineOffset,
           srcPixelStride, dstPixelStride,
	   srcScanlineStride, dstScanlineStride,
	   width, height,
	   0, 0,
	   pml); idx++;

    // the other subbands

    for (int lev=0;lev<level;lev++) {
	// upper right
	pml = (ParamLattice)coefs[band].get(idx);
	subBandLattice(
	   srcData, dstData,
           srcScanlineOffset, dstScanlineOffset,
           srcPixelStride, dstPixelStride,
	   srcScanlineStride, dstScanlineStride,
	   width, height,
	   width, 0,
	   pml); idx++;
	// bottom left
	pml = (ParamLattice)coefs[band].get(idx);
	subBandLattice(
	   srcData, dstData,
           srcScanlineOffset, dstScanlineOffset,
           srcPixelStride, dstPixelStride,
	   srcScanlineStride, dstScanlineStride,
	   width, height,
	   0, height,
	   pml); idx++;
	// bottom right
	pml = (ParamLattice)coefs[band].get(idx);   
	subBandLattice(
	   srcData, dstData,
           srcScanlineOffset, dstScanlineOffset,
           srcPixelStride, dstPixelStride,
	   srcScanlineStride, dstScanlineStride,
	   width, height,
	   width, height,
	   pml); idx++;
	width <<=1;
	height <<=1;
    }


   }
   
   /** Process just a subband with current Lattice */
   protected void subBandLattice(
	   float srcData[], float dstData[],
           int srcScanlineOffset, int dstScanlineOffset,
           int srcPixelStride, int dstPixelStride,
	   int srcScanlineStride, int dstScanlineStride,
	   int width, int height,
	   int firstx, int firsty,
	   ParamLattice pml) {

    int latticeHeight=pml.getHeight(), latticeWidth=pml.getWidth();   

    int sslo = srcScanlineOffset+firstx*srcPixelStride+firsty*srcScanlineStride;
    int dslo = dstScanlineOffset+firstx*dstPixelStride+firsty*dstScanlineStride;
    for (int j=0;j<height;j+=latticeHeight) {
	int spo = sslo, dpo = dslo;
	for (int i=0;i<width;i+=latticeWidth) {
	    float[] v = new float[latticeWidth*latticeHeight];
	    int k=0;

	    for (int lj=0;lj<latticeHeight;lj++) 
		for (int li=0;li<latticeWidth;li++) {	
		    int soff=li*srcPixelStride+lj*srcScanlineStride;
		    // we flatten lattice into a vector
		    v[k++]=srcData[spo+soff];
		}

	    // we apply the lattice quantization of this subband
	    float[] res=pml.apply(v);

	    k=0;
	    // write resulting vector onto destination image
	    for (int lj=0;lj<latticeHeight;lj++) 
		for (int li=0;li<latticeWidth;li++) {
		    int doff=li*dstPixelStride+lj*srcScanlineStride;
		    dstData[dpo+doff]=res[k++];
		}
	    spo += srcPixelStride*latticeWidth;
	    dpo += dstPixelStride*latticeWidth;
	}
	sslo += srcScanlineStride*latticeHeight;
	dslo += dstScanlineStride*latticeHeight;
    
   }
   }


   /** Default coeficients initialization for SB Uniform Quantization */
   protected void iniSBCoefs() {
      int subBands = 3*level+1;
      int bands=getSampleModel().getNumBands();
      coefs = new Vector[bands];
      
      for (int k=0;k<bands;k++) {
	  coefs[k] = new Vector(subBands);
	  coefs[k].add(new Float(1f));
	  int lev = 2;
	  for (int i=1;i<subBands;i+=3) {
	      coefs[k].add(new Float((float)lev)); 
	      coefs[k].add(new Float((float)lev));
	      coefs[k].add(new Float((float)lev));
	      lev <<=1;
	  }
      }
   }

   /** Default coeficients initialization for SB Uniform Quantization
     * of no initialized coeficients in a Vector
     * @param cof the Vector to initialize
     */
   protected void iniSBCoefs(Vector[] cof) {
      int subBands = 3*level+1;    
      int bands=getSampleModel().getNumBands();
      for (int k=0;k<bands;k++) {
	  if (((Float)cof[k].get(0)).floatValue()==0f) cof[k].set(0,new Float(1f));
	  int lev = 2;
	  for (int i=1;i<subBands;i+=3) {
	      if (((Float)cof[k].get(i)).floatValue()==0f)
		  cof[k].set(i,new Float((float)lev));
	      if (((Float)cof[k].get(i+1)).floatValue()==0f) 
		  cof[k].set(i+1,new Float((float)lev));
	      if (((Float)cof[k].get(i+2)).floatValue()==0f)
		  cof[k].set(i+2,new Float((float)lev));
	      lev <<=1;
	  }
      }
   }

   /** Initializes the Lattice Coefs like SBUniform */
    protected void iniLCoefs() {
      int subBands = 3*level+1;
      int bands=getSampleModel().getNumBands();

      coefs = new Vector[bands];      

      for (int k=0;k<bands;k++) {
	  coefs[k] = new Vector(subBands);
	  int t = ParamLattice.INTEGER;
	  coefs[k].add(new ParamLattice(t,1,1,1f));
	  int lev = 2;
	  for (int i=1;i<subBands;i+=3) {
	      coefs[k].add(new ParamLattice(t,1,1,(float)lev)); 
	      coefs[k].add(new ParamLattice(t,1,1,(float)lev));
	      coefs[k].add(new ParamLattice(t,1,1,(float)lev));
	      lev <<=1;
	  }
      }
    }

    /** Initializes the Lattice Coefs of no initialized coeficients in a Vector
      */
    protected void iniLCoefs(Vector[] cof) {
      int subBands = 3*level+1;
      int bands=getSampleModel().getNumBands();

      for (int k=0;k<bands;k++) {
	  ParamLattice pml = (ParamLattice)cof[k].get(0);
	  if (pml.getScale()==0f) pml.setScale(1f);     
	  int lev = 2;
	  for (int i=1;i<subBands;i+=3) {
	      pml = (ParamLattice)cof[k].get(i);
	      if (pml.getScale()==0f) pml.setScale((float)lev); 
	      pml = (ParamLattice)cof[k].get(i+1);
	      if (pml.getScale()==0f) pml.setScale((float)lev); 
	      pml = (ParamLattice)cof[k].get(i+2);
	      if (pml.getScale()==0f) pml.setScale((float)lev); 
	      lev <<=1;
	  }
      }
    }
}

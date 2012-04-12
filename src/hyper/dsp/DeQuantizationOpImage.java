package hyper.dsp;

import java.awt.*;
import java.awt.image.*;
import java.awt.color.*;
import javax.media.jai.*;
import java.util.Map;
import java.util.Vector;

/**
  * OpImage implementation for "Dequantization" operator.
  *
  * @author David Gavilan
  */
public class DeQuantizationOpImage extends QuantizationOpImage {

  protected static final ImageLayout setLayoutF(RenderedImage src){
    int bands = src.getSampleModel().getNumBands(); 
    int height = src.getHeight(), tileHeight=src.getTileHeight(); 
    int width = src.getWidth(), tileWidth=src.getTileWidth();

    int[] order = new int[bands];
    for (int i=0;i<bands;i++) order[i]=i;
    ComponentSampleModelJAI csm = new ComponentSampleModelJAI(
        DataBuffer.TYPE_FLOAT, tileWidth, tileHeight,
	tileWidth*bands, bands, order);
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
    FloatDoubleColorModel ccm = new FloatDoubleColorModel(
        cs,
	false, false, Transparency.OPAQUE, DataBuffer.TYPE_FLOAT);
	
    ImageLayout il = new ImageLayout(
		   src.getTileGridXOffset(),
                   src.getTileGridYOffset(),
                   src.getTileWidth(),
		   src.getTileHeight(),
		   csm, ccm);

    return il;
   }  
   public DeQuantizationOpImage(RenderedImage source,ImageLayout layout,
          Map config, String algorism, int level, Vector[] coefs) {
      super(source, setLayoutF(source), config, true);
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
     SHORT sources and a FLOAT destination, the RasterAccessor will have type FLOAT.
     Consequently your computeRect() implementation will need to use floating point
     processing.
     */	

     // source accessor
     RasterAccessor src = new RasterAccessor(sources[0],
        mapDestRect(destRect, 0), formatTags[0],
	getSourceImage(0).getColorModel());

     //System.out.println("deq: "+COps.getDataTypeName(src.getDataType()));
     //if (src.getDataType() != DataBuffer.TYPE_SHORT) {
     //   throw new IllegalArgumentException("Short datatype sources only.");
     //}

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
   
   
   /** Uniform DeQuantization */
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
	 
	   dstData[dstPixelOffset] = srcData[srcPixelOffset]*val;
	   
	   srcPixelOffset += dstPixelStride;
	   dstPixelOffset += dstPixelStride;
	   
	 }
	 srcScanlineOffset += srcScanlineStride;
	 dstScanlineOffset += dstScanlineStride;	 
       }	   
   }
   
   
   /** SubBand Uniform DeQuantization */
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
	 
	   dstData[dstPixelOffset] = srcData[srcPixelOffset]*val;
	   
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
		    
		    v[k++]=srcData[spo+soff];
		}

	    // we apply the lattice dequantization of this subband
	    float[] res=pml.iapply(v);
	    
	    k=0;
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

   
}

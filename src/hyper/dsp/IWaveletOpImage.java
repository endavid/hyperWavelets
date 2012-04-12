package hyper.dsp;

import java.awt.*;
import java.awt.image.*;
import javax.media.jai.*;
import java.util.Map;

/**
  * OpImage implementation for "IWavelet" operator.
  *
  * <p> Performs the Inverse Wavelet Tranformation of a float image, regarding the
  * algorism chosen. From the time being, algorisms supported are:
  * <p><UL>
  *    <LI>shore
  *    <LI>haar
  *    </UL></P>
  * <P>You must also specify how many levels shall be calculated.  
  * @see hyper.dsp.WaveletOpImage
  * @author David Gavilan
  */
public class IWaveletOpImage extends WaveletOpImage {

  public IWaveletOpImage(RenderedImage source, ImageLayout layout,
         Map config, String algorythm, int level) {
     super(source,layout,config,algorythm,level);
  }

  public void computeRect(Raster sources[], WritableRaster dest,
          Rectangle destRect) {

//  protected void computeImage(Raster[] sources, WritableRaster dest,
//       Rectangle destRect) {
     RasterFormatTag[] formatTags = getFormatTags();
     
     // destination accessor
     RasterAccessor dst = new RasterAccessor(dest, destRect, 
        formatTags[1], getColorModel());

     // the resulting image will be float (more accuracy before
     // the quantization process)
     if (dst.getDataType() != DataBuffer.TYPE_FLOAT) {
        throw new IllegalArgumentException("Supports float data only.");
     }
     
     // source accessor
     RasterAccessor src = new RasterAccessor(sources[0],
        mapDestRect(destRect, 0), formatTags[0],
	getSourceImage(0).getColorModel());

     if (src.getDataType() != DataBuffer.TYPE_FLOAT) {
        throw new IllegalArgumentException("Non-float source not supported yet.");
     }
     // get destination dimensions
     int dwidth = dst.getWidth();
     int dheight = dst.getHeight();
     int dnumBands = dst.getNumBands();
     
     int halfx = dwidth >> 1;
     int halfy = dheight >> 1;
     
     // get destination data array references and strides
     float dstDataArrays[][] = dst.getFloatDataArrays();
     int dstBandOffsets[] = dst.getBandOffsets();
     int dstPixelStride = dst.getPixelStride();
     int dstScanlineStride = dst.getScanlineStride();
     
     // get source data array references and strides     
     float srcDataArrays[][] = src.getFloatDataArrays();
     int srcBandOffsets[] = src.getBandOffsets();
     //int srcPixelStride = src.getPixelStride();
     int srcScanlineStride = src.getScanlineStride();

     copyBuffer(srcDataArrays,dstDataArrays,
         srcBandOffsets,dstBandOffsets,
	 dstPixelStride, dstPixelStride,
	 srcScanlineStride, dstScanlineStride, 
	 dwidth, dheight, dnumBands);

     for (int k=0;k<dnumBands;k++) {
       float dstData[] = dstDataArrays[k];

       int dstScanlineOffset = dstBandOffsets[k];
       
       int rangex = dwidth >> (level-1);
       int rangey = dheight >> (level-1);       
       int sslo = 0;
       int sps = 1;
       int ssls = rangex;
       for (int i=0; i<level; i++) {
	   float srcData[] = cropBuffer(dstData, dstScanlineOffset,
                dstPixelStride, dstScanlineStride, 
	            dstScanlineOffset, rangex, rangey);       
       if (algorism.equals("shore")) {
              shoreWt(srcData, dstData, sslo, dstScanlineOffset,
                  sps, dstPixelStride, ssls, dstScanlineStride, rangex, rangey);
	   } else if (algorism.equals("haar")) {
              haarWt(srcData, dstData, sslo, dstScanlineOffset,
                  sps, dstPixelStride, ssls, dstScanlineStride, rangex, rangey);
	   }
 	  rangex <<= 1;
 	  rangey <<= 1;
	   ssls=rangex;
       }		    
       
     }
     
     
     if (dst.isDataCopy()) {
       dst.clampDataArrays();
       dst.copyDataToRaster();
     }
  }

  private void shoreWt(float srcData[], float dstData[],
          int srcScanlineOffset, int dstScanlineOffset,
	  int srcPixelStride, int dstPixelStride,
	  int srcScanlineStride, int dstScanlineStride, int sizex, int sizey) {
    
       float v[] = new float[4];
       int halfx = sizex >> 1;
       int halfy = sizey >> 1;       
       int migY = halfy * srcScanlineStride;
       int migX = halfx * srcPixelStride;
       
       for (int j = 0; j<halfy;j++) {
         int srcPixelOffset = srcScanlineOffset;
		 int dstPixelOffset = dstScanlineOffset;
	 
		 for (int i = 0; i<halfx; i++) {

           v[0] = srcData[srcPixelOffset];
           v[1] = srcData[srcPixelOffset+migX];
           v[2] = srcData[srcPixelOffset+migY];
           v[3] = srcData[srcPixelOffset+migX+migY]; 

		   dstData[dstPixelOffset] = v[0]+(v[1]+v[2]+v[3])/2f;
		   dstData[dstPixelOffset+dstPixelStride] = 
	          v[0]+(-v[1]+v[2]-v[3])/2f;
           dstData[dstPixelOffset+dstScanlineStride] = 
	          v[0]+(v[1]-v[2]-v[3])/2f;
           dstData[dstPixelOffset+dstPixelStride+dstScanlineStride] =
	          v[0]+(-v[1]-v[2]+v[3])/2f;
		  
	 	  srcPixelOffset += srcPixelStride;
	 	  dstPixelOffset += dstPixelStride << 1;
	   
	 	}
	 	srcScanlineOffset += srcScanlineStride;
	 	dstScanlineOffset += dstScanlineStride << 1;	 
       }
  }

  private void haarWt(float srcData[], float dstData[],
          int srcScanlineOffset, int dstScanlineOffset,
	  int srcPixelStride, int dstPixelStride,
	  int srcScanlineStride, int dstScanlineStride, int sizex, int sizey) {
	  
     int halfx = sizex >> 1;
     int halfy = sizey >> 1;     
     int dslo = dstScanlineOffset;    
     int migSalt = halfy * srcScanlineStride;
     int sslo = srcScanlineOffset;
     
     // apply 1D Haar vertically
     for (int i = 0; i<sizex;i++) {
	 	int dstPixelOffset = dslo;
	 	int srcPixelOffset = sslo;
	 
	 	float[] vline=cropBuffer(srcData, srcScanlineOffset,
	         srcPixelStride, srcScanlineStride,
		 	srcPixelOffset, 1, sizey);
	 	for (int j = 0; j<sizey; j+=2) {
		   dstData[dstPixelOffset] = vline[j>>1]+vline[(j>>1)+halfy]/2f;
		   dstData[dstPixelOffset+dstScanlineStride] =
		       vline[j>>1]-vline[(j>>1)+halfy]/2f;

		   dstPixelOffset += dstScanlineStride<<1;
		 }
		 dslo +=dstPixelStride;
		 sslo +=srcPixelStride;
     }
     
     // apply 1D Haar horizontally
     migSalt = halfx * srcPixelStride;

     for (int j = 0; j<sizey;j++) {
		 int dstPixelOffset = dstScanlineOffset;


		 float[] hline=cropBuffer(dstData, dstScanlineOffset,
	         dstPixelStride, dstScanlineStride,
			 dstPixelOffset, sizex, 1);	 
		 for (int i = 0; i<sizex; i+=2) {

		   dstData[dstPixelOffset] = hline[i>>1] + hline[(i>>1)+halfx] / 2f;
		   dstData[dstPixelOffset+dstPixelStride] = 
		       hline[i>>1]-hline[(i>>1)+halfx]/2f;
	       
		   dstPixelOffset += dstPixelStride<<1;

		 }
		 dstScanlineOffset +=dstScanlineStride;

     }
     
  }

  public static void copyBuffer(float srcDataArrays[][], float dstDataArrays[][],
         int srcBandOffsets[], int dstBandOffsets[],
	 int srcPixelStride, int dstPixelStride,
	 int srcScanlineStride, int dstScanlineStride,
	 int dwidth, int dheight, int dnumBands){
  
    for (int k=0;k<dnumBands;k++) {
       float dstData[] = dstDataArrays[k];
       float srcData[] = srcDataArrays[k];
       int srcScanlineOffset = srcBandOffsets[k];
       int dstScanlineOffset = dstBandOffsets[k];
       	
       for (int j = 0; j<dheight;j++) {
         int srcPixelOffset = srcScanlineOffset;
	 int dstPixelOffset = dstScanlineOffset;
	 
	 for (int i = 0; i<dwidth; i++) {
	 
	   // we just copy each sample
	   dstData[dstPixelOffset] = srcData[srcPixelOffset];
	   
	   srcPixelOffset += srcPixelStride;
	   dstPixelOffset += dstPixelStride;
	   
	 }
	 srcScanlineOffset += srcScanlineStride;
	 dstScanlineOffset += dstScanlineStride;	 
       }
     }
  }
  
}

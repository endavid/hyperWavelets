package hyper.dsp;

import java.awt.*;
import java.awt.image.*;
import javax.media.jai.*;
import java.util.Map;

/**
  * OpImage implementation for "Wavelet" operator.
  *
  * <p> Performs the Wavelet Tranformation of a float image, regarding the
  * algorism chosen. From the time being, algorisms supported are:
  * <p><UL>
  *    <LI>shore
  *    <LI>haar
  *    </UL></P>
  * <P>You must also specify how many levels shall be calculated. For later use,
  *    remember that we order the subbands this way:
  *    <P>(being level=3)<P align=center>
  *    <table border=1><tr><td><table border=1><tr><td><table border=1>
  *       <tr><td>0</td><td>1</td></tr>
  *       <tr><td>2</td><td>3</td></tr></table></td><td width=20>4</td></tr>
  *       <tr><td height=20>5</td><td>6</td></tr></table></td><td width=40>7</td></tr>
  *       <tr><td height=40>8</td><td>9</td></tr></table></P>
  * @author David Gavilan
  */
public class WaveletOpImage extends PointOpImage {
//public class WaveletOpImage extends UntiledOpImage {

  protected String algorism;
  protected int level;
 
  /**
    * Constructs an OpImage representing a wavelet.
    * @param algorism The kind of wavelet.
    * @param level Times to apply the wavelet on image.
    */
  public WaveletOpImage(RenderedImage source, ImageLayout layout,
         Map config, String algorism, int level) {
     super(source,layout,config,true);
     
     this.algorism = algorism;
     this.level = level;
  }

  /**
    * Performs a wavelet operation on a specified rectangle. Doing so, we will
    * be applying a wavelet to each tile of the image. Those tiles should be
    * square.
    * @param sources an array of source Rasters, guaranteed to provide all
    *                necessary source data for computing the output. In this
    *                case, just one Raster.
    * @param dest a WritableRaster containing the area to be computed
    * @param destRect the rectangle within dest to be processed
    */
  public void computeRect(Raster sources[], WritableRaster dest,
          Rectangle destRect) {
  
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
     
//     if ((dwidth != dheight) && (algorism.equals("shore"))) {	
//        throw new IllegalArgumentException("Only square images supported in \"shore\" algorism: "+dwidth+"x"+dheight);
//     }
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

     for (int k=0;k<dnumBands;k++) {
       float dstData[] = dstDataArrays[k];
       float srcData[] = srcDataArrays[k];
       int srcScanlineOffset = srcBandOffsets[k];
       int dstScanlineOffset = dstBandOffsets[k];
       
       int rangex = halfx;
       int rangey = halfy;
       int sslo = srcScanlineOffset;
       int sps = dstPixelStride;
       int ssls = srcScanlineStride;
       for (int i=0; i<level; i++) {
          if (algorism.equals("shore")) {
              shoreWt(srcData, dstData, sslo, dstScanlineOffset,
                  sps, dstPixelStride, ssls, dstScanlineStride, rangex, rangey );
	  	} else if (algorism.equals("haar")) {
              haarWt(srcData, dstData, sslo, dstScanlineOffset,
                  sps, dstPixelStride, ssls, dstScanlineStride, rangex, rangey );
	  	}
	  	if (i<level-1) srcData = cropBuffer(dstData, dstScanlineOffset,
                    dstPixelStride, dstScanlineStride, 
	            	dstScanlineOffset, rangex, rangey);
	  	sslo = 0;
	  	sps = 1;
	  	ssls = rangex;
  	    rangex >>= 1;
  	    rangey >>= 1;
       }		    
       
     }
     
     
     if (dst.isDataCopy()) {
       dst.clampDataArrays();
       dst.copyDataToRaster();
     }
  }

  /** Applies the shore wavelet.
    * @param srcData[] the source image data
    * @param dstData[] where to store the transformed image
    * @param srcScanlineOffset in which scan-line the source starts
    * @param dstScanlineOffset the scan-line offset from where to start to store
    * @param srcPixelStride the distance between consecutive pixels in the source
    * @param dstPixelStride the distance between consecutive pixels in the destination
    * @param srcScanlineStride the distance between two scanlines in the source
    * @param dstScanlineStride the distance between two scanlines in the destination
    * @param half size (=width=height) of the wavelet subband - in this step
    */
  private void shoreWt(float srcData[], float dstData[],
      int srcScanlineOffset, int dstScanlineOffset,
	  int srcPixelStride, int dstPixelStride,
	  int srcScanlineStride, int dstScanlineStride, int halfx, int halfy) {
    
       float v[] = new float[4];
       
       
       for (int j = 0; j<halfy;j++) {
         int srcPixelOffset = srcScanlineOffset;
		 int dstPixelOffset = dstScanlineOffset;
	 
	 	for (int i = 0; i<halfx; i++) {

           v[0] = srcData[srcPixelOffset];
           v[1] = srcData[srcPixelOffset+srcPixelStride];
           v[2] = srcData[srcPixelOffset+srcScanlineStride];
           v[3] = srcData[srcPixelOffset+srcPixelStride+srcScanlineStride]; 

		   dstData[dstPixelOffset] = (v[0]+v[1]+v[2]+v[3])/4f;
		   dstData[dstPixelOffset+halfx*dstPixelStride] = 
	          (v[0]-v[1]+v[2]-v[3])/2f;
           dstData[dstPixelOffset+halfy*dstScanlineStride] = 
	          (v[0]+v[1]-v[2]-v[3])/2f;
           dstData[dstPixelOffset+halfx*dstPixelStride+halfy*dstScanlineStride] =
	          (v[0]-v[1]-v[2]+v[3])/2f;
		  
	 	  srcPixelOffset += srcPixelStride << 1;
	  	 dstPixelOffset += dstPixelStride;
	   
	 	}
	 	srcScanlineOffset += srcScanlineStride << 1;
	 	dstScanlineOffset += dstScanlineStride;	 
       }
  }

  /** Applies the haar wavelet.
    * @param srcData[] the source image data
    * @param dstData[] where to store the transformed image
    * @param srcScanlineOffset in which scan-line the source starts
    * @param dstScanlineOffset the scan-line offset from where to start to store
    * @param srcPixelStride the distance between consecutive pixels in the source
    * @param dstPixelStride the distance between consecutive pixels in the destination
    * @param srcScanlineStride the distance between two scanlines in the source
    * @param dstScanlineStride the distance between two scanlines in the destination
    * @param half size (=width=height) of the wavelet subband - in this step
    */
  private void haarWt(float srcData[], float dstData[],
          int srcScanlineOffset, int dstScanlineOffset,
		  int srcPixelStride, int dstPixelStride,
		  int srcScanlineStride, int dstScanlineStride, int halfx, int halfy) {
	  
     // apply 1D Haar horizontally
     int sizex = halfx << 1;
     int sizey = halfy << 1;
     int migSalt = halfx * dstPixelStride;
     int dslo = dstScanlineOffset;
     for (int j = 0; j<sizey;j++) {
         int srcPixelOffset = srcScanlineOffset;
	 	int halfOff = dslo;
	 
	 	for (int i = 0; i<sizex; i+=2) {
	   	dstData[halfOff] = (srcData[srcPixelOffset] + 
	       	srcData[srcPixelOffset+srcPixelStride]) / 2f;
	   	dstData[halfOff+migSalt] = (srcData[srcPixelOffset] -
	       	srcData[srcPixelOffset+srcPixelStride]);
	       
	   	srcPixelOffset += srcPixelStride<<1;
	   	halfOff += dstPixelStride;
	 	}
	 	srcScanlineOffset +=srcScanlineStride;
	 	dslo +=dstScanlineStride;
     }
     
     // apply 1D Haar vertically
     migSalt = halfy * dstScanlineStride;
     dslo = dstScanlineOffset;     
     for (int i = 0; i<sizex;i++) {
	 	int halfOff = dslo;
	 
	 	float[] vline=cropBuffer(dstData, dstScanlineOffset,
	         dstPixelStride, dstScanlineStride,
		 	halfOff, 1, sizey);
	 	for (int j = 0; j<sizey; j+=2) {
	  	 dstData[halfOff] = (vline[j]+vline[j+1]) / 2f;
	   	dstData[halfOff+migSalt] = vline[j]-vline[j+1];
	       
	   	halfOff += dstScanlineStride;
	 	}
	 	dslo +=dstPixelStride;
     }
  }
    
  /** Crops a portion of an image.
    * @param src[] the source image data    
    * @param scanlineOffset in which scan-line the source starts
    * @param pixelStride the distance between consecutive pixels in the source    
    * @param scanlineStride the distance between two scanlines in the source
    * @param pixelOffset the point from where we start the cropping
    * @param width width of the cropping
    * @param height height of the cropping
    * @return the portion of the image cropped    
    */
  public static float[] cropBuffer(float src[], int scanlineOffset,
         int pixelStride, int scanlineStride, 
	 int pixelOffset, int width, int height) {
     
     float dst[] = new float[width*height];

     int dstPixelOffset = 0;
     for (int j = 0; j<height;j++) {
         int off = pixelOffset;
	 
	 for (int i = 0; i<width; i++) {

	   dst[dstPixelOffset] = src[off];
		  
	   off += pixelStride;
	   dstPixelOffset ++;
	   
	 }
	 
	 pixelOffset += scanlineStride;	 

     }
     
     return dst;
  }
}

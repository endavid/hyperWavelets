
package hyper.dsp;

import java.awt.*;
import java.awt.image.*;
import java.awt.image.renderable.*;
import javax.media.jai.*;
import javax.media.jai.operator.*;
import java.util.HashMap;
import java.util.Vector;
import java.awt.color.*;
import java.awt.Transparency;
import javax.media.jai.iterator.*;

import com.sun.media.jai.codec.*;
import java.io.*;

/**
 * COps = Common Operations.
 * This class comprises a series of static methods in order to invoke <b>JAI</b>
 * operations via an easy-to-use function.
 * @see <a href="http://java.sun.com/products/java-media/jai/forDevelopers/jai1_0_1guide-unc/index.html">JAI manual</a>
 * @author David Gavilan
 */
public class COps {

  /**
    * Applies the wavelet operation to a <b>float</b> image. The result is another
    * float image.
    * @param image the input image
    * @param algorism which algorism to apply (Haar, Shore)
    * @param level number of levels of the DWT Transform
    * @return the output image as a <b>RenderedOp</b>
    * @see hyper.dsp.WaveletOpImage
    */
  public static RenderedOp wavelet(PlanarImage image, 
         String algorism, int level) {
    ParameterBlock pb = new ParameterBlock();
    pb.addSource(image);
    pb.add(algorism);
    pb.add(level);
    
    return JAI.create("Wavelet",pb);    
  }

  /**
    * Applies the inverse wavelet transform to a <b>float</b> image. The result is
    * another float image.
    * @param image the input image
    * @param algorism which algorism to apply (Haar, Shore)
    * @param level number of levels of the DWT Transform
    * @return the output image as a <b>RenderedOp</b>
    * @see hyper.dsp.IWaveletOpImage
    */
  public static RenderedOp iwavelet(PlanarImage image, 
         String algorism, int level) {
    ParameterBlock pb = new ParameterBlock();
    pb.addSource(image);
    pb.add(algorism);
    pb.add(level);
    
    return JAI.create("IWavelet",pb);    
  }

  /**
    * Applies the quantization operation to a <b>float</b> image. The result is
    * a <b>short</b> image.
    * @param image the input image
    * @param algorism which algorism to apply (Uniform, SBUniform, Latice)
    * @param level number of levels of the DWT Transform
    * @param coefs a vector containing the parameters per subband
    * @return the output image as a <b>RenderedOp</b>
    * @see hyper.dsp.QuantizationOpImage
    * @see hyper.dsp.ParamLattice
    */
  public static RenderedOp quantization(PlanarImage image,
         String algorism, int level, Vector[] coefs) {
    ParameterBlock pb = new ParameterBlock();
    pb.addSource(image);
    pb.add(algorism);
    pb.add(level);
    pb.add(coefs);
    
    return JAI.create("Quantization",pb);    
  }

  /**
    * Applies the dequantization operation to a <b>short</b> image. The result is
    * a <b>float</b> image.
    * @param image the input image
    * @param algorism which algorism to apply (Uniform, SBUniform, Latice)
    * @param level number of levels of the DWT Transform
    * @param coefs a vector containing the parameters per subband
    * @return the output image as a <b>RenderedOp</b>
    * @see hyper.dsp.QuantizationOpImage
    * @see hyper.dsp.ParamLattice
    */
  public static RenderedOp dequantization(PlanarImage image,
         String algorism, int level, Vector[] coefs) {
    ParameterBlock pb = new ParameterBlock();
    pb.addSource(image);
    pb.add(algorism);
    pb.add(level);
    pb.add(coefs);
    
    return JAI.create("Dequantization",pb);    
  }
  

  public static RenderedOp lookup(PlanarImage image,) {
    float blurmatrix[] = {1/16f, 1/8f, 1/16f,
                          1/8f,  1/4f, 1/8f,
			  1/16f, 1/8f, 1/16f};
    KernelJAI blurkernel = new KernelJAI(3,3,blurmatrix);
    return JAI.create("lookup", image, blurkernel);
  }


  public static RenderedOp blur(PlanarImage image) {
    float blurmatrix[] = {1/16f, 1/8f, 1/16f,
                          1/8f,  1/4f, 1/8f,
			  1/16f, 1/8f, 1/16f};
    KernelJAI blurkernel = new KernelJAI(3,3,blurmatrix);
    return JAI.create("convolve", image, blurkernel);
  }
  
  public static RenderedOp sharpen(PlanarImage image) {
    float sharpmatrix[] = {-1.0f, -1.0f, -1.0f,
                          -1.0f,  9.0f, -1.0f,
			  -1.0f, -1.0f, -1.0f};
    KernelJAI sharpkernel = new KernelJAI(3,3,sharpmatrix);
    return JAI.create("convolve", image, sharpkernel);
  }
  
  public static RenderedOp medianFilter(PlanarImage image,
                              MedianFilterShape maskShape, int maskSize) {
    ParameterBlock pb = new ParameterBlock();
    pb.addSource(image);
    pb.add(maskShape);
    pb.add(maskSize);
    
    return JAI.create("MedianFilter",pb);
  }

  public static RenderedOp sobelGradientMagnitude(PlanarImage image) {
    KernelJAI sobelVertKernel = KernelJAI.GRADIENT_MASK_SOBEL_VERTICAL;
    KernelJAI sobelHorizKernel = KernelJAI.GRADIENT_MASK_SOBEL_HORIZONTAL;
    ParameterBlock pb = new ParameterBlock();
    pb.addSource(image);
    pb.add(sobelHorizKernel);
    pb.add(sobelVertKernel);
    
    return JAI.create("gradientmagnitude", pb);
  }
  
  public static RenderedOp scale(RenderedImage image,
                       float magx, float magy, float transx, float transy) {
    ParameterBlock pb = new ParameterBlock();
    pb.addSource(image);
    pb.add(magx);pb.add(magy);pb.add(transx);pb.add(transy);
    pb.add(Interpolation.getInstance(Interpolation.INTERP_NEAREST));
    return JAI.create("scale", pb);
  }
  
  public static RenderedOp subtract(PlanarImage img1, PlanarImage img2) {
    ParameterBlock pb = new ParameterBlock();
    pb.addSource(img1);
    pb.addSource(img2);
    return JAI.create("subtract",pb);
  }
  
  public static RenderedOp DCT(PlanarImage img) {
    ParameterBlock pb = new ParameterBlock();
    pb.addSource(img);
    return JAI.create("dct",pb);
  }

  public static RenderedOp inverseDCT(PlanarImage img) {
    ParameterBlock pb = new ParameterBlock();
    pb.addSource(img);
    return JAI.create("idct",pb);
  }

  public static RenderedOp absolute(PlanarImage img) {
    ParameterBlock pb = new ParameterBlock();
    pb.addSource(img);
    return JAI.create("absolute",pb);
  }

  public static RenderedOp invert(PlanarImage img) {
    ParameterBlock pb = new ParameterBlock();
    pb.addSource(img);
    return JAI.create("invert",pb);
  }

  public static RenderedOp multiply(PlanarImage img, double val) {
    int bands = img.getSampleModel().getNumBands();
    double[] cons = new double[bands];
    for (int i=0;i<bands;i++) cons[i]=val;
    
    ParameterBlock pb = new ParameterBlock();
    pb.addSource(img);
    pb.add(cons);
    
    return JAI.create("MultiplyConst",pb);
  }
  
  public static RenderedOp rescale(PlanarImage img,int factor, int offset) {
    int bands = img.getSampleModel().getNumBands();
    int[] con = new int[bands];
    int[] off = new int[bands];
    for (int i=0;i<bands;i++) { con[i]=factor; off[i]=offset; }
    ParameterBlock pb = new ParameterBlock();
    pb.addSource(img);
    pb.add(con);
    pb.add(off);
    return JAI.create("rescale",pb);
  }

  /** Rescale samples of the image from by doing <br>
    * <code>dst[x][y][b] = src[x][y][b]*constant + offset</code>
    */
  public static RenderedOp rescaleSamples(PlanarImage img, double offset, double scale) {
     int bands = img.getSampleModel().getNumBands();
     double[] offsets = new double[bands];
     double[] scales = new double[bands];
     for (int i=0;i<bands;i++) {
     	offsets[i]=offset; // offsets
     	scales[i]=scale; // scales
     }

 	ParameterBlock pb = new ParameterBlock();
 	pb.addSource(img);
 	pb.add(scales);
 	pb.add(offsets);
 	
 	return JAI.create("rescale",pb);
 }
  
  /**
   * Used to change data type of the image databuffer and/or re-tile it.
   * Due to a bug in JAI, data type could not be changed with this operator 
   * until JAI 1.1.1
   * @param img the input image
   * @param tileDim the Dimension of the new tile
   * @param type target image type
   * @return an output image with the desired type and tile size.
   */
  public static RenderedOp reformat(PlanarImage img, Dimension tileDim,int type) {
    int tileWidth = tileDim.width;
    int tileHeight = tileDim.height;
    ImageLayout tileLayout = new ImageLayout(img);
    tileLayout.setTileWidth(tileWidth);
    tileLayout.setTileHeight(tileHeight);
    
    HashMap map = new HashMap();
    map.put(JAI.KEY_IMAGE_LAYOUT, tileLayout);
    map.put(JAI.KEY_INTERPOLATION, 
        Interpolation.getInstance(Interpolation.INTERP_BICUBIC));
    RenderingHints tileHints = new RenderingHints(map);
    
    ParameterBlock pb = new ParameterBlock();
    pb.addSource(img);
    pb.add(type);
    return JAI.create("format", pb, tileHints);    
  }
  
  /**
   * Just to change the datatype. No tile size change.
   * @param img the input image
   * @param type the destination image type
   * @return the output image
   */
  public static RenderedOp reformat(PlanarImage img, int type) {
    Dimension d = new Dimension(img.getTileWidth(),img.getTileHeight());
    return reformat(img,d,type);
  }

  /** Treating img as the difference between 2 images, computes
    * the Mean Square Error (MSE) per channel
    * @param img the input img, which is supposed to be the difference between two
    * @return a vector containing the Mean Square Error per band
    */
  public static double[] computeMSE(PlanarImage img) {
    int bands = img.getSampleModel().getNumBands(); 
    int height = img.getHeight(); 
    int width = img.getWidth();
    
    double[] mse= new double[bands];

    // used to access the source image
    RandomIter iter = RandomIterFactory.create(img, null);
  
    double v;
    for (int band=0; band < bands; band++) {
      mse[band]=0f;
      for (int i=0; i<width; i++)
        for (int j=0; j<height; j++) {
          v=(double)iter.getSampleFloat(i,j,band);
          mse[band]+=v*v;
        }
      mse[band] /= (double)width*height;
    }
    
    return mse;
  }

    /**
      * Returns a string representation of datatype constants.
      * @param type the DataBuffer type
      * @return a String representing the type
      */
    public static String getDataTypeName(int type) {
	switch (type) {
	case DataBuffer.TYPE_BYTE:
	    return "byte";
	case DataBuffer.TYPE_SHORT:
	    return "short";
	case DataBuffer.TYPE_INT:
	    return "int";
	case DataBuffer.TYPE_USHORT:
	    return "unsigned short";
	case DataBuffer.TYPE_DOUBLE:
	    return "double";
	case DataBuffer.TYPE_FLOAT:
	    return "float";
	}
	return "unknown";
    }
  
  
  /**
    * Saves images to a JPEG file
    */
  public static void saveAsJPG(PlanarImage pimg, String file) throws java.io.IOException {
    OutputStream out = new FileOutputStream(file);
    JPEGEncodeParam param = new JPEGEncodeParam();
    ImageEncoder encoder = ImageCodec.createImageEncoder("JPEG",out,param);
    encoder.encode(pimg);
    out.close();
  }  

  /**
    * Saves images to a TIFF file
    */
  public static void saveAsTIFF(PlanarImage pimg, String file) throws java.io.IOException {
    OutputStream out = new FileOutputStream(file);
    TIFFEncodeParam param = new TIFFEncodeParam();
    ImageEncoder encoder = ImageCodec.createImageEncoder("TIFF",out,param);
    encoder.encode(pimg);
    out.close();
  }
  
  /**
    * Saves images to a BMP file
    */    
  public static void saveAsBMP(PlanarImage pimg, String file) throws java.io.IOException {
    OutputStream out = new FileOutputStream(file);
    BMPEncodeParam param = new BMPEncodeParam();
    ImageEncoder encoder = ImageCodec.createImageEncoder("BMP",out,param);
    encoder.encode(pimg);
    out.close();
  }

  /**
    * Saves images to a PNM file
    */  
  public static void saveAsPNM(PlanarImage pimg, String file) throws IOException {
    OutputStream out = new FileOutputStream(file);
    PNMEncodeParam param = new PNMEncodeParam();
    ImageEncoder encoder = ImageCodec.createImageEncoder("PNM",out,param);
    encoder.encode(pimg);
    out.close();
  }

  
  /**
    * Use this under JAI < 1.1.1 to reformat image. (slow!)
    */
  public static TiledImage toFloat(PlanarImage in) {
    int bands = in.getSampleModel().getNumBands(); 
    int height = in.getHeight(), tileHeight=in.getTileHeight(); 
    int width = in.getWidth(), tileWidth=in.getTileWidth();
    
    ComponentSampleModelJAI csm = new ComponentSampleModelJAI(
        DataBuffer.TYPE_FLOAT, tileWidth, tileHeight,
	tileWidth*bands, bands, new int[] {0,1,2});
    FloatDoubleColorModel ccm = new FloatDoubleColorModel(
        ColorSpace.getInstance(ColorSpace.CS_sRGB),
	false, false, Transparency.OPAQUE, DataBuffer.TYPE_FLOAT);
		
    TiledImage outImage = new TiledImage(in.getMinX(), in.getMinY(), 
        in.getWidth(), in.getHeight(), in.getMinX(), in.getMinY(), 
	csm, ccm);

    // used to access the source image
    RandomIter iter = RandomIterFactory.create(in, null);
  
    for (int band=0; band < bands; band++) { 
    for (int i=0; i<width; i++)
      for (int j=0; j<height; j++) {
        
        outImage.setSample(i,j,band,(float)iter.getSample(i,j,band));

      }
    }
    
    return outImage;      
  }

  /**
    * Use this under JAI < 1.1.1 to reformat image. (slow!)
    */
  public static TiledImage toByte(PlanarImage in) {
    int bands = in.getSampleModel().getNumBands(); 
    int height = in.getHeight(), tileHeight=in.getTileHeight(); 
    int width = in.getWidth(), tileWidth=in.getTileWidth();
    
    ComponentSampleModel csm = new ComponentSampleModel(
        DataBuffer.TYPE_BYTE, tileWidth, tileHeight,
	tileWidth*bands, bands, new int[] {0,1,2});
    ComponentColorModel ccm = new ComponentColorModel(
        ColorSpace.getInstance(ColorSpace.CS_sRGB), new int[] {8,8,8},
	false, false, Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
		
    TiledImage outImage = new TiledImage(in.getMinX(), in.getMinY(), 
        in.getWidth(), in.getHeight(), in.getMinX(), in.getMinY(), 
	csm, ccm);

    // used to access the source image
    RandomIter iter = RandomIterFactory.create(in, null);
  
    for (int band=0; band < bands; band++) { 
    for (int i=0; i<width; i++)
      for (int j=0; j<height; j++) {
        
        outImage.setSample(i,j,band,(byte)iter.getSampleFloat(i,j,band));

      }
    }
    
    return outImage;      
  }

}

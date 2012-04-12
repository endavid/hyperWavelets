package hyper.dsp;

import java.awt.*;
import java.awt.image.*;
import java.awt.image.renderable.*;
import javax.media.jai.*;

/**
  * Class implementing the RIF interface for the IWavelet operator.
  * An instance of this class should be registered with the OperationRegistry
  * with operation name "IWavelet" and product name "ccd-hyper".
  */
public class IWaveletRIF implements RenderedImageFactory {
   public IWaveletRIF() {}
   
   public RenderedImage create(ParameterBlock paramBlock,
                               RenderingHints renderHints) {
      RenderedImage source = paramBlock.getRenderedSource(0);
      
      ImageLayout layout = renderHints == null ? null : 
                  (ImageLayout)renderHints.get(JAI.KEY_IMAGE_LAYOUT);
      
      String algorism = (String)paramBlock.getObjectParameter(0);
      int level = paramBlock.getIntParameter(1);
		  
      return new IWaveletOpImage(source, layout, renderHints,
             algorism, level);
   }

}

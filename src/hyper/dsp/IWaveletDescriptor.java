package hyper.dsp;

import java.awt.*;
import java.awt.image.*;
import java.awt.image.renderable.*;
import javax.media.jai.*;
import javax.media.jai.registry.RenderedRegistryMode;
import javax.media.jai.registry.RenderableRegistryMode;
import java.util.Vector;
import javax.media.jai.util.Range;

/**
  * An <code>OperationDescriptor</code> describing the "IWavelet" operation.
  * <P> The "IWavelet" operation performs the inverse tranformation over the image,
  * obtaining as a result a float image, compound of source subbands.
  * <p><table border=1>
  * <caption>Resource List</caption>
  * <tr><th>Name</th><th>Value</th></tr>
  * <tr><th>GlobalName</th><td>Wavelet</td></tr>
  * <tr><th>LocalName</th><td>Wavelet</td></tr>
  * <tr><th>Description</th><td>Wavelet Tranformation</td></tr>
  * <tr><th>DocURL</th><td>WaveletDescriptor.html</td></tr>
  * <tr><th>Version</th><td>0.1</td></tr>
  * <tr><th>arg0Desc</th><td>A String to specify which algorism to use.</td></tr>
  * <tr><th>arg1Desc</th><td>Number of levels (iterations to do).</td></tr>
  * </table></p>
  * <p><table border=1>
  * <caption>Parameter List</caption>
  * <tr><th>Name</th><th>Class Type</th><th>Default Value</th></tr>
  * <tr><td>algorism</td><td>java.lang.String</td><td>"uniform"</td></tr>
  * <tr><td>level</td><td>java.lang.Integer</td><td>1</td></tr> 
  * </table></p>  
  * @author David Gavilan
  */
public class IWaveletDescriptor extends OperationDescriptorImpl {

  private static final String[][] resources = {
     {"GlobalName", "IWavelet"},
     {"LocalName",  "IWavelet"},
     {"Description", "Does the inverse wavelet transformation on a image."},
     {"DocURL",      "IWaveletDescriptor.html"},
     {"Version",     "0.0"},
     {"arg0Desc",    "A String to specify which wavelet algorism to use."},
     {"arg1Desc",    "Number of levels (iterations to do)."}
  };
  
  private static final Class[] paramClasses = {
      java.lang.String.class, java.lang.Integer.class };
  private static final String[] paramNames = {
      "algorythm", "level" };
  private static final Object[] paramDefaults = {
      new String("shore"), new Integer(1) };
  private static final Object[] validParamValues  = {
      null,
      new Range(Integer.class, new Integer(1), new Integer(10)) 
      };
  private static final Vector algorithms = validAlgorithms();
  
  public IWaveletDescriptor() {
    super(resources, new String[] {RenderedRegistryMode.MODE_NAME,
          RenderableRegistryMode.MODE_NAME}, 1,
	  paramNames, paramClasses, paramDefaults, validParamValues);
  }
  
  protected boolean validateParameters(String modeName,
         ParameterBlock args, StringBuffer msg) {
    if (!super.validateParameters(modeName, args, msg)) {
       return false;
    }
    
    String algo = (String)args.getObjectParameter(0);
    
    if (!algorithms.contains(algo)) {
       msg.append(getName() + algo + " - Unknown algorism.");
       return false;
    }
    
    return true;
  }
  
  private static Vector validAlgorithms() {
    Vector v = new Vector();
    v.add("shore");
    v.add("haar");
    return v;
  }  
    
}

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
  * An <code>OperationDescriptor</code> describing the "dequantization" operation.
  * <P> The "Dequantization" operation restores original values where possible
  * after a quantization has been applied on an image.
  * <p><table border=1>
  * <caption>Resource List</caption>
  * <tr><th>Name</th><th>Value</th></tr>
  * <tr><th>GlobalName</th><td>Dequantization</td></tr>
  * <tr><th>LocalName</th><td>Dequantization"</td></tr>
  * <tr><th>Description</th><td>Dequantizes an image.</td></tr>
  * <tr><th>DocURL</th><td>DeQuantizationDescriptor.html</td></tr>
  * <tr><th>Version</th><td>0.1</td></tr>
  * <tr><th>arg0Desc</th><td>A String to specify which algorism to use.</td></tr>
  * <tr><th>arg1Desc</th><td>Value of the uniform quantization or number of levels (sb).</td></tr>
  * <tr><th>arg2Desc</th><td>A Vector containing the quantization per subband.</td></tr>
  * </table></p>
  * <p><table border=1>
  * <caption>Parameter List</caption>
  * <tr><th>Name</th><th>Class Type</th><th>Default Value</th></tr>
  * <tr><td>algorism</td><td>java.lang.String</td><td>"uniform"</td></tr>
  * <tr><td>level</td><td>java.lang.Integer</td><td>1</td></tr>
  * <tr><td>coeficients</td><td>java.util.Vector</td><td>null</td></tr>
  * </table></p>
  * @author David Gavilan
  */
public class DeQuantizationDescriptor extends OperationDescriptorImpl {

  private static final String[][] resources = {
     {"GlobalName", "Dequantization"},
     {"LocalName",  "Dequantization"},
     {"Description", "Dequantizes an image."},
     {"DocURL",      "DeQuantizationDescriptor.html"},
     {"Version",     "0.1"},
     {"arg0Desc",    "A String to specify which algorism to use."},
     {"arg1Desc","Value of the uniform quantization or number of levels (sb)."},
     {"arg2Desc",    "An array of Vectors containing the quantization per subband."}
  };

  
  private static final Class[] paramClasses = {
       java.lang.String.class, java.lang.Integer.class, java.util.Vector[].class};
  private static final String[] paramNames = {
       "algorism", "level", "coeficients"};
  private static final Object[] paramDefaults = {
      new String("uniform"), new Integer(1), null};
  private static final Object[] validParamValues  = {
       null,
       new Range(Integer.class, new Integer(1), new Integer(256)),
       null
      };
  private static final Vector algorithms = validAlgorithms();
  
  public DeQuantizationDescriptor() {
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
    v.add("uniform");
    v.add("sbuniform");
    v.add("lattice");
    return v;
  }  
    
}

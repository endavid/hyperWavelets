import java.io.*;

import java.awt.image.DataBuffer;
import java.awt.image.renderable.*;
import javax.media.jai.*;
import javax.media.jai.registry.*;

import hyper.QuantizerConfig;
import hyper.dsp.*;
import hyper.coding.*;
import hyper.io.*;
import org.freehep.util.io.*;

/**
  * Haar Wavelet Transform + Lattice Quantization + Index Coding + Arithmetic Coding
  */
public class CompressMe {
	
	// Register "Wavelet" operator and its RIFs
	static {
     OperationRegistry registry =
        JAI.getDefaultInstance().getOperationRegistry();
	
     registry.registerDescriptor(new WaveletDescriptor());     
     RenderedImageFactory waveletRIF = new WaveletRIF();
     RIFRegistry.register(registry, "Wavelet", "ccd-hyper",waveletRIF);
     
     registry.registerDescriptor(new QuantizationDescriptor());     
     RenderedImageFactory quantizationRIF = new QuantizationRIF();
     RIFRegistry.register(registry, "Quantization", "ccd-hyper",quantizationRIF);
   }

	
	public static void main(String args[]) {
		try {
			// Lattice Configuration
			String cfgFile = "quantizer1.xml";
			if (args.length > 1) cfgFile = args[1];
			QuantizerConfig config = new QuantizerConfig(cfgFile);
			
			PlanarImage pim = JAI.create("fileload",args[0]);
			pim = COps.reformat(pim,DataBuffer.TYPE_FLOAT);
			pim = COps.wavelet(pim,"haar",config.getLevel());
			pim = COps.quantization(pim,"lattice",config.getLevel(),config.getConfig());
					
		    QuadOutputStream qos = new QuadOutputStream(new FileOutputStream(args[0]+".coded"));						
  	 	 IndexCodingJAI ic = new IndexCodingJAI(pim,config.getLevel(),qos);
	        ic.code();
  	      qos.close();
	      
			QuadInputStream qis = new QuadInputStream(new FileInputStream(args[0]+".coded"));
		    BitOutputStream bos = new BitOutputStream(new FileOutputStream(args[0]+".compressed"));
		    
			ArithmeticCoding ac = new ArithmeticCoding(qis,bos);
			ac.code();
			qis.close();
			bos.close();
			
        } catch (Exception e) {
				System.err.println("CompressMe: "+e);
		}
	}
}

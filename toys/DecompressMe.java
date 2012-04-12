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
public class DecompressMe {
	
	// Register "Wavelet" operator and its RIFs
	static {
     OperationRegistry registry =
        JAI.getDefaultInstance().getOperationRegistry();
	
     registry.registerDescriptor(new IWaveletDescriptor());     
     RenderedImageFactory iwaveletRIF = new IWaveletRIF();
     RIFRegistry.register(registry, "IWavelet", "ccd-hyper",iwaveletRIF);
     
     registry.registerDescriptor(new DeQuantizationDescriptor());     
     RenderedImageFactory dequantizationRIF = new DeQuantizationRIF();
     RIFRegistry.register(registry, "DeQuantization", "ccd-hyper",dequantizationRIF);
   }

	
	public static void main(String args[]) {
		try {
			// Lattice Configuration
			String cfgFile = "quantizer1.xml";
			if (args.length > 1) cfgFile = args[1];
			QuantizerConfig config = new QuantizerConfig(cfgFile);


/*
			BitInputStream bis = new BitInputStream(new FileInputStream(args[0]+".compressed"));
		    QuadOutputStream qos = new QuadOutputStream(new FileOutputStream(args[0]+".coded"));
		    
			ArithmeticDecoding adc = new ArithmeticDecoding(bis,qos);
			adc.decode();
			bis.close();
			qos.close();
*/
		    QuadInputStream qis = new QuadInputStream(new FileInputStream(args[0]+".coded"));
  	 	 IndexDecodingJAI idc = new IndexDecodingJAI(config.getLevel(),qis);
	        PlanarImage pim = idc.decodeJAI();
  	      qis.close();

			pim = COps.dequantization(pim,"lattice",config.getLevel(),config.getConfig());
			pim = COps.iwavelet(pim,"haar",config.getLevel());
			pim = COps.reformat(pim,DataBuffer.TYPE_BYTE);

			hyper.dsp.COps.saveAsTIFF(pim,args[0]+".tiff");
			
        } catch (Exception e) {
				System.err.println("CompressMe: "+e);
		}
	}
}

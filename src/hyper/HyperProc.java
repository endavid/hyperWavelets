/* 
** @(#) HyperProc.java v 1.1 2002/01/08
**
**
**************************************************************************
**    Copyright (C) 2001 David Gavilan
**************************************************************************
*/

package hyper;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.awt.image.renderable.*;
import javax.swing.*;
import javax.media.jai.*;
import javax.media.jai.operator.*;
import javax.media.jai.registry.*;
import java.util.*;
import java.net.URL;
import java.io.*;
import hyper.dsp.*;
import hyper.io.*;
import hyper.coding.*;
import org.freehep.util.io.*;

/** An application class that uses <b>JAI (Java Advanced Imaging)</b> to
    operate on images.
    <P> Its main purpose from the time being is to provide methods to code images using -transform-based algorythms.
    <P> Within the application comes documentation on how to invoke those
    operations, but a basic chain of operation would be this:
    <P><code><table border=1>
    <tr><td>tileset 512</td><td>changes the tile size to 512x512</td></tr>
    <tr><td>level 2</td><td>changes the number of levels of wavelet transform to 2</td></tr>
    <tr><td>fwt haar</td><td>applies the Haar wavelet transform</td></tr>
    <tr><td>quant lattice</td><td>quantizes the transformed image using default lattices</td></tr>
    <tr><td>label</td><td>writes a labeled image to disk (default.lab)</td></tr>
    <tr><td>dequant lattice</td><td>dequantizes</td></tr>
    <tr><td>iwt haar</td><td>inverse wavelet transform</td></tr>
    <tr><td>diff</td><td>shows difference between loaded image and resulting one</td></tr>
    <tr><td>mult 4</td><td>enhances visualization of noise</td></tr>
    </table></code>
    <P>
    <h3>Configuration</h3>
    <P>The application can manage XML configuration files to select quantization parameters. There should be within the directory a file called <code>quantizer.dtd</code> which define the Document Type Definitions of our XML. Just in case, here's the DTD:
    <p><code><![CDATA[
<!ELEMENT quantizer (band+)>
<!ATTLIST quantizer level CDATA "1">
<!ELEMENT band (subband+)>
<!ATTLIST band num CDATA #REQUIRED>
<!ATTLIST band name CDATA #IMPLIED>
<!ELEMENT subband (#PCDATA)>
<!ATTLIST subband num CDATA #REQUIRED>
<!ATTLIST subband width CDATA "1">
<!ATTLIST subband height CDATA "1">
<!ATTLIST subband type CDATA "int">
<!ATTLIST subband shape CDATA "square">
<!ATTLIST subband scale CDATA "1"> ]]>
     </code>
     <P>
     <h2>Editing Menus</h2>
     <P>You can add menus to the application editing file <code>resources/HyperProc.properties</code>, or add propertiles files for different languages.
  * @version 1.1  8 Jan 2002
  * @author David Gavilan
  **/
public class HyperProc extends JFrame implements ActionListener {
  public static final int UNKNOWN = 0, TIFF = 1, BMP = 2, JPG = 3, PNM = 4,
                          GIF = 5;
  
  /** Menu options */
  JMenuBar menuBar;
  /** Panel to visualice and navigate an image */
  WaveletPanel panel;
  /** Feedback text area */
  final JTextArea log = new JTextArea(5,20);
  /** File dialog */
  final JFileChooser fc = new JFileChooser(".");
  /** Parameter dialog, to change some options */
  final JParameterDialog paramDialog;
  /** Dialog that contains help documents */
  final JDialog helpDialog;
  /** Name of the last loaded image */
  String lastLoaded;  
  /** Whether or not to show the results of applying operations over current image */
  boolean showUpdates = true;
  /** Area for the user to input commands */
  final JTextField command = new JTextField(20);
  /** This is a buffer to store last input commands (history) */
  Vector commandBuf = new Vector(20);
  /** To move around command buffer */
  int commandIndex = 0;
  /** Configuration of quantizer */
  Vector[] lastLatticeVectors = null;

  // Register "Wavelet" operator and its RIFs
  static {
     OperationRegistry registry =
        JAI.getDefaultInstance().getOperationRegistry();
	
     registry.registerDescriptor(new WaveletDescriptor());     
     RenderedImageFactory waveletRIF = new WaveletRIF();
     RIFRegistry.register(registry, "Wavelet", "ccd-hyper",waveletRIF);
     
     registry.registerDescriptor(new IWaveletDescriptor());     
     RenderedImageFactory iWaveletRIF = new IWaveletRIF();
     RIFRegistry.register(registry, "IWavelet", "ccd-hyper",iWaveletRIF);

     registry.registerDescriptor(new QuantizationDescriptor());     
     RenderedImageFactory quantizationRIF = new QuantizationRIF();
     RIFRegistry.register(registry, "Quantization", "ccd-hyper",quantizationRIF);

     registry.registerDescriptor(new DeQuantizationDescriptor());     
     RenderedImageFactory dequantizationRIF = new DeQuantizationRIF();
     RIFRegistry.register(registry, "Dequantization", "ccd-hyper",dequantizationRIF);

  }


	// Resources
    private static ResourceBundle resources;
    private static Hashtable menuItems=new Hashtable();
    static {
        try {
            resources = ResourceBundle.getBundle("hyper.resources.HyperProc", 
                                                 Locale.getDefault());
        } catch (MissingResourceException mre) {
	    System.err.println("Locale: "+Locale.getDefault());
            System.err.println("resources/HyperProc.properties not found");
            System.exit(1);
        }
    }


  /**
    * This class is used to capture keyboard events. It calls command interpreter
    * whenever <b>ENTER</b> is typed. It uses a buffer to remember last commands,
    * browsable using <b>UP</b> and <b>DOWN</b> keys.
    */
  class Accions extends KeyAdapter {
	public void keyTyped(KeyEvent e) {
	    try {
	        int code=e.getKeyChar();
		switch (code) { 
		  case KeyEvent.VK_ENTER: 
		   String s = command.getText();
		   commandBuf.add(s);
		   commandIndex = commandBuf.size();
		   interpret(s);
		   command.setText("");
		   break;
		  default:		 
		}
	    } catch(Exception ex) {
		log.append(ex+"\n");
	    }
	}	
	public void keyPressed(KeyEvent e) {
	    try {
	        int code=e.getKeyCode();
		switch (code) { 	
		  case KeyEvent.VK_UP:
		   if (commandIndex>0) commandIndex--;
		   command.setText((String)commandBuf.elementAt(commandIndex));
		   break;
		  case KeyEvent.VK_DOWN:
		   commandIndex++;
		   if (commandIndex>=commandBuf.size())
		       commandIndex=commandBuf.size()-1;
		   command.setText((String)commandBuf.elementAt(commandIndex));
		   break;
		  default:		 
		}
	    } catch(Exception ex) {
		log.append(ex+"\n");
	    }
	}	
		   

  } // end class Accions
      
  /**
    * Initialize the interface
    */
  public HyperProc() {
            
     addWindowListener(new WindowAdapter() {
         public void windowClosing(WindowEvent e) {System.exit(0);}
	 });
	         
     menuBar=createMenubar();
     setJMenuBar(menuBar);

     lastLoaded=getResourceString("InitialImage");

     Container contentPane = getContentPane();
     panel=new WaveletPanel(lastLoaded);
     panel.updateView();
     
     contentPane.add("Center",panel);
     log.setMargin(new Insets(5,5,5,5));
     log.setEditable(false);
     log.setLineWrap(true);
     log.setWrapStyleWord(true);
     log.setBackground(new Color(220,200,240));
     JScrollPane lpane=new JScrollPane(log);
     JPanel outp = new JPanel(new BorderLayout());
     
     command.addKeyListener(new Accions());
          
     outp.add(lpane,BorderLayout.CENTER);
     outp.add(command,BorderLayout.SOUTH);
     contentPane.add("South",outp);

     paramDialog = new JParameterDialog(this, "Parameter Dialog");

     helpDialog = new JBrowserDialog (JBrowserDialog.BIG,
				      getResource("htmlHelp"));

  }

  /**
    * As this class is implementing an <b>ActionListener</b>, we watch for events
    * in this method. We want to know about the menu events.   
    * The names of the item events are the same as those defined in the resource
    * properties.
    */
  public void actionPerformed(ActionEvent e) {
        JMenuItem source = (JMenuItem)(e.getSource());
        String s = "Action event detected."
                   + "\n"
                   + "    Event source: " + source.getText()
                   + " (an instance of " + getClassName(source) + ")";
        //log.append(s + "\n");	
	if (menuItems.get("load")==source) {
	    openImageCallback();
	} else if (menuItems.get("reload")==source) {
	    reloadCallback();
	} else if (menuItems.get("save")==source) {
	    saveCallback();
	} else if (menuItems.get("config")==source) {
	    openConfigCallback();
	} else if (menuItems.get("execute")==source) {
	    scriptCallback();
	} else if (menuItems.get("parameter")==source) {
	    paramDialog.pack();
            paramDialog.show();
	} else if (menuItems.get("close")==source) {
	    System.exit(0);
	} else if (menuItems.get("FWTshore")==source) {
	    fwtCallback("shore");
	} else if (menuItems.get("IWTshore")==source) {
	    iwtCallback("shore");
	} else if (menuItems.get("FWThaar")==source) {
	    fwtCallback("haar");
	} else if (menuItems.get("IWThaar")==source) {
	    iwtCallback("haar");
	} else if (menuItems.get("QnUniform")==source) {
	    quantCallback("uniform",null);
	} else if (menuItems.get("DeQnUniform")==source) {
	    dequantCallback("uniform",null);
	} else if (menuItems.get("QnSB")==source) {
	    quantCallback("sbuniform",null);
   	} else if (menuItems.get("DeQnSB")==source) {
	    dequantCallback("sbuniform",null);
 	} else if (menuItems.get("QnLattice")==source) {
	if (lastLatticeVectors == null)
	    lastLatticeVectors = iniLattice(paramDialog.getLevel(),
					  paramDialog.getLatticeWidth(),
					  paramDialog.getLatticeHeight(),
					  panel.pimg.getSampleModel().getNumBands());
	    quantCallback("lattice",lastLatticeVectors);
   	} else if (menuItems.get("DeQnLattice")==source) {
	if (lastLatticeVectors == null)
	    lastLatticeVectors = iniLattice(paramDialog.getLevel(),
					  paramDialog.getLatticeWidth(),
					  paramDialog.getLatticeHeight(),
					  panel.pimg.getSampleModel().getNumBands());
	    dequantCallback("lattice",lastLatticeVectors);
	} else if (menuItems.get("label")==source) {
		interpret("label");
    } else if (menuItems.get("unlabel")==source) {
    	interpret("labdecode");
	} else if (menuItems.get("indexcode")==source) {
		interpret("indexcode");
    } else if (menuItems.get("indexdecode")==source) {
    	interpret("indexdecode");    	
	} else if (menuItems.get("manual")==source) {	    
	    helpDialog.show();
	}

  } // end actionPerformed

  /**
    * The command interpreter. Translates a String into actions.
    * Those actions are detailed within the user documentation.
    */
  public void interpret(String com) {
    StringTokenizer stok = new StringTokenizer(com);
    if (!stok.hasMoreTokens()) return;
    String c = stok.nextToken();
    String token;
    try {
    if (c.equals("load")) {
       if (stok.hasMoreTokens()) {
         token = stok.nextToken();
		 loadImage(token);
		 lastLoaded=token;
       } else {
         openImageCallback();
       }
    } else if (c.equals("exec")) {
       if (stok.hasMoreTokens()) {
         token = stok.nextToken();
	 execScript(new File(token));
       } else {
         scriptCallback();
       }       
    } else if (c.equals("reload")) {
       reloadCallback();
    } else if (c.equals("save")) {
       if (stok.hasMoreTokens()) {
        token = stok.nextToken();
	if (saveAs(token)) {
 	  log.append(hora()+"Saved \""+token+"\"\n");
	} else {
	  log.append(hora()+"Unknown file format \""+token+"\"\n");
	}
       } else {
        saveCallback();
       } 
    } else if (c.equals("fwt")) {
       if (stok.hasMoreTokens()) {
         token = stok.nextToken();

	 if (token.equals("shore") || token.equals("haar")) {
   	   fwtCallback(token);
	 } else {
	   log.append(hora()+"<type>=(shore|haar)\n");
	 }
       } else {
         log.append(hora()+"fwt <type> [source [destination]]\n");
       }
    } else if (c.equals("iwt")) {
       if (stok.hasMoreTokens()) {
         token = stok.nextToken();

	 if (token.equals("shore") || token.equals("haar")) {
  	   iwtCallback(token);
	 } else {
	   log.append(hora()+"<type>=(shore|haar)\n");
	 }

       } else {
         log.append(hora()+"iwt <type> [source [destination]]\n");
       }
    } else if (c.equals("quant")) {
       if (stok.hasMoreTokens()) {
         String ag = stok.nextToken();

	 if (ag.equals("uniform") || ag.equals("sbuniform")) {
	   int level = paramDialog.getLevel();
	   int subBands = 3*level+1;
	   int bands = panel.pimg.getSampleModel().getNumBands();

	   Vector[] v = new Vector[bands];
	   for (int k=0;k<bands;k++) {
	       v[k] = new Vector(subBands);
	       for (int i=0;i<subBands;i++) v[k].add(new Float(0f));
	   }
	   int i; float val;
	   while (stok.hasMoreTokens()) {
	     token = stok.nextToken();
	     i = Integer.parseInt(token);
	     if (stok.hasMoreTokens()) {
		 token = stok.nextToken();
		 int band = Integer.parseInt(token);

	       token = stok.nextToken();
	       val = Float.parseFloat(token);
	       v[band].set(i,new Float(val));
	     }
	   }
  	   quantCallback(ag,v);
	 } else if (ag.equals("lattice")) {
	   int level = paramDialog.getLevel();
	   int bands = panel.pimg.getSampleModel().getNumBands();

	   int subBands = 3*level+1;
	   Vector[] v;
	   if (lastLatticeVectors == null) {
	       v = iniLattice(paramDialog.getLevel(),
				 paramDialog.getLatticeWidth(),
				 paramDialog.getLatticeHeight(), bands);
	   } else {
	       v = lastLatticeVectors;
	   }

	   int i; float val;
	   while (stok.hasMoreTokens()) {
	     token = stok.nextToken();
	     i = Integer.parseInt(token);
	     if (stok.hasMoreTokens()) {
	       token = stok.nextToken();
	       int band=Integer.parseInt(token);

	       token = stok.nextToken();
	       int lattw=Integer.parseInt(token);
	       token=stok.nextToken();
	       int latth=Integer.parseInt(token);
	       token=stok.nextToken();
	       val = Float.parseFloat(token);
	       v[band].set(i,new ParamLattice(ParamLattice.INTEGER,lattw,latth,val));
	     }
	   }
	   lastLatticeVectors = v;
	   quantCallback(ag,v);
	 } else {
	   log.append(hora()+"<type>=(uniform|sbuniform|lattice)\n");
	 }

       } else {
         log.append(hora()+"quant <type> [band coef band coef ...]\n");
       }
    } else if (c.equals("dequant")) {
       if (stok.hasMoreTokens()) {
         String ag = stok.nextToken();

	 if (ag.equals("uniform") || ag.equals("sbuniform")) {
	   int level = paramDialog.getLevel();
	   int subBands = 3*level+1;
	   int bands = panel.pimg.getSampleModel().getNumBands();

	   Vector[] v = new Vector[bands];
	   for (int k=0;k<bands;k++) {
	       v[k] = new Vector(subBands);
	       for (int i=0;i<subBands;i++) v[k].add(new Float(0f));
	   }
	   int i; float val;
	   while (stok.hasMoreTokens()) {
	     token = stok.nextToken();
	     i = Integer.parseInt(token);
	     if (stok.hasMoreTokens()) {
		 token = stok.nextToken();
		 int band = Integer.parseInt(token);

	       token = stok.nextToken();
	       val = Float.parseFloat(token);
	       v[band].set(i,new Float(val));
	     }
	   }
  	   dequantCallback(ag,v);
	 } else if (ag.equals("lattice")) {
	   int level = paramDialog.getLevel();
	   int subBands = 3*level+1;
	   int bands = panel.pimg.getSampleModel().getNumBands();

	   Vector[] v;
	   if (lastLatticeVectors == null) {
	       v = iniLattice(paramDialog.getLevel(),
				 paramDialog.getLatticeWidth(),
				 paramDialog.getLatticeHeight(),bands);
	   } else {
	       v = lastLatticeVectors;
	   }

	   int i; float val;
	   while (stok.hasMoreTokens()) {
	     token = stok.nextToken();
	     i = Integer.parseInt(token);
	     if (stok.hasMoreTokens()) {	 
	       token = stok.nextToken();
	       int band=Integer.parseInt(token);

	       token = stok.nextToken();
	       int lattw=Integer.parseInt(token);
	       token=stok.nextToken();
	       int latth=Integer.parseInt(token);
	       token=stok.nextToken();
	       val = Float.parseFloat(token);
	       v[band].set(i,new ParamLattice(ParamLattice.INTEGER,lattw,latth,val));
	     }
	   }
	   lastLatticeVectors = v;
	   dequantCallback(ag,v);
	 } else {
	   log.append(hora()+"<type>=(uniform|sbuniform|lattice)\n");
	 }

       } else {
         log.append(hora()+"dequant <type> [band coef band coef ...]\n");
       }
    } else if (c.equals("label")) {
		if (lastLatticeVectors == null)
		    lastLatticeVectors = iniLattice(paramDialog.getLevel(),
					  paramDialog.getLatticeWidth(),
					  paramDialog.getLatticeHeight(),
					  panel.pimg.getSampleModel().getNumBands());
		String fname = "default.lab";
		if (stok.hasMoreTokens()) fname=stok.nextToken();
		labelling(fname,lastLatticeVectors);
    } else if (c.equals("labdecode")) {
		if (lastLatticeVectors == null)
		    lastLatticeVectors = iniLattice(paramDialog.getLevel(),
					  paramDialog.getLatticeWidth(),
					  paramDialog.getLatticeHeight(),
					  panel.pimg.getSampleModel().getNumBands());
		String fname = "default.lab";
		if (stok.hasMoreTokens()) fname=stok.nextToken();
		labelDecoding(fname,lastLatticeVectors);
    } else if (c.equals("config")) { // load QuantizerConfiguration
		if (lastLatticeVectors == null)
		    lastLatticeVectors = iniLattice(paramDialog.getLevel(),
					  paramDialog.getLatticeWidth(),
					  paramDialog.getLatticeHeight(),
					  panel.pimg.getSampleModel().getNumBands());
		String fname = "default.xml";
		if (stok.hasMoreTokens()) fname=stok.nextToken();
		QuantizerConfig qc=new QuantizerConfig(fname);
		lastLatticeVectors = qc.getConfig();
		paramDialog.setLevel(qc.getLevel());

		log.append(hora()+"Quantizer Configuration Loaded\n");
    } else if (c.equals("mult")) {
       if (stok.hasMoreTokens()) {
         token = stok.nextToken();
	 	float v = Float.parseFloat(token);
	 	multiply(v);
         log.append(hora()+"multiplied\n");       
       } else {
         log.append(hora()+"mult (const)\n");
       }
    } else if (c.equals("visualization")) {
    	String param=new String("absolute");
    	if (stok.hasMoreTokens()) param = stok.nextToken();
    	visualization(param);    	
    } else if (c.equals("diff")) {
      differenceCallback();
      log.append(hora()+"Difference\n");
    } else if (c.equals("blur")) {
      blur();
      log.append(hora()+"blurred\n");
    } else if (c.equals("sharpen")) {
      sharpen();
      log.append(hora()+"sharpened\n");
    } else if (c.equals("medianf")) {
      medianFilter();
      log.append(hora()+"median filtered\n");
    } else if (c.equals("dct")) {
      dct();
      log.append(hora()+"Discret Cosinus Transform\n");
    } else if (c.equals("idct")) {
      idct();
      log.append(hora()+"Inverse Discret Cosinus Transform\n");
    } else if (c.equals("gradmag")) {
      gradientMagnitude();
      log.append(hora()+"Gradient Magnitude applied\n");
    } else if (c.equals("byte")) {
      toByte();
      log.append(hora()+"Image datatype changed to byte.\n");
    } else if (c.equals("int")) {
      toInt();
      log.append(hora()+"Image datatype changed to integer.\n");      
    } else if (c.equals("float")) {
      toFloat();
      log.append(hora()+"Image datatype changed to float.\n");      
    } else if (c.equals("show")) {
      if (!showUpdates) panel.updateView();
      showUpdates = true;
      log.append(hora()+"The results of ops are visible\n");
    } else if (c.equals("hide")) {
      showUpdates = false;
      log.append(hora()+"Now you won't see the results of ops\n");
    } else if (c.equals("size")) {
	  int width = panel.getTileWidth(), height = panel.getTileHeight();
      log.append(hora()+"size: "+panel.imageXSize()+"x"+panel.imageYSize()+
        "x"+panel.getNChannels()+" - tile: "+width+"x"+height+"\n");
    } else if (c.equals("type")) {
	log.append(hora()+"type: "+
           COps.getDataTypeName(panel.getDataType())+"\n");
    } else if (c.equals("retile")) {
	PlanarImage tmp = panel.pimg;
	panel.pimg=new TiledImage(panel.pimg,false);
	tmp.dispose();
	log.append(hora()+"copied.\n");
    } else if (c.equals("tileset")) {
       if (stok.hasMoreTokens()) {
         token = stok.nextToken();
		 int v1 = Integer.parseInt(token);
		 int v2;
	 	if (stok.hasMoreTokens()) v2 = Integer.parseInt(stok.nextToken());
	 	else v2=v1;
	 	panel.setTiles(v1,v2);
	 	if (showUpdates) panel.updateView();
	 
         log.append(hora()+"Retiled\n");       
       } else {
         log.append(hora()+"tileset (width) [height]\n");
       }
    } else if (c.equals("level")) {
	if (stok.hasMoreTokens()) {
	    token=stok.nextToken();
	    int nl = Integer.parseInt(token);
	    if (nl>0) {
		paramDialog.setLevel(nl);
	    }
	}
	log.append(hora()+"number of levels: "+paramDialog.getLevel()+"\n");
	} else if (c.equals("indexcode")) {
		String fname = "default.coded";
		if (stok.hasMoreTokens()) fname=stok.nextToken();
		indexCodingCallBack(fname);
	} else if (c.equals("indexdecode")) {
		String fname = "default.coded";
		if (stok.hasMoreTokens()) fname=stok.nextToken();
		indexDecodingCallBack(fname);
    } else if (c.equals("close") || c.equals("quit")) {
      System.exit(0);
    } else if (c.equals("help") || c.equals("?")) {
	    helpDialog.show();    	
    } else {
    	log.append("Unknown command. Type \"help\" for a list of available commands.\n");    	
    }
    } catch (Exception e) {
      log.append("interpret: "+e+"\n");
    }
  } // end interpret

  /**
    * Loads an image into the image panel.
    * @param s the file name
    */
    void loadImage(String s) throws FileNotFoundException, IOException {
     panel.loadImage(s);  
     panel.revalidate();   
     validate();     
  }
  
  /**
    * Opens a file-load requester to load a configuration file for the quantization
    */
  void openConfigCallback() {
    try {
    int returnVal = fc.showOpenDialog(this);
    if (returnVal == JFileChooser.APPROVE_OPTION) {
	QuantizerConfig qc =
	    new QuantizerConfig((fc.getSelectedFile()).getAbsolutePath(), true);
      
	lastLatticeVectors = qc.getConfig();
	paramDialog.setLevel(qc.getLevel());
      
      log.append(hora()+"Loaded Quantizer Configuration \n");
    } else {
      log.append(hora()+"Open command cancelled by user.\n");
    }
    } catch (Exception e) {
      log.append("config load: "+e+"\n");
    }
  } // end openImageCallback
  
  /**
    * Opens a file-load requester to load an image
    */
  void openImageCallback() {
    try {
    int returnVal = fc.showOpenDialog(this);
    if (returnVal == JFileChooser.APPROVE_OPTION) {
      lastLoaded = (fc.getSelectedFile()).getAbsolutePath();
      loadImage(lastLoaded);

      
      log.append(hora()+"Loaded \""+lastLoaded+"\"\n");
    } else {
      log.append(hora()+"Open command cancelled by user.\n");
    }
    } catch (Exception e) {
      log.append("load: "+e+"\n");
    }
  } // end openImageCallback
  
  /**
    * Reloads last loaded image
    */
  void reloadCallback() {
      try {
      
	  loadImage(lastLoaded);
      
	  log.append(hora()+"Loaded \""+lastLoaded+"\"\n");
      } catch (Exception e) {
	  log.append(e+"\n");
      }
  }
  
    /**
      * Checks whether the image in the Image Panel is square or not
      * @param operation a String representing the operation which wants to
      *        check this condition
      */
    boolean isSquare(String operation) {
      if (!panel.isSquare()) {
	  log.append("I cannot apply \""+operation+"\" to non-square tiles.\n"+
		     "Image Size: "+panel.imageXSize()+"x"+
		     panel.imageYSize()+" Tile Size: "+
		     panel.getTileWidth()+"x"+panel.getTileHeight()+"\n");
	  return false;
      }
      return true;
    }

    /**
      * Checks whether the image in the Image Panel is multiple of 2^level
      * @param operation a String representing the operation which wants to
      *        check this condition
      */
    boolean canBeDecomposed(String operation) {
		int level = paramDialog.getLevel();
		int factor = 1<<level;
		int width = panel.getTileWidth(), height = panel.getTileHeight();
		if ((width % factor !=0) ||
	    	(height % factor !=0)) {
	    	log.append("Operation \""+operation+"\" can not be applied to "+
		       width+"x"+height+" tiles being level "+level+"\n");
	    	return false;
		}
		return true;
    }

    /**
      * Check if there is any subband that cannot be filled with selected
      * lattice size
      * @param plist a Vector containing the <code>ParamLattice</code>s
      */
    boolean doLatticesFit(Vector[] plist) {
	int level = paramDialog.getLevel();
	int bands = panel.pimg.getSampleModel().getNumBands();	
	int subbands=level*3+1;
	
	for (int k=0;k<bands;k++) {
	    if (plist[k].size()<subbands) {
		log.append("The number of subbands and number of lattices defined doesn't match.\n");
		return false;
	    }

	    int width=panel.getTileWidth(), height=panel.getTileHeight();
	    width >>= level; height >>=level;
	    for (int i=0;i<subbands;i++) {
		ParamLattice pl=(ParamLattice)plist[k].get(i);
		if ((width % pl.getWidth() != 0) ||
		    (height % pl.getHeight() !=0)) {
		    log.append("Band "+k+" - ");
		    log.append("Subband "+i+" has wrong size\n");
		    log.append("Subband size: "+width+"x"+height+" Tile size: "
			       +pl.getWidth()+"x"+pl.getHeight()+"\n");
		    return false;
		}
		if ((i!=0) && (i%3==0)) {
		    width <<=1;
		    height <<=1;
		}
	    }
	}
	return true;

    }

  /**
    * Called to do <b>Forward Wavelet Transform</b> on the current image.
    * @param algorythm the algorythm to use (haar, shore, ...)
    */
  void fwtCallback(String algorythm) {

      //if (!isSquare("Wavelet")) return;
      if (!canBeDecomposed("Wavelet")) return;

    try {
    
      //runGc();
      RenderedOp rop = 
        COps.wavelet(panel.pimg,algorythm,paramDialog.getLevel());      
      panel.pimg = rop.createInstance();

      if (showUpdates) panel.updateView();

      log.append(hora()+"Forward "+algorythm+" DWT\n");
    } catch (Exception e) {
      log.append("FWT: "+e+"\n");     
    }
  }

  /**
   * Called to do <b>Inverse Wavelet Transform</b> on the CURRENT image.
   * @param algorythm the algorythm to use (haar, shore, ...)
   */
  void iwtCallback(String algorythm) {
       //if (!isSquare("IWavelet")) return;
       if (!canBeDecomposed("IWavelet")) return;

    try {
      RenderedOp rop = 
        COps.iwavelet(panel.pimg,algorythm,paramDialog.getLevel());
      panel.pimg = rop.createInstance();
      
      if (showUpdates) panel.updateView();
    
      log.append(hora()+"Inverse DWT\n");
    } catch (Exception e) {
      log.append(hora()+e);
    }
  }

  /**
   * Called to do quantization on the current image.
   * @param kind type of quantization (uniform, sbuniform, lattice)
   * @param coefs a <b>Vector</b> containing a list of coeficients per subband
   *              just int values in sbuniform or <b>ParamLattices</b>.
   * @see hyper.dsp.ParamLattice
   * @see hyper.dsp.COps#quantization
   */
  void quantCallback(String kind, Vector[] coefs) {
    if (kind.equals("lattice")) if (!doLatticesFit(coefs)) return;

    int level=paramDialog.getLevel();
    // in uniform quantization, level is used as the quantization ratio
    if (kind.equals("uniform")) level=paramDialog.getUniRatio();

    panel.pimg = COps.quantization(panel.pimg,kind,level,coefs);
    if (showUpdates) panel.updateView();

    log.append(hora()+"Quantization: "+kind+"\n");

  }

  /**
   * Called to do dequantization on the current image.
   * @param kind type of quantization (uniform, sbuniform, lattice)
   * @param coefs a <b>Vector</b> containing a list of coeficients per subband
   *              just int values in sbuniform or <b>ParamLattices</b>.
   * @see hyper.dsp.ParamLattice
   * @see hyper.dsp.COps#dequantization
   */
  void dequantCallback(String kind, Vector[] coefs) {
    if (kind.equals("lattice")) if (!doLatticesFit(coefs)) return;

    int level=paramDialog.getLevel();
    // in uniform quantization, level is used as the quantization ratio
    if (kind.equals("uniform")) level=paramDialog.getUniRatio();

    panel.pimg = COps.dequantization(panel.pimg,kind,level,coefs);
    if (showUpdates) panel.updateView();
    
    log.append(hora()+"Dequantization: "+kind+"\n");
  }

  /**
    * Labels a quantized image given a set of lattices, and writes it to disk.
    * @param fname File name of the output file.
    * @param v The <b>Vector</b> containing the lattices.
    * @see hyper.dsp.LabellingJAI
    */
  void labelling(String fname, Vector[] v) {
      try {
		  LabellingJAI lab = new LabellingJAI(fname);
 
		  lab.imageLabelling(panel.pimg,paramDialog.getLevel(),v);      
		  log.append(hora()+"Labelled: \""+fname+"\"\n");
      } catch (Exception e) {
		  log.append(hora()+"Labelling: "+e+"\n");
      }
  }

  /**
    * Loads a labelled image from disk, given a set of lattices.
    * @param fname File name of the input file.
    * @param v The <b>Vector</b> containing the lattices.
    * @see hyper.dsp.LabellingJAI
    */
  void labelDecoding(String fname, Vector[] v) {
      try {
		  LabellingJAI lab = new LabellingJAI();
		  FileInputStream fis = new FileInputStream(fname);
		  BitInputStream bit = new BitInputStream(fis);
	 	 panel.pimg=lab.imageDecoding(bit,paramDialog.getLevel(),v);
	  	if (showUpdates) panel.updateView();
		  log.append(hora()+"Labels Decoded: \""+fname+"\"\n");	  	
      } catch (Exception e) {
	  	log.append(hora()+"Lab Decode: "+e+"\n");
      }
  }

  void indexCodingCallBack(String fname) {
      try {
		  FileOutputStream fos = new FileOutputStream(fname);
	 	 QuadOutputStream qos = new QuadOutputStream(fos);
	 	 IndexCodingJAI ic = new IndexCodingJAI(panel.pimg,paramDialog.getLevel(),qos);
	      ic.code();
	      qos.close();
		  log.append(hora()+"Index Coded: \""+fname+"\"\n");	      
      } catch (Exception e) {
		  log.append(hora()+"Index code: "+e+"\n");
      }
  }

  void indexDecodingCallBack(String fname) {
      try {
		  FileInputStream fis = new FileInputStream(fname);
	 	 QuadInputStream qis = new QuadInputStream(fis);
	 	 IndexDecodingJAI id = new IndexDecodingJAI(paramDialog.getLevel(),qis);
	      panel.pimg = id.decodeJAI();
	      qis.close();
	      if (showUpdates) panel.updateView();
		  log.append(hora()+"Index Decoded: \""+fname+"\"\n");	      	      
      } catch (Exception e) {
		  log.append(hora()+"Index code: "+e+"\n");
      }
  }



  /**
   * Open a file requester to save current image to a file.
   */
  void saveCallback() {
    int returnVal = fc.showSaveDialog(this);
    if (returnVal == JFileChooser.APPROVE_OPTION) {
      String fullname = (fc.getSelectedFile()).getAbsolutePath();
      try {
        boolean saved = saveAs(fullname);
	
	if (saved){
          JOptionPane.showMessageDialog(null, "File saved", "confirmation",
                    JOptionPane.INFORMATION_MESSAGE); 
	  log.append(hora()+"Saved \""+fullname+"\"\n");
	} else {
	  log.append("Unknown format\n");
	}
      } catch (Exception e) {
        JOptionPane.showMessageDialog(null, "Error saving", "alert",
                    JOptionPane.ERROR_MESSAGE); 
   	log.append(hora()+"Error saving \""+fullname+"\"\n");
      }      
    } else {
      log.append(hora()+"Save command cancelled by user.\n");
    }
  }

  /**
    * Called to save current image to a file
    * @param fullname the file name of the output file
    * @throws java.io.IOException if the file name does not exist or something
    */
  boolean saveAs(String fullname) throws java.io.IOException {
    boolean saved = true;
    
    switch (getExtension(fullname)) {
	  case JPG:
	    panel.saveAsJPG(fullname);
	    break;
	  case TIFF:
	    panel.saveAsTIFF(fullname);
	    break;
	  case BMP:
	    panel.saveAsBMP(fullname);
	    break;	    
	  case PNM:
	    panel.saveAsPNM(fullname);
	    break;
	  default:
	    saved = false;
    }
    return saved;
  }
  
  /**
    * Executes a script file
    * @param f the file name of the script
    * @throws IOException
    */
  void execScript(File f) throws IOException {
      String script = f.getAbsolutePath();
      BufferedReader br = new BufferedReader(new FileReader(f));
      log.append(hora()+"[Begin \""+script+"\"]\n");
           
     
      String comm;
      do{
	  	comm = br.readLine();
	  	if (comm!=null) interpret(comm);	   
	  } while (comm!=null);
	     
      log.append(hora()+"[End \""+script+"\"]\n");
  }
  
  /**
    * Opens a file requester to select a script file to execute
    */
  void scriptCallback() {
    try {
    int returnVal = fc.showOpenDialog(this);
    if (returnVal == JFileChooser.APPROVE_OPTION) {
      execScript(fc.getSelectedFile());
      /*
      String script = (fc.getSelectedFile()).getAbsolutePath();
      BufferedReader br = new BufferedReader(new
                          FileReader(fc.getSelectedFile()));
      log.append(hora()+"[Begin \""+script+"\"]\n");
           
     
        String comm;
        do{
	  comm = br.readLine();
	  if (comm!=null) interpret(comm);	   
	} while (comm!=null);
	
      
      log.append(hora()+"[End \""+script+"\"]\n");
      */
    } else {
      log.append(hora()+"Execution cancelled by user.\n");
    }
    } catch (Exception e) {
      log.append("script: "+e+"\n");
    }     
  } // end scriptCallback

  /**
   * A encoder toolkit by doing FWT and quantization.
   */
  void encoderCallback(String kind) {
/*    BufferedImage in = panel.getBufferedImage();
    int size = in.getWidth();
    int out[][][] = new int[panel.getNChannels()][size][size];
        
    wavelet.fwt(in.getRaster(),lastOpResult,in.getWidth(),paramDialog.getLevel());
    quant.quantize(lastOpResult, out, size);
    panel.setBufferedImage(out);
    lastOpResult=out;

    log.append(hora()+"Encode: "+kind+"\n");*/

  }


  /**
   * A decoder toolkit by doing IWT and dequantization.
   */
  void decoderCallback(String kind) {
/*
    int size = lastOpResult[0].length;
    int out[][][] = new int[panel.getNChannels()][size][size];

    quant.dequantize(lastOpResult, out, size);
    wavelet.iwt(out, out, size, paramDialog.getLevel());
    panel.setBufferedImage(out);    
    lastOpResult = out;
*/
    log.append(hora()+"Decode: "+kind+"\n");

  }



  // Operaciones (convoluciones, etc)

  /**
    * Multiplies the current image by a constant value.
    * @param val a float value
    * @see hyper.dsp.COps#multiply
    */
  public void multiply(double val) {
    panel.pimg = COps.multiply(panel.pimg,val);
    if (showUpdates) panel.updateView();
  }
  
  /**
    * Applies a gaussian blur to the current image
    * @see hyper.dsp.COps#blur
    */
  public void blur() {
    panel.pimg = COps.blur(panel.pimg);
    if (showUpdates) panel.updateView();
  }

  /**
    * Applies a convolution to sharpen the current image
    * @see hyper.dsp.COps#sharpen
    */
  public void sharpen() {
    panel.pimg = COps.sharpen(panel.pimg);
    if (showUpdates) panel.updateView();    
  }

  public void visualization(String mode) {
	if (mode.equals("absolute"))
	  panel.setVisualizationMode(ImageDisplay.ABSOLUTE);
	else if (mode.equals("rescale"))
	  panel.setVisualizationMode(ImageDisplay.RESCALE);
	else if (mode.equals("inverse"))
	  panel.setVisualizationMode(ImageDisplay.INVERSE);
	else {
		log.append("Unknown Visualization Mode\n");
		return;
	}
    if (showUpdates) panel.updateView();
    log.append(hora()+"changed visualization\n");
  }


  /**
    * Applies the median filter
    * @see hyper.dsp.COps#medianFilter
    */
  public void medianFilter() {
    panel.pimg = COps.medianFilter(panel.pimg,
       MedianFilterDescriptor.MEDIAN_MASK_SQUARE,3);
    if (showUpdates) panel.updateView();       
  }

  /**
    * Applies the Discrete Cosinus Transform
    * @see hyper.dsp.COps#DCT
    */
  public void dct() {
    panel.pimg = COps.DCT(panel.pimg);
    if (showUpdates) panel.updateView();       
  }

  /**
    * Applies the Inverse Discrete Cosinus Transform
    * @see hyper.dsp.COps#inverseDCT
    */
  public void idct() {
    panel.pimg = COps.inverseDCT(panel.pimg);
    if (showUpdates) panel.updateView();       
  }

  /**
    * Applies the gradient magnitude
    * @see hyper.dsp.COps#sobelGradientMagnitude
    */
  public void gradientMagnitude() {
    panel.pimg = COps.sobelGradientMagnitude(panel.pimg);
    if (showUpdates) panel.updateView();
  }

  /**
    * Changes image datatype to byte
    * @see hyper.dsp.COps#reformat
    */
  public void toByte() {
    panel.pimg = COps.reformat(panel.pimg, DataBuffer.TYPE_BYTE);
    if (showUpdates) panel.updateView();
  }

  /**
    * Changes image datatype to int
    * @see hyper.dsp.COps#reformat
    */
  public void toInt() {
    panel.pimg = COps.reformat(panel.pimg, DataBuffer.TYPE_INT);
    if (showUpdates) panel.updateView();
  }
  
  /**
    * Changes image datatype to float
    * @see hyper.dsp.COps#reformat
    */
  public void toFloat() {
    panel.pimg = COps.reformat(panel.pimg, DataBuffer.TYPE_FLOAT);
    if (showUpdates) panel.updateView();
  }
  
   /** 
     * Initializes the Lattice Coefs like SBUniform, with a default
     * lattice width and height, except from subband 0 (1x1)
     * @param level number of levels of the wavelet transform
     * @param lw default lattice width
     * @param lh default lattice height
     * @param bands number of channels or bands of the image = #Vectors
     */
   public Vector[] iniLattice(int level, int lw, int lh, int bands) {
      int subBands = 3*level+1;
      
      Vector[] coefs = new Vector[bands];

      for (int k=0;k<bands;k++) {
	  coefs[k] = new Vector();
	  int t = ParamLattice.INTEGER;
	  coefs[k].add(new ParamLattice(t,1,1,1f));
	  int lev = 2;
	  for (int i=1;i<subBands;i+=3) {
	      coefs[k].add(new ParamLattice(t,lw,lh,(float)lev)); 
	      coefs[k].add(new ParamLattice(t,lw,lh,(float)lev));
	      coefs[k].add(new ParamLattice(t,lw,lh,(float)lev));
	      lev <<=1;
	  }
      }
      return coefs;
   }     
  
  /**
   * Called to display the difference between compressed image
   * and original image (last loaded one)

   */
  void differenceCallback() {
  // cargar la imagen lastLoaded y hacer la diferencia con lastOpResult
    int i,j,c;
    //BufferedImage imgB;
    PlanarImage imgAnt;
    try {
      imgAnt = panel.load(lastLoaded);      
      
      panel.pimg=COps.subtract(imgAnt,panel.pimg);
      panel.updateView();

      double[] mse=COps.computeMSE(panel.pimg);
      double psnr[] = new double[panel.getNChannels()];
      for (c=0;c<panel.getNChannels();c++) {
       psnr[c] = 10*Math.log(255f*255f/mse[c])/Math.log(10);
      }

      log.append(hora()+"Difference - PSNR:"
        +verV(psnr,panel.getNChannels())+" MSE: "
        +verV(mse,panel.getNChannels())+"\n");

    } catch (Exception e) {
      log.append(""+e);
    }

  } // end differenceCallback
    

  /**
    * Returns just the class name -- no package info.
    * @param o the Object of which you want to know its class
    */
  protected String getClassName(Object o) {
        String classString = o.getClass().getName();
        int dotIndex = classString.lastIndexOf(".");
        return classString.substring(dotIndex+1);
  }
      
  public static void main(String s[]) {
      HyperProc window = new HyperProc();
      
      Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();     

      window.setTitle(window.getResourceString("Title"));
      window.setSize((int)(dim.width*3/4.0),(int)(dim.height*3/4.0));
      center(window);
      window.setVisible(true);
      
      /* Toolbar */
      /*
      JFrame w2 = new JFrame();
      w2.setTitle("Toolbar");
      w2.setSize(100,450);
      w2.setVisible(true);*/
            
  }

  /**
    * Centers window on screen
    * @param f the window to center
    */
  public static void center(Frame f) {
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    Dimension frameSize = f.getSize();
    int x = (screenSize.width - frameSize.width) / 2;
    int y = (screenSize.height - frameSize.height) / 2;
    f.setLocation(x,y);
  }


    /**
     * Create the menubar for the app.  By default this pulls the
     * definition of the menu from the associated resource file. 
     */
    protected JMenuBar createMenubar() {
	JMenuItem mi;
	JMenuBar mb = new JMenuBar();

	String[] menuKeys = tokenize(getResourceString("menubar"));
	for (int i = 0; i < menuKeys.length; i++) {
	    JMenu m = createMenu(menuKeys[i]);
	    if (m != null) {
		mb.add(m);
	    }
	}
	return mb;
    }

   /**
     * Create a menu for the app.  By default this pulls the
     * definition of the menu from the associated resource file.
     */
    protected JMenu createMenu(String key) {
	String[] itemKeys = tokenize(getResourceString(key));
	JMenu menu = new JMenu(getResourceString(key + "Label"));
	for (int i = 0; i < itemKeys.length; i++) {
	    if (itemKeys[i].equals("-")) {
		menu.addSeparator();
	    } else {
		JMenuItem mi = createMenuItem(itemKeys[i]);
		menu.add(mi);
	    }
	}
	return menu;
    }


    protected JMenuItem createMenuItem(String cmd) {
	JMenuItem mi;
	String lab=getResourceString(cmd+"Label");
	String img=getResourceString(cmd+"Image");
	if(img!=null)
	    mi=addItem(lab,img);
	else
	    mi=addItem(lab);
	menuItems.put(cmd,mi); // nos lo guardamos en la tabla hash
	return mi;
    }


  /**
   * Creates a menu element (menu option)
   * @param s the string that represents that option
   */ 
  JMenuItem addItem(String s) {
    JMenuItem menuItem=new JMenuItem(s);
    menuItem.addActionListener(this);
    return menuItem;
  }
  
  /**
   * Creates a menu element (menu option) with a image
   * @param s the string that represents that option
   * @param img the file name of the image
   */
  JMenuItem addItem(String s,String img) {
    JMenuItem menuItem=new JMenuItem(s,new ImageIcon(img));
    menuItem.addActionListener(this);
    return menuItem;
  }

  /**
   * Returns a string with system's time
   */
  String hora() {
    Calendar c = new GregorianCalendar();
    int min=c.get(Calendar.MINUTE);
    return ("<"+c.get(Calendar.HOUR_OF_DAY)+":"+((min<10)?"0"+min:""+min)+"> ");
  }

  /**
   * Returns a string to display an array of doubles this way: (1.0, 2.0, ...)
   * @param v the vector (array) to display
   */
  public static String verV(double[] v){
    String s="(";
    for (int i=0;i<v.length;i++) {
       s += v[i];
       if (i<v.length-1) s+=", ";
    }
    s += ")";
    
    return s;
  }
  

  /**
   * Returns a string to display 'c' elements of an array of doubles this way:
   * (1.0, 2.0, ...)
   * @param v the vector (array) to display
   * @param c number of elements to display
   */
  public static String verV(double[] v,int c){
    String s="";
    if (c>1) s += "(";
    for (int i=0;i<c;i++) {
       s += v[i];
       if (i<c-1) s+=", ";
    }
    if (c>1) s += ")";
    
    return s;
  }

  /**
    * Returns a constant representing the image type from a file name
    * @param s a string corresponding to a file name
    */
  public static int getExtension(String s) {
     s = s.toLowerCase();
     if (s.endsWith(".jpg") ||
         s.endsWith(".jpeg"))
	 return JPG;
     if (s.endsWith(".bmp"))
	 return BMP;
     if (s.endsWith(".gif"))
	 return GIF;	 
     if (s.endsWith(".tif") ||
         s.endsWith(".tiff"))
	 return TIFF;
     if (s.endsWith(".pgm") ||
         s.endsWith(".ppm"))
	 return PNM;
     return UNKNOWN;
  }
  
  /** forces collecting garbage */
  public static void runGc() {
    Runtime rt = Runtime.getRuntime();
    rt.gc();
    long mem = rt.freeMemory();
    System.out.println("Free Memory = "+ mem);
  }


    protected String getResourceString(String nm) {
	String str;
	try {
	    str = resources.getString(nm);
	    //System.out.println("Resource: "+str);
	} catch (MissingResourceException mre) {
	    str = null;
	}
	return str;
    }

    protected URL getResource(String key) {
	String name = getResourceString(key);
	if (name != null) {
	    URL url = this.getClass().getResource(name);
	    return url;
	}
	return null;
    }

    /**
     * Take the given string and chop it up into a series
     * of strings on whitespace boundries.  This is useful
     * for trying to get an array of strings out of the
     * resource file.
     */
    protected String[] tokenize(String input) {
	if (input==null) return null;
	Vector v = new Vector();
	StringTokenizer t = new StringTokenizer(input);
	String cmd[];

	while (t.hasMoreTokens())
	    v.addElement(t.nextToken());
	cmd = new String[v.size()];
	for (int i = 0; i < cmd.length; i++)
	    cmd[i] = (String) v.elementAt(i);

	return cmd;
    }

 
}

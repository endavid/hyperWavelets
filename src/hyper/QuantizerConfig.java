package hyper;

import javax.xml.parsers.*;
import org.w3c.dom.*;
import java.io.*;
import java.util.Vector;
import hyper.dsp.ParamLattice;


/** This class uses the XML parser and DOM to read a configuration file
  * and generate quantization parameters.
  * @author David Gavilan
  */
public class QuantizerConfig {

    protected int level;
    protected Vector[] coefs;
    protected boolean debug;
    
    /** Constructs a Quantizer Configuration object from an XML file.
     * @param filename path to the XML file
     */
    public QuantizerConfig(String filename)
	    throws javax.xml.parsers.ParserConfigurationException, org.xml.sax.SAXException, 
 	   java.io.IOException {     
		this(filename, false);
    }

    /** Constructs a Quantizer Configuration object from an XML file.
     *  @param filename path to the XML file
     *  @param debug whether or not to show some output to the console.
     */
    public QuantizerConfig(String filename, boolean debug) 
    throws javax.xml.parsers.ParserConfigurationException, org.xml.sax.SAXException,
    java.io.IOException {

		this.debug = debug;

	    DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
	    File f = new File(filename);
	    Document doc = db.parse(f);
	    Element root = doc.getDocumentElement();
	    level = Integer.parseInt(root.getAttribute("level"));
	     
	    int numsubbands = 3*level+1;

	    NodeList bands = root.getElementsByTagName("band");
	    int n = bands.getLength();
	    
	    // al menos habra una banda
	    coefs = new Vector[n];
	    
	    for (int i=0;i<n;i++) {
		// cada item es un Node generico (CDATA, etc..), pero
		// hacemos un cast a Element, que es lo que sabemos que
		// devuelve el getElementsByTagName
		Element item = (Element)bands.item(i);

		int band = Integer.parseInt(item.getAttribute("num"));
		coefs[band] = new Vector();
		print("band: "+band);

		NodeList subbands = item.getElementsByTagName("subband");
		int ns = subbands.getLength(); // al menos tiene que haber una
		for (int j=0;j<numsubbands;j++) coefs[band].add(null);

		int t=1, width=1, height=1;
		float scale=1f;
		for (int j=0;j<ns;j++) {
		    Element subitem = (Element)subbands.item(j);

		    int subband = Integer.parseInt(subitem.getAttribute("num"));

		    t = ParamLattice.INTEGER;
		    if (subitem.getAttribute("type").equals("dual"))
			t = ParamLattice.DUAL;
		    width = Integer.parseInt(subitem.getAttribute("width"));
		    height = Integer.parseInt(subitem.getAttribute("height"));
		    scale = Float.parseFloat(subitem.getAttribute("scale"));

		    ParamLattice pml = new ParamLattice(t, width, height, scale);

		    coefs[band].set(subband, pml);
		}
		
		// habra que inicializar de alguna manera los que quedan a (null)
		// los podemos inicializar todos igual que el ultimo "subband"
		// El DTD ya nos dice, ademas, que como minimo hay un "subband"
		for (int j=0;j<numsubbands;j++) {
		    if (coefs[band].get(j) == null) {
			coefs[band].set(j,new ParamLattice(t,width,height,scale));
		    }
		}
		
		for (int j=0;j<numsubbands;j++) 
		    print("subband "+j+": "+(ParamLattice)coefs[band].get(j));

	    }


    }


    /** Return an array per band, each being a Vector containing
      * ParamLattice objects, the configuration of a Quantizer
      */
    public Vector[] getConfig() {
	return coefs;
    }

    /** Number of levels in Wavelet Transform.
      * This will determine number of subbands per band
      */
    public int getLevel() {
	return level;
    }

    protected void print(String s) {
	if (debug) System.out.println(s);
    }

//    public static void main(String s[]) {
//	QuantizerConfig app = new QuantizerConfig(s[0],true);
//    }
}

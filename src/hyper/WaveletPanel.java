package hyper;

import java.awt.*;
import java.awt.image.*;
import java.awt.image.renderable.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.media.jai.*;
import javax.media.jai.iterator.*;
import java.io.*;
import java.util.StringTokenizer;
import com.sun.media.jai.codec.*;


/**
  * This class extends a <b>JPanel</b> and is comprised of an area where the image
  * is showed and another to navigate it.
  * @see hyper.ImageDisplay
  * @see hyper.Panner
  * @author David Gavilan
  */
public class WaveletPanel extends JPanel {

//  public static final int NONE=0,FULL=1,SCALED=2;
  
//  protected BufferedImage bufImg;
//  protected int imgType;
//  protected int how2view;
  protected int tileWidth=512,tileHeight=512;
  protected ImageDisplay canvas;
  protected Panner panner;
    
  transient PlanarImage pimg; // la dejamos publica para poderla alterar desde fuera


  /**
   * El constructor define un Panel con una imagen dentro (JLabel)
   */
  public WaveletPanel(String s) {
          
     //setBackground(Color.white);
//     how2view=NONE;
//     imgType = BufferedImage.TYPE_INT_RGB;
     
     canvas = new ImageDisplay();
     try {
       loadImage(s);
     } catch (Exception e) {System.out.println("WaveletPanel: "+e);}
     
     setLayout(new BorderLayout());
     add(canvas,BorderLayout.CENTER);
     
     // Panel de control
     JPanel controlPanel = new JPanel();
     controlPanel.setLayout(new BorderLayout());
     controlPanel.setOpaque(true);
     
     // build the panner controller
     panner = new Panner(canvas, pimg, 128);

     panner.setBorder(new CompoundBorder(
                      new EtchedBorder(3),
                      new LineBorder(Color.gray, 3)
                      ) );     
     controlPanel.add(panner,BorderLayout.CENTER);
//     controlPanel.add(panner.getOdometer(),BorderLayout.SOUTH);
     
     add(controlPanel,BorderLayout.WEST);

  }
  
    
  /**
    * Mira el tipo de model para decirnos el numero de canales que la forman
    */
  public int getNChannels() {
    return pimg.getSampleModel().getNumBands();  
  }

    public int getDataType() {
	return pimg.getSampleModel().getDataType();
    }

  /**
   * Podemos optar por ver la imagen entera, verla reducida o no verla
   * Si la vemos entera, ocuparemos más memoria.
   */
  public void updateView() {
    if (pimg == null) return;
    if (canvas!=null) canvas.set(pimg);
    if (panner!=null) {
		panner.set(canvas,canvas.getImage(),128);
		panner.revalidate();
    }
    repaint();    
  }
        
  /**
    * Load a PNM (PGM/PPM), GIF, JPEG or TIFF file
    */
  public void loadImage(String s) throws FileNotFoundException {
    if (!(new File(s)).exists()) throw (new FileNotFoundException(s+" (No such file)"));
    pimg = load(s);
    updateView();
  }

  /**
   * Loads an image, reformats it to float and tiles it
   */
  public PlanarImage load(String s) {
    return hyper.dsp.COps.reformat(JAI.create("fileload",s),
           new Dimension(tileWidth,tileHeight),
           DataBuffer.TYPE_FLOAT).createInstance();
  }

  /** Retiles current image */
  public void setTiles(int width, int height) {
     tileWidth=width;
     tileHeight=height;
     pimg = hyper.dsp.COps.reformat(pimg,
           new Dimension(tileWidth,tileHeight),
           DataBuffer.TYPE_FLOAT).createInstance(); 
  }
  
  /**
    * guarda la imagen (bufImg) en JPEG a disco
    */
  public void saveAsJPG(String file) throws java.io.IOException {
    hyper.dsp.COps.saveAsJPG(pimg,file);
  }

  /**
    * guarda la imagen (bufImg) en TIFF a disco
    */
  public void saveAsTIFF(String file) throws java.io.IOException {
    hyper.dsp.COps.saveAsTIFF(pimg,file);  	
  }
  
    /**
    * guarda la imagen (bufImg) en BMP a disco
    */
  public void saveAsBMP(String file) throws java.io.IOException {
    hyper.dsp.COps.saveAsBMP(pimg,file);
  }
  
   /**
    * guarda la imagen (bufImg) en PPM a disco
    */
  public void saveAsPNM(String file) throws IOException {
    //writeBufferedImage(new FileOutputStream(file));
    hyper.dsp.COps.saveAsPNM(pimg,file);    
  }
  
  /**
    * Image Width
    */
  public int imageXSize() {
    // suponemos imagen cuadrada, width=height
    return (pimg.getWidth());
  }
  /**
    * Image Height
    */
  public int imageYSize() {
    return (pimg.getHeight());
  }
  
    public int getTileWidth() {
	return tileWidth;
    }
    
    public int getTileHeight() {
	return tileHeight;
    }

    /**
      * Whether or not all the tiles in which the image is divided are squared
      * @return true if the images is composed of square tiles.
      */
    public boolean isSquare() {
	int w=imageXSize(), h=imageYSize();	
	if (tileWidth!=tileHeight) return false;
	if (w % tileWidth != 0) return false;
	if (h % tileHeight != 0) return false;
	return true;
    }
    
    public void setVisualizationMode(int mode) {
    	canvas.setVisualizationMode(mode);
    }
}

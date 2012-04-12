package hyper;

import java.awt.*;
import java.io.File;
import java.net.URL;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.event.HyperlinkEvent.*;
import javax.swing.text.html.*;

/**
  * Used to browse <b>HTML</b> documentation.
  * @version 1.0 2002/1/11
  * @author David Gavilan
  */
public class JBrowserDialog extends JDialog implements HyperlinkListener {

    /** To open a big dialog window */
    public static final int BIG=1;
    /** To open an about-like dialog window */
    public static final int TINY=2;

    /**
      * Initializes the dialog with the desired size and an initial
      * URL page
      */
    public JBrowserDialog(int dimType, URL url) {
	setDimType(dimType);
	try {
	  JEditorPane jp = new JEditorPane(url);
	  jp.setEditable(false);
	  jp.addHyperlinkListener(this);
	  JScrollPane jsp = new JScrollPane(jp);
	  getContentPane().add(jsp);
	} catch(Exception e) {
	  System.out.println("JBrowserDialog: "+e);
	}
    }

    /**
      * Changes window dimensions
      * @param dimType which size would you prefer
      */
    public void setDimType(int dimType) {
      Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
      int w,h;
      switch(dimType){
      case BIG:
	  w=dim.width/2;
	  h=(dim.height*3)/4;
	  break;
      case TINY:
	  w=dim.width/5;
	  h=dim.height/5;
	  break;
      default:
	  w=10; h=10;
      }

      setSize(w,h);
    }

    /**
      * This method is invoked whenever an hyperlink is clicked.
      * Implementation of <code>HyperlinkListener</code>.
      */
    public void hyperlinkUpdate(HyperlinkEvent e) {
	if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
	    JEditorPane pane = (JEditorPane) e.getSource();	   
 	    if (e instanceof HTMLFrameHyperlinkEvent) {
		HTMLFrameHyperlinkEvent  evt = (HTMLFrameHyperlinkEvent)e;
 		HTMLDocument doc = (HTMLDocument)pane.getDocument();
 		doc.processHTMLFrameHyperlinkEvent(evt);	   
 	    } else {
 		try {
		    pane.setPage(e.getURL());
 		} catch (Throwable t) {
 		    t.printStackTrace();
 		}
 	    }
 	}
    }


}

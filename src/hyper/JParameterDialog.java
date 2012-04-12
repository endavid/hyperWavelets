package hyper;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import hyper.dsp.*;

/**
 * A parameter setting dialog.
 * DWT decomposition level, Sub-band uniform quantization table, and uniform
 * quantization ratio can be set interactively in the dialog.
 */
public class JParameterDialog extends JDialog {

  JPanel cards;
  JComboBox levelChoice;
//  JTextField quantSheet[][] = new JTextField[8][8];
  JTextField latticeWidth, latticeHeight;
  JTextField uniratioText;

  JButton okBtn;

  final static String DWTLEVELPANEL = "DWT Levels";
  final static String QUANTSHEETPANEL = "Quantization Parameters";
  final static String UNIRATIOPANEL = "Uniform Qn Ratio";

  //SBUniformQuantizer quantizer = new SBUniformQuantizer(3);


  /**
   * Create a modal JParamterDialog.
   */
  public JParameterDialog(Frame parent, String title) {
    super(parent, title, true);

    Container contentPane = getContentPane();
    
    cards = new JPanel();
    cards.setLayout(new CardLayout());

    JPanel cp = new JPanel();
    String[] ss = {DWTLEVELPANEL,QUANTSHEETPANEL,UNIRATIOPANEL};
    JComboBox c = new JComboBox(ss);
    c.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
	    JComboBox cb = (JComboBox)e.getSource();
	    String arg = (String)cb.getSelectedItem();
	    ((CardLayout)cards.getLayout()).show(cards,arg);
	}
    });
    
    cp.add(c);
    contentPane.add("North", cp);

    JPanel dwtLevelP = new JPanel();
    dwtLevelP.setLayout(new FlowLayout());

    JLabel msg;

    msg = new JLabel("Set level to:");
    //msg.setAlignment(JLabel.CENTER);
    dwtLevelP.add(msg);

    String[] sn = {"1","2","3","4","5","6"};
    levelChoice = new JComboBox(sn);
    levelChoice.setSelectedIndex(2);
    dwtLevelP.add(levelChoice);

    cards.add(DWTLEVELPANEL, dwtLevelP);

    JPanel quantSheetP = new JPanel();
    quantSheetP.setLayout(new FlowLayout());
    
/*
    for (int i=0; i<8; i++)
      for (int j=0; j<8; j++) {
	quantSheet[i][j] =
	  new JTextField("4", 2);	
	  //new JTextField(String.valueOf(quantizer.quantTable[i][j]), 2);
	quantSheetP.add(quantSheet[i][j]);
      }
*/  
    latticeWidth=new JTextField("4",2);
    latticeHeight=new JTextField("4",2);
    quantSheetP.add(new JLabel("Lattice Parameters"));
    quantSheetP.add(latticeWidth);
    quantSheetP.add(latticeHeight);
    cards.add(QUANTSHEETPANEL, quantSheetP);

    JPanel uniratioP = new JPanel();
    dwtLevelP.setLayout(new FlowLayout());

    msg = new JLabel("Set Uniform Quantization Ratio to:");
    //msg.setAlignment(JLabel.CENTER);
    uniratioP.add(msg);

    uniratioText = new JTextField("8", 2);
    uniratioP.add(uniratioText);

    cards.add(UNIRATIOPANEL, uniratioP);

    contentPane.add("Center", cards);

    okBtn = new JButton("Ok");
    okBtn.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
	    JButton b = (JButton)e.getSource();
	    String arg = (String)b.getText();

	    if (arg.toString().equals("Ok")) {
	      // actions before closing [...]
              hide(); }   
	}
    });
    contentPane.add("South", okBtn);
  }
  

  

  /**
   * Returns the number of DWT decomposition level.
   */
  public int getLevel() {
    return levelChoice.getSelectedIndex()+1;
  }
  
    /**
      * Sets the number of DWT decomposition level.
      */
    public void setLevel(int n) {
	levelChoice.setSelectedIndex(n-1);
    }


  /**
   * Returns the uniform quantization ratio.
   */
  public int getUniRatio() {
    return Integer.valueOf(uniratioText.getText()).intValue();
  }

    /**
      * Returns default lattice width
      */
    public int getLatticeWidth() {
	return Integer.valueOf(latticeWidth.getText()).intValue();
    }

    /**
      * Returns default lattice height
      */
    public int getLatticeHeight() {
	return Integer.valueOf(latticeHeight.getText()).intValue();
    }

}

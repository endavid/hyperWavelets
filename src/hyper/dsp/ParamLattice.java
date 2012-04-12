package hyper.dsp;


/**
 * This class contains the basic definition of a lattice.
 * <P>It also contain methods to operate images using lattices (<code>apply</code>)
 *
 *
 * @author David Gavilan
 * @version 1.1
 */
public class ParamLattice {
    /** Integer Lattice */
    public static final int INTEGER=1;
    /** Dual Lattice */
    public static final int DUAL=2;

    /** Lattice type: INTEGER, DUAL, ... */
    private int type;
    /** Lattice dimensions */
    private int width, height;
    /** A factor by which this lattice gets multiplied whenever used */
    private float scaleFactor;

    /** Default constructor creates an Integer lattice 1x1 and no scale factor,
      * that is, scale factor = 1
      */
    public ParamLattice() {
	this(INTEGER,1,1,1f);
    }

    /** Constructs a lattice of the desired type and size (width and height) */      
    public ParamLattice(int type, int w, int h) {
	this(type, w, h, 1f);
    }

    /** Constructs a lattice of the desired type, size and scale factor */
    public ParamLattice(int type, int w, int h, float s) {
	this.type = type;
	width=w; height=h;
	scaleFactor=s;
    }

    /** Applies quantization function to a vector */
    public float[] apply(float v[]) {
		float[] res=new float[v.length];
		float half = scaleFactor / 2f;

		switch (type) {
		case INTEGER:
		    for (int i=0;i<v.length;i++) {
			res[i]=Math.round((v[i]+half)/scaleFactor);
		    }
		    break;
		default:
		    // don't do anything, just cast
		    for (int i=0;i<v.length;i++) {
				res[i]=(short)v[i];
			}
		}
		return res;
    }

    /** Dequantizes a vector */
    public float[] iapply(float v[]) {
		float[] res=new float[v.length];
	
		switch (type) {
		case INTEGER:
	 	   for (int i=0;i<v.length;i++) {
				res[i]=(v[i]*scaleFactor);
	  	  }
	  	  break;
		default:
	 	   // don't do anything
	 	   for (int i=0;i<v.length;i++) {
				res[i]=v[i];
		    }
		}
		return res;
    }

    public int getType() { return type; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
//    public int getLenght() { return length; }
	public int getSize() { return width*height; }
    public float getScale() { return scaleFactor; }

    public void setScale(float f) { scaleFactor=f; }

    // Some properties
    /** Whether or not this lattice is just as a uniform quantizer
      * @return true if lattice size is 1 
      */
    public boolean isBasic() {
	return (type==INTEGER && width==1 && height==1);
    }

    public String getTypeString(int type) {
	switch(type) {
	case INTEGER:
	    return new String("INTEGER");	  
	case DUAL:
	    return new String("DUAL");	    
	}
	return new String("");
    }

    /** We redefine toString method to print this object */
    public String toString() {
	return new String("Lattice("+width+"x"+height+":"+scaleFactor+", "+
			  getTypeString(type)+", squared)");
    }
}

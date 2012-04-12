package hyper.coding;

import java.util.Vector;
import java.util.BitSet;
import java.io.*;
import hyper.io.*;

/** Decodes an Index Coded quad stream. <p>
  * @see hyper.coding.IndexCoding
  * @see hyper.io.QuadInputStream
  * @author David Gavilan, Cristina Fernandez, Joan Serra
  * @version 1.0 12/03/2002  
  */
public class IndexDecoding{

	/** Threshold */
	protected int T;
	protected QuadInputStream qis;
	protected Vector IS;
	protected int[] result;
	protected int sum=0;
	protected int size; // en este caso, = result.length
	protected BitSet bitSet;	
	
	public IndexDecoding() {}
	
	public IndexDecoding(int size, QuadInputStream qis) throws IOException{	
		result = new int[size];
		this.size=size;
		IS = new Vector(size);
		bitSet = new BitSet(size);
		this.qis = qis;
		T = (int)qis.readUBits(8);
	}
	

	public void decode() throws IOException {
		int tt = T;
		sum = 0;
		while(tt>0){
			System.out.println("IndexDecoding: tt = "+tt);
			int n = obtainIndex(tt);
//			System.out.println("obtained");
			refinement(tt,sum);
//			System.out.println("refined");			
			sum += n;
			tt>>=1;
		}		
	}

	/** 
	 *@return el numero d'elements afegits a IS*/
	protected int obtainIndex(int tt) throws IOException{		
	    Vector is = new Vector(); 
		int[] indexs = {0,0};

		int n = (int)qis.readUBits(32); /* llegim el nombre de quads d'aquest pas */ 
		int ant = 0;
		for(int i=0;i<n;i++){
			int idx = 1;
			int q = qis.readQuad();
			while ((q != QuadOutputStream.MINUS) 
			       && (q != QuadOutputStream.PLUS)){
				idx<<=1;
				idx|=q;
				q=qis.readQuad();
			}
			int pos = idx + ant;
			ant = pos;
//			System.out.println("idx="+idx+", adding "+pos+ " to \"is\". Added: ");
			pos = realIndex(pos,indexs); // segun como programemos esta funcion, se hace todo lentisimo
			is.add(new Integer(pos));
			
			setValue(pos-1,q,tt);
		}
		//IndexCoding.showIntVector(is);
		//realIndex(is);
		//setValue(is,sig,tt);
		//IndexCoding.showIntVector(is);		
		IS.addAll(is);
		updateBitSet(is);
		//System.out.println("IS ");
		//IndexCoding.showIntVector(IS);

		return n; 
	}
	


	protected void refinement(int tt, int size) throws IOException {
		for(int i=0;i<size;i++){
			int idx = ((Integer)IS.get(i)).intValue()-1; /*Posicion en el vector result*/
			int val = getValue(idx);
			int sign = (val<0)?-1:1;
			val = Math.abs(val);	
			int q = qis.readQuad();
			if (q == QuadOutputStream.ONE) val+=tt;
			setValue(idx,val*sign);			
		}
	}
	
	protected void setValue(int pos, int sign, int tt){
		int s = (sign==QuadOutputStream.PLUS)?1:-1;
		result[pos]= s*tt;
	}
	

	protected void setValue(Vector is, Vector sig, int tt){
		for (int i=0;i<is.size();i++)
			result[i]=(((Integer)is.get(i)).intValue())*
         			(((Integer)sig.get(i)).intValue())*tt;
	}
	
	protected void setValue(int pos,int val){
		result[pos] = val;
	}

	public int getValue(int pos){
		return result[pos];
	}

/*	
	protected int realIndex(int idx){
	 	int i;
	 	for (i=0;i<size;i++) {
	 		if (!bitSet.get(i)) idx--;
	 		if (idx==0) return (i+1);
	 	}
	 	return (i+1);
	}
*/

	/** Finds out which is the real index in the resulting vector of read local index.
	  * @param elem the relative index
	  * @param idx a vector to remember last visited indexes/holes
	  */
	protected int realIndex(int elem,int[] idx){
	 	int i=0;
	 	for (i=idx[0];i<size;i++) {
	 		if (!bitSet.get(i)) idx[1]++;
	 		if (idx[1]==elem) {
	 			idx[0]=i+1;
	 			return (i+1);
	 		}
	 	}
	 	return (i+1);
	}

/*	
	protected int realIndex(Vector is){
	 	int i;
	 	int idx = 0;
	 	int pos = 0;
 		if (pos>=is.size()) return 1;		 			
	 	int elem = ((Integer)is.get(pos)).intValue();
	 	for (i=0;i<size;i++) {
	 		if (!bitSet.get(i)){
	 			idx++;
		 		if (idx==elem){
		 		   is.set(pos,new Integer(i+1));
  	  			pos++;
					if (pos>=is.size()) return 1;		 			
					elem = ((Integer)is.get(pos)).intValue();
				}

	 		}
	 	}
	 	return 0;
	}
*/
	protected void updateBitSet(Vector is) {
		for (int i=0;i<is.size();i++) {
			int pos = ((Integer)is.get(i)).intValue();
			bitSet.set(pos-1);
		}
	}
	
}
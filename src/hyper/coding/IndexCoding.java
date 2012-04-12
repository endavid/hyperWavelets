package hyper.coding;

import java.util.Vector;
import java.io.*;
import hyper.io.*;

/** An implementation of Index Coding algorithm. <p>
  * Using a <code>QuadOutputStream</code>, we output the initial threshold,
  * the reduced set of indices, and the refinement values of other stages.
  * @see hyper.io.QuadOutputStream
  * @author David Gavilan, Cristina Fernandez, Joan Serra
  * @version 1.0 12/03/2002
  */ 
public class IndexCoding{

	/** Set of significant coefficients*/
	protected Vector SCS;
	/** Temporary set containing significant coefficients found in a given round */
	protected Vector TPS;
	/** Set of Insignificant coefficients */
	protected Vector ICS;
	/** The indices (in ICS) of significant coefficients */
	protected Vector S;
	/** Threshold */
	protected int T;
	/** The coded stream, resulting of 4 different simbols: {0,1,+,-} */
	protected QuadOutputStream qos;
	
	public IndexCoding() {}

	/** Creates an instance of the algorithm, and outputs the initial threshold to the stream.
	  * @param initialSet the values we are going to code
	  * @param qos the output stream	  
	  */	
	public IndexCoding(int[] initialSet, QuadOutputStream qos) throws IOException{
		ICS = new Vector(initialSet.length);
		SCS = new Vector(initialSet.length);
		TPS = new Vector(initialSet.length/2);
		S = new Vector(initialSet.length/2);		

		this.qos = qos;
		
		for(int i=0;i<initialSet.length;i++){
			ICS.add(new Integer(initialSet[i]));
		}
		
		T = findThreshold();
		
		this.qos.writeUBits(T,8);
	}
	
	/** Finds an appropiate initial threshold. By default, 128 (gray).
	  */	
	public int findThreshold() {
		return 128;
	}
	
	/** Returns found initial threshold */
	public int getThreshold() { return T; }
	
	/** Applies the Index Coding algorithm, doing output to the <code>QuadOutputStream</code>.
	 */
	public void code() throws IOException{
		int tt = T;
		
		while(tt>0){
			System.out.println("IndexCoding: tt = "+tt);
			sort(tt);
			if (S.size()>0) reduce();
			refinement(tt);
			updateSets();
			tt>>=1;
		}
	}
	
	/** Sorting stage. It outputs the number of elements over current threshold
	  * that are left in ICS yet.<p>
	  * index(i) in S belongs to natural numbers (>0).
	  * @param tt current threshold
	  */
	protected void sort(int tt) throws IOException{		
		for(int i=0;i<ICS.size();i++){
			if (Math.abs(((Integer)ICS.get(i)).intValue())>= tt){
				TPS.add(ICS.get(i));
				S.add( new Integer(i+1));
			}
		}
		qos.writeUBits(S.size(),32); // numero de elementos q enviamos
	}

	/** Reduction stage. It outputs the reduced set of S.
	  */
	protected void reduce() throws IOException{
		int idx = ((Integer)S.get(0)).intValue();
		int val = (((Integer)TPS.get(0)).intValue()>=0)?
			QuadOutputStream.PLUS:QuadOutputStream.MINUS;
		qos.writeUQuads(idx,QuadOutputStream.minBits(idx)-1);
		qos.writeQuad(val);
		for(int i=1;i<S.size();i++){
			idx = (((Integer)S.get(i)).intValue())-(((Integer)S.get(i-1)).intValue());
			val = (((Integer)TPS.get(i)).intValue()>=0)?
   			QuadOutputStream.PLUS:QuadOutputStream.MINUS;
			qos.writeUQuads(idx,QuadOutputStream.minBits(idx)-1);
			qos.writeQuad(val);
			
		}
	}
	
	/** Refinement stage. Outputs finer detail to which interval belong already sent indices.
	  */
	protected void refinement(int tt) throws IOException{
		//showIntVector(SCS);
		for(int i=0;i<SCS.size();i++){
			int val = Math.abs(((Integer)SCS.get(i)).intValue());
			int high = 2*T;
			int low = 0;
			int range = 2*T;
			int pp = QuadOutputStream.ZERO; /*precission point*/
			
			while(range>tt){
				if(val>=(low + (range>>1))){
					low = low + (range>>1);
					pp = QuadOutputStream.ONE;
				}
				else{
					high = high - (range>>1);
					pp = QuadOutputStream.ZERO;
				}
				range = high - low;
			}
			if (tt<T) qos.writeQuad(pp);
		}
	}
	
	/** Update all sets each step of the coding process */
	protected void updateSets(){
		for(int i=S.size()-1;i>=0;i--){
//			showIntVector(S);
//			showIntVector(ICS);
			ICS.remove(((Integer) S.get(i)).intValue()-1);
		}
		SCS.addAll(TPS);
		TPS.removeAllElements();
		S.removeAllElements();
	}
	
	/** To show in the console a <code>Vector</code> of <code>Integer</code> values */
	public static void showIntVector(Vector v) {
		String s="[ ";
		for (int i=0;i<v.size();i++) {
			s+=(Integer)v.get(i) + " ";
		}
		s+="]";
		System.out.println(s);
    }

}
package ch.epfl.gameboj.bits;

import java.util.Arrays;
import java.util.Objects;

import ch.epfl.gameboj.Preconditions;

/**
 * 
 * @author Christophe Saad (282557)
 * @author David Cian (287967)
 *
 */
public final class BitVector {
	//must be immutable
	
	private final int[] bitVector;
	
	private static int[] bitVector(int size, boolean initialValue) {
	    Preconditions.checkArgument(size%32 == 0 && size >= 0);
	    int[] bitVector = new int[size];
	    Arrays.fill(bitVector, initialValue ? 1 : 0);
	    return bitVector;
	}
	
	
	private BitVector extract(int start, int size, boolean methodZeroExtended) {
	    int[] extractedBits = new int[size];
	    
	    return null;
	}
	
	
	//Pour extraire chaque int puis les coller dans extract
	//Tu prends la premiere partie du 1er puis la 2eme du 2eme et tu les "or" ensemble
	private int intExtract(int start, int size, boolean methodZeroExtended) {
	   
	   if(!methodZeroExtended) {
	       int startIntIndex = (Math.floorMod(start, 32*size())) / 32;
	       int startPosition = Math.floorMod(start, 32);              
	       return (bitVector[startIntIndex] >>> startPosition) | (bitVector[startIntIndex + 1] << Integer.SIZE - startPosition);  
	   }
	   else {
	       
	   }
	}
	
//	private boolean testBitExtract(int index, boolean methodZeroExtended) {
//        
//	    if(index >= 0 && index < size()) return testBit(index);
//	    else {
//	        if(methodZeroExtended) return false;
//	        else {
//	            return testBit(index % size());
//	        }
//	    }
//    }
	
	//Constructeur privé
	private BitVector(int[] bitVector) {
	    this.bitVector = bitVector;
	}
	
	public BitVector(int size, boolean initialValue) {
		this(bitVector(size, initialValue));
	}
	
	public BitVector(int size) {
		this(size, false);
	}
	
	public int[] getArray() {
		return Arrays.copyOf(this.bitVector, this.bitVector.length);
	}
	
	public int size() {
		return bitVector.length * Integer.SIZE;
	}
	
	public boolean testBit(int index) {
	    //CHECK INDEX??
	    Preconditions.checkArgument(index >= 0 && index < size());
		return Bits.test(Objects.checkIndex(index, bitVector.length) / Integer.SIZE, index % Integer.SIZE);
	}
	
	public BitVector not() {
		int[] notVector = new int[bitVector.length];
		
		for(int i = 0; i < notVector.length; i++) {
			notVector[i] = ~bitVector[i]; 
		}
		
		return new BitVector(notVector);
	}
	
	public BitVector and(BitVector otherVector) {
		Preconditions.checkArgument(bitVector.length == Objects.requireNonNull(otherVector, "The provided vector must not be null").size(),
				"Two bit vectors of different sizes cannot be compared.");
		
		int[] andVector = new int[bitVector.length];
		
		for(int i = 0; i < andVector.length; i++) {
			andVector[i] = bitVector[i] & otherVector.getArray()[i];
		}
		
		return new BitVector(andVector);
	}
	
	public BitVector or(BitVector otherVector) {
		Preconditions.checkArgument(bitVector.length == Objects.requireNonNull(otherVector, "The provided vector must not be null").size(), 
				"Two bit vectors of different sizes cannot be compared.");
		
		int[] orVector = new int[bitVector.length];
		
		for(int i = 0; i < orVector.length; i++) {
			orVector[i] = bitVector[i] | otherVector.getArray()[i];
		}
		
		return new BitVector(orVector);
	}
	
	public BitVector extractZeroExtended(int start, int size) { 
		return extract(start, size, true);
	}
	
	public BitVector extractWrapped(int start, int size) {
	    return extract(start, size, false);
	}
	
	public BitVector shift(int distance) {
		return null;
	}
	
	@Override
	public boolean equals(Object o) {
		return (o instanceof BitVector) && Arrays.equals(bitVector, ((BitVector)o).getArray());
	}
	
	@Override
	public int hashCode() {
		return bitVector.hashCode();
	}
	
	@Override
	public String toString() {
		return null;
	}
}

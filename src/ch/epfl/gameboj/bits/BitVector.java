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
	
	//DON'T FORGET TO LINK COMMITS TO ISSUES WITH Ref. #xxx where xxx is the issue number 
	//Use Feature-driven development (FDD) and branch then create merge request for new functionality
	
	private final int[] bitVector;
	
	private static int[] bitVector(int size, boolean initialValue) {
	    Preconditions.checkArgument(size%32 == 0 && size >= 0);
	    int[] bitVector = new int[size];
	    Arrays.fill(bitVector, initialValue ? 1 : 0);
	    return bitVector;
	}
	
	
	private BitVector extract(int start, int size, boolean methodZeroExtended) {
	    int[] extractedInts = new int[size];
	    for (int i = 0; i < size; i++) {
	        extractedInts[i] = intExtract(start + 32*i, methodZeroExtended);
	    }
	    return new BitVector(extractedInts);
	}
	
	
	private int intExtract(int start, boolean methodZeroExtended) {
	   
	   if(Math.floorMod(start, 32) == 0) {
	       if(!methodZeroExtended) return bitVector[Math.floorDiv((Math.floorMod(start, size())), 32)];
	       else return (start > size() | start < 0) ? 0 : bitVector[Math.floorDiv(start, 32)];
	   }
	   
	   if(!methodZeroExtended) {
	       int startIntIndex = Math.floorDiv((Math.floorMod(start, size())), 32);
	       int startPosition = Math.floorMod(start, 32);              
	       return (bitVector[startIntIndex] >>> startPosition) | (bitVector[(startIntIndex + 1) % bitVector.length] << Integer.SIZE - startPosition);  
	   }
	   else if(methodZeroExtended) {
	       if(start > size() | start < -Integer.SIZE) return 0;
	       else {
	           int startIntIndex = Math.floorDiv(start, 32);
	           if(startIntIndex == bitVector.length - 1) return bitVector[startIntIndex] >>> start | 0;
	           if(startIntIndex ==0) return bitVector[startIntIndex] << start | 0;
	           else return bitVector[startIntIndex] >>> start | bitVector[(startIntIndex + 1)] << Integer.SIZE - start;
	       }
	   }
	   
	   return 0;
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
		return extractZeroExtended(distance, size());
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
		return bitVector.toString();
	}
}

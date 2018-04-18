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
	    Preconditions.checkArgument(size%Integer.SIZE == 0 && size > 0);
	    int[] bitVector = new int[size/Integer.SIZE];
	    Arrays.fill(bitVector, initialValue ? -1 : 0);
	    return bitVector;
	}
	
	
	private BitVector extract(int start, int size, boolean methodZeroExtended) {
	    Preconditions.checkArgument(size%Integer.SIZE == 0 && size > 0);
	    int[] extractedInts = new int[size/Integer.SIZE];
	    for (int i = 0; i < size/Integer.SIZE; i++) {
	        extractedInts[i] = intExtract(start + Integer.SIZE*i, methodZeroExtended);
	    }
	    return new BitVector(extractedInts);
	}
	
	
	private int intExtract(int start, boolean methodZeroExtended) {
	   
	   if(Math.floorMod(start, Integer.SIZE) == 0) {
	       if(!methodZeroExtended) return bitVector[Math.floorDiv((Math.floorMod(start, size())), Integer.SIZE)];
	       else return (start > size() | start < 0) ? 0 : bitVector[Math.floorDiv(start, Integer.SIZE)];
	   }
	   
	   if(!methodZeroExtended) {
	       int startIntIndex = Math.floorDiv((Math.floorMod(start, size())), Integer.SIZE);
	       int startPosition = Math.floorMod(start, Integer.SIZE);              
	       return (bitVector[startIntIndex] >>> startPosition) | (bitVector[(startIntIndex + 1) % bitVector.length] << (Integer.SIZE - startPosition));  
	   }
	   else if(methodZeroExtended) {
	       int startIntIndex = Math.floorDiv(start, Integer.SIZE);
	       if(startIntIndex >= bitVector.length || startIntIndex < -1) return 0;
	       else {
	           int startPosition = Math.floorMod(start, Integer.SIZE);
	           if(startIntIndex == bitVector.length - 1) return bitVector[startIntIndex] >>> startPosition;
	           if(startIntIndex == -1) return (bitVector[startIntIndex + 1] << (Integer.SIZE - startPosition));
	           else return (bitVector[startIntIndex] >>> startPosition) | (bitVector[(startIntIndex + 1)] << (Integer.SIZE - startPosition));
	       }
	   }
	   
	   return 0;
	}
	
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
		return Bits.test(bitVector[Math.floorDiv(Objects.checkIndex(index, size()), Integer.SIZE)], index % Integer.SIZE);
	}
	
	public BitVector not() {
		int[] notVector = new int[bitVector.length];
		
		for(int i = 0; i < notVector.length; i++) {
			notVector[i] = ~bitVector[i]; 
		}
		
		return new BitVector(notVector);
	}
	
	public BitVector and(BitVector otherVector) {
		Preconditions.checkArgument(size() == Objects.requireNonNull(otherVector, "The provided vector must not be null").size(),
				"Two bit vectors of different sizes cannot be compared.");
		
		int[] andVector = new int[bitVector.length];
		
		for(int i = 0; i < andVector.length; i++) {
			andVector[i] = bitVector[i] & otherVector.getArray()[i];
		}
		
		return new BitVector(andVector);
	}
	
	public BitVector or(BitVector otherVector) {
		Preconditions.checkArgument(size() == Objects.requireNonNull(otherVector, "The provided vector must not be null").size(), 
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
		return extractZeroExtended(-distance, size());
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
		StringBuilder b = new StringBuilder();
		for(int i = size() - 1; i >= 0; --i) {
		    b.append(testBit(i) ? 1 : 0);
		}
		return b.toString();
	}
	
	
	public final static class Builder {
	    
	    private int[] vector;
	    
	    public Builder(int size) {
	        vector = bitVector(size, false);
	    }
	    
	    public Builder setByte(int index, int b) {
	        if(vector == null) throw new IllegalStateException();
	        Preconditions.checkBits8(b);
	        if(!(index >= 0 && index < vector.length*Integer.SIZE/Byte.SIZE)) throw new IndexOutOfBoundsException();
	        vector[Math.floorDiv(Byte.SIZE*index , Integer.SIZE)] |= b << Math.floorMod(Byte.SIZE*index, Integer.SIZE);
	        return this;
	    }
	    public BitVector build() {
	        if(vector == null) throw new IllegalStateException();
	        BitVector builded = new BitVector(vector);
	        vector = null;
	        return builded;
	    }
	}
}

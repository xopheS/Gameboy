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
	
	public BitVector(int size, boolean initialValue) {
		Preconditions.checkArgument(size%32 == 0 && size >= 0);
		bitVector = new int[size];
		Arrays.fill(bitVector, initialValue ? 1 : 0);
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
		return Bits.test(Objects.checkIndex(index, bitVector.length) / Integer.SIZE, index % Integer.SIZE);
	}
	
	public int[] not() {
		int[] notVector = new int[bitVector.length];
		
		for(int i = 0; i < notVector.length; i++) {
			notVector[i] = ~bitVector[i]; 
		}
		
		return notVector;
	}
	
	public int[] and(BitVector otherVector) {
		Preconditions.checkArgument(bitVector.length == Objects.requireNonNull(otherVector, "The provided vector must not be null").size(),
				"Two bit vectors of different sizes cannot be compared.");
		
		int[] andVector = new int[bitVector.length];
		
		for(int i = 0; i < andVector.length; i++) {
			andVector[i] = bitVector[i] & otherVector.getArray()[i];
		}
		
		return andVector;
	}
	
	public int[] or(BitVector otherVector) {
		Preconditions.checkArgument(bitVector.length == Objects.requireNonNull(otherVector, "The provided vector must not be null").size(), 
				"Two bit vectors of different sizes cannot be compared.");
		
		int[] orVector = new int[bitVector.length];
		
		for(int i = 0; i < orVector.length; i++) {
			orVector[i] = bitVector[i] | otherVector.getArray()[i];
		}
		
		return orVector;
	}
	
	public int[] extractZeroExtended(int start, int size) {
		return null;
	}
	
	public int[] extractWrapped(int start, int size) {
		return null;
	}
	
	public int[] shift(int distance) {
		return null;
	}
	
	@Override
	public boolean equals(Object o) {
		return false;
	}
	
	@Override
	public int hashCode() {
		return 0;
	}
	
	@Override
	public String toString() {
		return null;
	}
}

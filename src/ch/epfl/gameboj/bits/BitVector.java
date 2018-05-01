package ch.epfl.gameboj.bits;

import java.util.Arrays;
import java.util.Objects;

import ch.epfl.gameboj.Preconditions;

/**
 * Cette classe simule un vecteur de bits plus long que 64.
 *
 * @author Christophe Saad (282557)
 * @author David Cian (287967)
 *
 */

public final class BitVector {

    private final int[] bitVector;

    // TEST FUNCTION////////////////////////////////////////////
    public static BitVector rand() {
        int[] val = { -1, -8, 2839, 7 };
        return new BitVector(val);
    }

    public BitVector(BitVector b) {
        this(b.bitVector);
    }
    //////////////////////////////////////////////////////////

    private static int[] bitVector(int size, boolean initialValue) {
        Preconditions.checkArgument(size % Integer.SIZE == 0 && size > 0);
        int[] bitVector = new int[size / Integer.SIZE];
        Arrays.fill(bitVector, initialValue ? -1 : 0);
        return bitVector;
    }

    private static class FloorEuclideanDiv {
        int div, mod;

        FloorEuclideanDiv(int dividend, int divisor) {
            div = Math.floorDiv(dividend, divisor);
            mod = Math.floorMod(dividend, divisor);
        }
    }

    private BitVector extract(int start, int size, boolean methodZeroExtended) {
        Preconditions.checkArgument(size % Integer.SIZE == 0 && size > 0);
        int[] extractedInts = new int[size / Integer.SIZE];
        FloorEuclideanDiv d = new FloorEuclideanDiv(start, Integer.SIZE);

        if (methodZeroExtended) {
            if (Math.floorMod(start, Integer.SIZE) == 0) {
                for (int i = 0; i < size / Integer.SIZE; i++) {
                    extractedInts[i] = (i < -d.div || i >= d.div + bitVector.length) ? 0 : bitVector[i + d.div];
                }
            } else {
                for (int i = 0; i < size / Integer.SIZE; i++) {
                    extractedInts[i] = intExtZeroExtended(start + Integer.SIZE * i);
                }
            }
        } else {
            if (Math.floorMod(start, Integer.SIZE) == 0) {
                for (int i = 0; i < size / Integer.SIZE; i++) {
                    extractedInts[i] = bitVector[Math.floorMod(i + d.div, bitVector.length)];
                }
            } else {
                for (int i = 0; i < size / Integer.SIZE; i++) {
                    extractedInts[i] = intExtWrapped(start + Integer.SIZE * i);
                }
            }
        }
        return new BitVector(extractedInts);
    }

    private int intExtZeroExtended(int start) {
        FloorEuclideanDiv f = new FloorEuclideanDiv(start, Integer.SIZE);
        if (f.div >= bitVector.length || f.div < -1) {
            return 0;
        } else {
            if (f.div == bitVector.length - 1) {
                return bitVector[f.div] >>> f.mod;
            }
            if (f.div == -1) {
                return (bitVector[f.div + 1] << (Integer.SIZE - f.mod));
            } else {
                return (bitVector[f.div] >>> f.mod) | (bitVector[(f.div + 1)] << (Integer.SIZE - f.mod));
            }
        }
    }

    private int intExtWrapped(int start) {
        FloorEuclideanDiv f = new FloorEuclideanDiv(start, Integer.SIZE);
        int startIntIndex = Math.floorDiv((Math.floorMod(start, size())), Integer.SIZE);
        return (bitVector[startIntIndex] >>> f.mod)
                | (bitVector[Math.floorMod((startIntIndex + 1), bitVector.length)] << (Integer.SIZE - f.mod));
    }

    // Constructeur privé
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
        return Bits.test(bitVector[Math.floorDiv(Objects.checkIndex(index, size()), Integer.SIZE)],
                Math.floorMod(index, Integer.SIZE));
    }

    /**
     * Effectue la négation du vecteur de bits.
     * 
     * @return la négation
     */
    public BitVector not() {
        int[] notVector = new int[bitVector.length];

        for (int i = 0; i < notVector.length; i++) {
            notVector[i] = ~bitVector[i];
        }

        return new BitVector(notVector);
    }

    /**
     * Effectue le ET entre ce vecteur et un autre.
     * 
     * @param otherVector
     * l'autre vecteur
     * @return la conjonction
     */
    public BitVector and(BitVector otherVector) {
        Preconditions.checkArgument(
                size() == Objects.requireNonNull(otherVector, "The provided vector must not be null").size(),
                "Two bit vectors of different sizes cannot be compared.");

        int[] andVector = new int[bitVector.length];

        for (int i = 0; i < andVector.length; i++) {
            andVector[i] = bitVector[i] & otherVector.getArray()[i];
        }

        return new BitVector(andVector);
    }

    /**
     * Effectue le OU entre ce vecteur et un autre.
     * 
     * @param otherVector
     * l'autre vecteur
     * @return la disjonction
     */
    public BitVector or(BitVector otherVector) {
        Preconditions.checkArgument(
                size() == Objects.requireNonNull(otherVector, "The provided vector must not be null").size(),
                "Two bit vectors of different sizes cannot be compared.");

        int[] orVector = new int[bitVector.length];

        for (int i = 0; i < orVector.length; i++) {
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
        return (o instanceof BitVector) && Arrays.equals(bitVector, ((BitVector) o).getArray());
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(bitVector);
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        for (int i = size() - 1; i >= 0; --i) {
            b.append(testBit(i) ? 1 : 0);
        }
        return b.toString();
    }

    public static final class Builder {

        private int[] vector;

        public Builder(int size) {
            vector = bitVector(size, false);
        }

        /**
         * Change la valeur d'un octet.
         * 
         * @param index
         * l'index de l'octet
         * @param b
         * la valeur à lui donner
         * @return le builder
         */
        public Builder setByte(int index, int b) {
            if (vector == null) {
                throw new IllegalStateException();
            }
            Objects.checkIndex(index, (vector.length * Integer.SIZE) / Byte.SIZE);
            int intIndex = Math.floorDiv(index * Byte.SIZE, Integer.SIZE);
            int startPosition = Math.floorMod(index * Byte.SIZE, Integer.SIZE);
            vector[intIndex] &= ~(0b1111_1111 << startPosition);
            vector[intIndex] |= Preconditions.checkBits8(b) << startPosition;
            return this;
        }

        /**
         * Construit le vecteur de bits.
         * 
         * @return le vecteur
         */
        public BitVector build() {
            if (vector == null) {
                throw new IllegalStateException();
            }
            BitVector built = new BitVector(vector);
            vector = null;
            return built;
        }
    }
}

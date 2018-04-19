package ch.epfl.gameboj;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import ch.epfl.gameboj.bits.BitVector;

class BitVectorTestAM {
    
    @Test
    void creator() {
        BitVector v1 = new BitVector(32, true);
        assertEquals("11111111111111111111111111111111", v1.toString());
    }
    
    @Test
    void not() {
        BitVector v1 = new BitVector(32, true);
        BitVector v2 = v1.not();
        assertEquals("00000000000000000000000000000000", v2.toString());
    }
    
    @Test
    void or() {
        BitVector v1 = new BitVector(32, true);
        BitVector v2 = new BitVector(32, false);
        BitVector v3 = v1.or(v2);
        assertEquals("11111111111111111111111111111111", v3.toString());
    }
    
    @Test
    void and() {
        BitVector v1 = new BitVector(32, true);
        BitVector v2 = new BitVector(32, false);
        BitVector v3 = v1.and(v2);
        assertEquals("00000000000000000000000000000000", v3.toString());
    }
    
    @Test
    void TEST() {
        BitVector v1 = new BitVector(32, true);
        BitVector v2 = v1.extractZeroExtended(-17, 32).not();
        BitVector v3 = v2.extractWrapped(11, 64);
        assertEquals("11111111111111111111111111111111", v1.toString());
        assertEquals("00000000000000011111111111111111", v2.toString());
        assertEquals("1111111111100000000000000011111111111111111000000000000000111111", v3.toString());

    }
    
    @Test
    void TEST2() {
        BitVector v1 = new BitVector(32, true);
        BitVector v2 = v1.extractZeroExtended(-57, 128);
        BitVector v3 = v2.extractWrapped(11, 128);
        assertEquals("00000000000000000000000000000000000000011111111111111111111111111111111000000000000000000000000000000000000000000000000000000000", v2.toString());
        assertEquals("00000000000000000000000000000000000000000000000000111111111111111111111111111111110000000000000000000000000000000000000000000000", v3.toString());
    }
    @Test
    void TEST3() {
        BitVector v1 = new BitVector(64, true);
        BitVector v2 = v1.extractZeroExtended(-32, 64);
        BitVector v3 = v2.extractWrapped(-32, 96);
        assertEquals("1111111111111111111111111111111111111111111111111111111111111111", v1.toString());
        assertEquals("1111111111111111111111111111111100000000000000000000000000000000", v2.toString());
        assertEquals("111111111111111111111111111111110000000000000000000000000000000011111111111111111111111111111111", v3.toString());
    }
    
    @Test
    void testBuilder() {
        BitVector v = new BitVector.Builder(32)
                .setByte(0, 0b1111_0000)
                .setByte(1, 0b1010_1010)
                .setByte(3, 0b1100_1100)
                .build();
              assertEquals("11001100000000001010101011110000", v.toString());
    }
    
    @Test
    void testBuilder2() {
        BitVector v = new BitVector.Builder(64)
                .setByte(0, 0b1111_0000)
                .setByte(1, 0b1010_1010)
                .setByte(3, 0b1100_1100)
                .setByte(7, 0b1111_1111)
                .build();
              assertEquals("1111111100000000000000000000000011001100000000001010101011110000", v.toString());
    }
    
    @Test
    void testBuilder3() {
        BitVector v = new BitVector.Builder(32)
                .setByte(0, 0b1111_0000)
                .setByte(1, 0b1010_1010)
                .setByte(3, 0b1100_1100)
                .setByte(3, 0b0011_1111)
                .build();
              assertEquals("00111111000000001010101011110000", v.toString());
    }
    

}

package ch.epfl.gameboj.bits;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BitVectorTest {

    BitVector v1, v2, v3;
    
    @BeforeEach
    void initAll() {
        v1 = new BitVector.Builder(64)
                .setByte(0, 0b1111_0000)
                .setByte(1 * Byte.SIZE, 0b1010_1010)
                .setByte(3 * Byte.SIZE, 0b1100_1100)
                .setByte(6 * Byte.SIZE, 0b1111_1111)
                .build();
        //0000000011111111000000000000000011001100000000001010101011110000
        //0000000000000000110011000000000010101010111100000000000000000000  
        v2 = new BitVector.Builder(64)
                .setByte(0, 0b1101_1100)
                .setByte(1 * Byte.SIZE, 0b1000_1000)
                .setByte(3 * Byte.SIZE, 0b1101_1110)
                .setByte(6 * Byte.SIZE, 0b0011_0110)
                .build();
        //0000000000110110000000000000000011011110000000001000100011011100  
        
        v3 = new BitVector.Builder(64)
                .setByte(0, 0b1101_1100)
                .setByte(1 * Byte.SIZE, 0b1000_1000)
                .setByte(3 * Byte.SIZE, 0b1101_1110)
                .setByte(6 * Byte.SIZE, 0b0011_0110)
                .build();
    }
    
    @Test
    void builderThrowsExceptionIfArrayNotMultipleOf32() {
        assertThrows(IllegalArgumentException.class, () -> { 
            new BitVector.Builder(22); });
    }
    
    @Test
    void builderThrowsExceptionIfAlreadyUsed() {
        BitVector.Builder b = new BitVector.Builder(64);
        b.build();
        assertThrows(IllegalStateException.class, () -> { 
            b.setByte(8 * Byte.SIZE, 0b0001_1100); });
    }
    
    @Test
    void builderSetByteWorks() {
        BitVector.Builder b = new BitVector.Builder(64);
        b.setByte(7 * Byte.SIZE, 0b1110_1100);
        assertEquals("1110110000000000000000000000000000000000000000000000000000000000", b.build().toString());
    }   
    
    @Test
    void bitVectorToStringWorks() {
        BitVector.Builder b = new BitVector.Builder(64);
        assertEquals("0000000000000000000000000000000000000000000000000000000000000000", b.build().toString());
    }
       
      
    @Test
    void extractWrapped() {
        assertEquals("0011001100000000001010101011110000000000001111111100000000000000",
                v1.extractWrapped(-30, 64).toString());
    }
    
    @Test
    void andWorks() {
        assertEquals("0000000000110110000000000000000011001100000000001000100011010000", v1.and(v2).toString());
    }
    
    @Test
    void orWorks() {
        assertEquals("0000000011111111000000000000000011011110000000001010101011111100", v1.or(v2).toString());
    }
    
    @Test
    void shiftWorks() {
        assertEquals("0000000000000000110011000000000010101010111100000000000000000000", v1.shift(-16).toString());
    }
    
    @Test
    void extractWhenModulo32() {
        assertEquals("00000000111111110000000000000000", v1.extractWrapped(32, 32).toString());
    }
    
    @Test
    void testBitWorks() {
        assertFalse(v1.testBit(23));
    }
    
    @Test
    void shinz1() {
        BitVector v1 = new BitVector(32, true);
        BitVector v2 = v1.extractZeroExtended(-17, 32).not();
        BitVector v3 = v2.extractWrapped(11, 64);
        for (BitVector v: List.of(v1, v2, v3)) {
            System.out.println(v);
        }
        //Should print
        //11111111111111111111111111111111
        //00000000000000011111111111111111
        //1111111111100000000000000011111111111111111000000000000000111111
    }
    
    @Test
    void shinz2() {
        BitVector v = new BitVector.Builder(32)
                .setByte(0, 0b1111_0000)
                .setByte(1 * Byte.SIZE, 0b1010_1010)
                .setByte(3 * Byte.SIZE, 0b1100_1100)
                .build();
        assertEquals("11001100000000001010101011110000", v.toString());
    }
    
    @Test
    void hashWorks() {
        assertEquals(v2.hashCode(), v3.hashCode());      
    }
    
    @Test
    void equalsWorks() {
        assertTrue(v2.equals(v3));      
    }
    
    // TESTS EXTRACTZEROEXTENDED
    @Test
    public void extractZeroExtendedTest() {
        BitVector v1 = new BitVector(32, true);
        BitVector v2 = new BitVector(32);
        // 11001100000000001010101011110000
        BitVector v3 = new BitVector.Builder(32).setByte(0, 0b1111_0000).setByte(1 * Byte.SIZE, 0b1010_1010)
                .setByte(3 * Byte.SIZE, 0b1100_1100).build();
        // 00000000110101011111111000000000
        BitVector v4 = new BitVector.Builder(32).setByte(0, 0b0000_0000).setByte(1 * Byte.SIZE, 0b1111_1110)
                .setByte(2 * Byte.SIZE, 0b1101_0101).build();
        
        BitVector v5 = v1.extractZeroExtended(-4, 64);
        assertEquals("0000000000000000000000000000111111111111111111111111111111110000", v5.toString());
        BitVector v6 = v2.extractZeroExtended(-4, 64);
        assertEquals("0000000000000000000000000000000000000000000000000000000000000000", v6.toString());
        BitVector v7 = v3.extractZeroExtended(-1, 32);
        assertEquals("10011000000000010101010111100000", v7.toString());
        BitVector v8 = v4.extractZeroExtended(-14, 64);
        assertEquals("0000000000000000000000000011010101111111100000000000000000000000", v8.toString());
        BitVector v9 = v3.extractZeroExtended(-32, 32);
        assertEquals("00000000000000000000000000000000", v9.toString());
    }

}

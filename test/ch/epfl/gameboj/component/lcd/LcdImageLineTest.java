package ch.epfl.gameboj.component.lcd;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ch.epfl.gameboj.bits.BitVector;

class LcdImageLineTest {
    LcdImageLine lcdImageLine;
    int width = 256;
    
    @BeforeEach
    void setupBeforeEach() {
        
    }

    @Test
    void testHashCode() {
        fail("Not yet implemented");
    }

    @Test
    void testSize() {
        lcdImageLine = new LcdImageLine(new BitVector(width), new BitVector(width), new BitVector(width));
        assertEquals(width, lcdImageLine.size());
    }

    @Test
    void testShift() {
        fail("Not yet implemented");
    }

    @Test
    void testExtractWrapped() {
        fail("Not yet implemented");
    }

    @Test
    void testMapColors() {
        fail("Not yet implemented");
    }

    @Test
    void testBelowLcdImageLine() {
        fail("Not yet implemented");
    }

    @Test
    void testBelowLcdImageLineBitVector() {
        fail("Not yet implemented");
    }

    @Test
    void testJoin() {
        fail("Not yet implemented");
    }

    @Test
    void testEqualsObject() {
        fail("Not yet implemented");
    }

}

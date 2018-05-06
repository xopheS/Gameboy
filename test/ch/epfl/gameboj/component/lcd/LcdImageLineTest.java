package ch.epfl.gameboj.component.lcd;

import static org.junit.jupiter.api.Assertions.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;

import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ch.epfl.gameboj.bits.BitVector;

class LcdImageLineTest {
    
    static BitVector mockMSB, mockLSB, mockAlpha, mockOtherMSB, mockOtherLSB, mockOtherAlpha;
    LcdImageLine lcdImageLine;
    static int width = 256;
    
    @BeforeAll
    static void setupBeforeAll() {
        mockLSB = mock(BitVector.class);
        mockMSB = mock(BitVector.class);
        mockAlpha = mock(BitVector.class);
        
        when(mockMSB.size()).thenReturn(width);
        when(mockLSB.size()).thenReturn(width);
        when(mockAlpha.size()).thenReturn(width);
    }
    
    @BeforeEach
    void setupBeforeEach() {
        
    }

    @Test
    void testSize() {
        lcdImageLine = new LcdImageLine(new BitVector(width), new BitVector(width), new BitVector(width));
        assertThat(width, is(equalTo(lcdImageLine.size())));
    }
    
    @Test
    void testGetMSBWorks() {
        lcdImageLine = new LcdImageLine(mockMSB, mockLSB, mockAlpha);
        assertThat(lcdImageLine.getMsb(), is(equalTo(mockMSB)));
    }
    
    @Test
    void testGetLSBWorks() {
        lcdImageLine = new LcdImageLine(mockMSB, mockLSB, mockAlpha);
        assertThat(lcdImageLine.getLsb(), is(equalTo(mockLSB)));
    }
    
    @Test
    void testGetOpacityWorks() {
        lcdImageLine = new LcdImageLine(mockMSB, mockLSB, mockAlpha);
        assertThat(lcdImageLine.getOpacity(), is(equalTo(mockAlpha)));
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
    void testEqualsWorksWhenEqual() {
        lcdImageLine = new LcdImageLine(mockMSB, mockLSB, mockAlpha);
        
        BitVector mockOtherMSB = mock(BitVector.class), mockOtherLSB = mock(BitVector.class), mockOtherAlpha = mock(BitVector.class);
        when(mockOtherMSB.size()).thenReturn(width);
        when(mockOtherLSB.size()).thenReturn(width);
        when(mockOtherAlpha.size()).thenReturn(width);
        
        LcdImageLine otherLcdImageLine = new LcdImageLine(mockOtherMSB, mockOtherLSB, mockOtherAlpha);
        
        when(mockMSB.equals(mockOtherMSB)).thenReturn(true);
        when(mockLSB.equals(mockOtherLSB)).thenReturn(true);
        when(mockAlpha.equals(mockOtherAlpha)).thenReturn(true);
        
        assertThat(lcdImageLine, is(equalTo(otherLcdImageLine)));
    }
    
    @Test
    void testEqualsFailsWhenDifferent() {
        lcdImageLine = new LcdImageLine(mockMSB, mockLSB, mockAlpha);
        
        BitVector mockOtherMSB = mock(BitVector.class), mockOtherLSB = mock(BitVector.class), mockOtherAlpha = mock(BitVector.class);
        
        LcdImageLine otherLcdImageLine = new LcdImageLine(mockOtherMSB, mockOtherLSB, mockOtherAlpha);
        
        when(mockMSB.equals(mockOtherMSB)).thenReturn(false);
        when(mockLSB.equals(mockOtherLSB)).thenReturn(false);
        when(mockAlpha.equals(mockOtherAlpha)).thenReturn(true);
        
        assertThat(lcdImageLine, is(not(equalTo(otherLcdImageLine))));
    }
}

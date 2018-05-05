package ch.epfl.gameboj.component.lcd;

import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.hamcrest.*;
import org.hamcrest.core.*;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ch.epfl.gameboj.component.cpu.Cpu;
import ch.epfl.gameboj.component.lcd.LcdController;

class LcdControllerTest {
    static final int LCD_WIDTH = 160;
    static final int LCD_HEIGHT = 144;
    
    Cpu mockCpu;
    LcdController lcdController;
    
    @BeforeAll
    void setupBeforeAll() {
        mockCpu = mock(Cpu.class);
    }

    @BeforeEach
    void setupBeforeEach() {
        mockCpu = mock(Cpu.class);
        lcdController = new LcdController(mockCpu);
    }
    
    @Test
    void testLcdController() {
        fail("Not yet implemented");
    }

    @Test
    void testCurrentImageInitiallyBlank() {
        assertThat(lcdController.currentImage(), is(equalTo(new LcdImageLine(new BitVector())));
    }

    @Test
    void testCycle() {
        long[] firstTenVBLANK = new long[] { 83302, 100858, 118414, 135970, 153526, 171082, 188638, 206194, 223750, 241306 };
        long[] detectedVBLANK = new long[10];
        fail("Not yet implemented");
    }

    @Test
    void testAttachTo() {
        fail("Not yet implemented");
    }

    @Test
    void testRead() {
        fail("Not yet implemented");
    }

    @Test
    void testWrite() {
        fail("Not yet implemented");
    }

}

package ch.epfl.gameboj.component.lcd;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ch.epfl.gameboj.component.cpu.Cpu;
import ch.epfl.gameboj.component.lcd.LcdController;

class LcdControllerTest {
    Cpu mockCpu;
    LcdController lcdController;
    
    @BeforeAll
    void setupBeforeAll() {
        mockCpu = new Cpu();
    }

    @BeforeEach
    void setupBeforeEach() {
        lcdController = new LcdController(mockCpu);
    }
    
    @Test
    void testLcdController() {
        fail("Not yet implemented");
    }

    @Test
    void testCurrentImage() {
        fail("Not yet implemented");
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

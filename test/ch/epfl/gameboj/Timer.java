package ch.epfl.gameboj;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import ch.epfl.gameboj.AddressMap;
import ch.epfl.gameboj.component.Timer;
import ch.epfl.gameboj.component.cpu.Cpu;

class TimerTest {

    private Cpu c = new Cpu();
    private Timer t = new Timer(c);

    private int[] getRegisters() {
        int[] r = { t.read(AddressMap.REG_DIV), t.read(AddressMap.REG_TIMA),
                t.read(AddressMap.REG_TMA), t.read(AddressMap.REG_TAC) };
        return r;
    }

    @Test
    void readAndWriteWorkForDivTimaTma() {
        for (int i = 0; i < 256; ++i) {
            t.write(AddressMap.REG_DIV, i);
            assertArrayEquals(new int[] { 0, 0, 0, 0 }, getRegisters());
        }
        for (int i = 0; i < 256; ++i) {
            t.write(AddressMap.REG_TIMA, i);
            assertArrayEquals(new int[] { 0, i, 0, 0 }, getRegisters());
        }
        for (int i = 0; i < 256; ++i) {
            t.write(AddressMap.REG_TMA, i);
            assertArrayEquals(new int[] { 0, 255, i, 0 }, getRegisters());
        }
    }

    // test pour bit 3; non exhaustif
    @Test
    void incTimerModifyTima1() {

        t.write(AddressMap.REG_TAC, 0b101);

        t.cycle(0); // 100
        // state : false
        assertArrayEquals(new int[] { 0, 0, 0, 0b101 }, getRegisters());

        t.cycle(0); // 1000
        // state : true
        assertArrayEquals(new int[] { 0, 0, 0, 0b101 }, getRegisters());

        t.cycle(0); // 1100
        // state : true
        assertArrayEquals(new int[] { 0, 0, 0, 0b101 }, getRegisters());

        t.cycle(0); // 10000
        // state : false
        assertArrayEquals(new int[] { 0, 1, 0, 0b101 }, getRegisters());

        t.cycle(0); // 10100
        // state : false
        assertArrayEquals(new int[] { 0, 1, 0, 0b101 }, getRegisters());

        t.cycle(0); // 11000
        // state : true
        assertArrayEquals(new int[] { 0, 1, 0, 0b101 }, getRegisters());

        t.cycle(0); // 11100
        // state : true
        assertArrayEquals(new int[] { 0, 1, 0, 0b101 }, getRegisters());

        t.cycle(0); // 100000
        // state : false
        assertArrayEquals(new int[] { 0, 2, 0, 0b101 }, getRegisters());

    }

    // test pour bit 3 : exhaustif
    @Test
    void incTimerModifyTima2() {

        t.write(AddressMap.REG_TAC, 0b101); // bit 3

        int it = 0;
        int DIV = 0;

        for (int i = 0; i < 255; ++i) {
            t.cycle(0);
            t.cycle(0);
            t.cycle(0);
            it += 12;
            if (it >= 256 * (DIV + 1))
                ++DIV;
            assertArrayEquals(new int[] { DIV, i, 0, 0b101 }, getRegisters());
            t.cycle(0);
            it += 4;
            if (it >= 256 * (DIV + 1))
                ++DIV;
            assertArrayEquals(new int[] { DIV, i + 1, 0, 0b101 },
                    getRegisters());
        }

    }

    // test pour bit 5 : exhaustif
    @Test
    void incTimerModifyTima3() {

        t.write(AddressMap.REG_TAC, 0b110); // bit 5

        int it = 0;
        int DIV = 0;

        for (int i = 0; i < 255; ++i) {
            for (int j = 0; j < 15; ++j)
                t.cycle(0);
            it += 60;
            if (it >= 256 * (DIV + 1))
                ++DIV;
            assertArrayEquals(new int[] { DIV, i, 0, 0b110 }, getRegisters());
            t.cycle(0);
            it += 4;
            if (it >= 256 * (DIV + 1))
                ++DIV;
            assertArrayEquals(new int[] { DIV, i + 1, 0, 0b110 },
                    getRegisters());
        }
    }

    // test pour bit 7 : exhaustif
    @Test
    void incTimerModifyTima4() {

        t.write(AddressMap.REG_TAC, 0b111); // bit 7

        int it = 0;
        int DIV = 0;

        for (int i = 0; i < 255; ++i) {
            for (int j = 0; j < 63; ++j)
                t.cycle(0);
            it += 252;
            if (it >= 256 * (DIV + 1))
                ++DIV;
            assertArrayEquals(new int[] { DIV, i, 0, 0b111 }, getRegisters());
            t.cycle(0);
            it += 4;
            if (it >= 256 * (DIV + 1))
                ++DIV;
            assertArrayEquals(new int[] { DIV, i + 1, 0, 0b111 },
                    getRegisters());
        }

    }

    // test pour bit 9 : peut-etre obsolete (4*255>256)
    @Test
    void incTimerModifyTima5() {

        t.write(AddressMap.REG_TAC, 0b100); // bit 9

        int it = 0;
        int DIV = 0;

        for (int i = 0; i < 100; ++i) {
            for (int j = 0; j < 255; ++j)
                t.cycle(0);
            it += 1020;
            if (it >= 256 * (DIV + 1)) {
                if (DIV == 255)
                    DIV = 0;
                else
                    ++DIV;
            }
            if (it >= 256 * (DIV + 1)) {
                if (DIV == 255)
                    DIV = 0;
                else
                    ++DIV;
            }
            if (it >= 256 * (DIV + 1)) {
                if (DIV == 255)
                    DIV = 0;
                else
                    ++DIV;
            }
            assertArrayEquals(new int[] { DIV, i, 0, 0b100 }, getRegisters());
            t.cycle(0);
            it += 4;
            if (it >= 256 * (DIV + 1)) {
                if (DIV == 255)
                    DIV = 0;
                else
                    ++DIV;
            }
            assertArrayEquals(new int[] { DIV, i + 1, 0, 0b100 },
                    getRegisters());
        }

    }
    
    @Test
    void TimaGoesBackToTma() {
        
        t.write(AddressMap.REG_TMA, 42);
        t.write(AddressMap.REG_TIMA, 255);
        t.write(AddressMap.REG_TAC, 0b101);
        
        t.cycle(0);
        t.cycle(0);
        t.cycle(0);
        t.cycle(0);
        
        assertArrayEquals(new int[] {0, 42, 42, 0b101}, getRegisters());
        assertEquals(0b100, c.read(AddressMap.REG_IF));
        
    }
    
    @Test
    void ThirdBitOfTacChangeState() {
        
        t.write(AddressMap.REG_TAC, 0b101);
        t.cycle(0);
        t.cycle(0);
        t.write(AddressMap.REG_TAC, 0b001);
        assertArrayEquals(new int[] {0, 1, 0, 0b001}, getRegisters());
        
    }
    
    

}

package ch.epfl.gameboj.component.lcd;

import static ch.epfl.gameboj.component.lcd.LcdImage.BLANK_LCD_IMAGE;
import static ch.epfl.gameboj.component.lcd.LcdController.IMAGE_CYCLE_DURATION;
import static ch.epfl.gameboj.component.Component.NO_DATA;

import static org.junit.jupiter.api.Assertions.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.CoreMatchers.*;

import static org.mockito.Mockito.*;

import java.util.Arrays;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import ch.epfl.gameboj.AddressMap;
import ch.epfl.gameboj.Bus;
import ch.epfl.gameboj.bits.Bits;
import ch.epfl.gameboj.component.cpu.Cpu;
import ch.epfl.gameboj.component.cpu.Cpu.Interrupt;
import ch.epfl.gameboj.component.lcd.LcdController;

class LcdControllerTest {
    static final int LCD_WIDTH = 160;
    static final int LCD_HEIGHT = 144;
    
    static Cpu mockCpu;  
    static Bus mockBus;
    LcdController lcdController;
    
    int vblankNum = 0;
    long currentCycle = 0;
    
    @BeforeAll
    static void setupBeforeAll() {   
        
    }

    @BeforeEach
    void setupBeforeEach() {
        mockCpu = mock(Cpu.class);
        
        mockBus = mock(Bus.class);
        
        lcdController = new LcdController(mockCpu);
        
        lcdController.attachTo(mockBus);
    }

    @Test
    void testCurrentImageInitiallyBlank() {
        assertThat(lcdController.currentImage(), is(equalTo(BLANK_LCD_IMAGE)));
    }
    
    @Test
    void testQuickCopyWorks() {        
        int[] busMem = new int[500];
        int startAddressMSB = 0b01101101;
        int startAddress = startAddressMSB << Byte.SIZE;
        
        for (int i = startAddress; i < startAddress + 500; ++i) {
            busMem[i - startAddress] = Bits.clip(8, i);
            when(mockBus.read(i)).thenReturn(Bits.clip(8, i));
        }
        
        lcdController.write(AddressMap.REGS_LCD_START + 6, startAddressMSB);

        for (int i = 0; i < 160; ++i) {
            lcdController.cycle(i);
            assertThat(lcdController.read(AddressMap.OAM_START + i), is(equalTo(busMem[i])));
        }
    }

    @Test
    void testCycleVBLANKAtCorrectInstants() {
        long[] firstTenVBLANK = new long[] { 83302, 100858, 118414, 135970, 153526, 171082, 188638, 206194, 223750, 241306 };
        long[] firstTenVBLANKActual = new long[10];
        long powerOn = firstTenVBLANK[0] - IMAGE_CYCLE_DURATION + 1140; //add 144 for test to work
        
        doAnswer(new Answer<Void>() {
            public Void answer(InvocationOnMock arg0) {
                System.out.println("vblank at " + currentCycle);
                firstTenVBLANKActual[vblankNum] = currentCycle;
                return null;
            }           
        }).when(mockCpu).requestInterrupt(Interrupt.VBLANK);
        
        for (int i = 0; i < 241307; ++i) {
            currentCycle = i;
            if (i == powerOn) {
                System.out.println("attempting to power on at cycle " + i);
                lcdController.write(AddressMap.REGS_LCD_START, 0b1000_0000);
            }
            lcdController.cycle(i);
            if (i == firstTenVBLANK[vblankNum]) {
                vblankNum++;
            }
        }
        
        verify(mockCpu, times(10)).requestInterrupt(Interrupt.VBLANK);
        
        assertThat(firstTenVBLANKActual, is(equalTo(firstTenVBLANK)));
    }

    @Test
    void testAttachTo() {
        verify(mockBus).attach(lcdController);
    }

    @Test
    void testReadReturnsNO_DATAWhenOtherAddress() {
        lcdController.write(AddressMap.VRAM_START + 5, 0x45);
        assertThat(lcdController.read(0x5), is(equalTo(NO_DATA)));
    }
    
    @Test
    void testReadReturnsVRAMWhenVRAMAddress() {
        lcdController.write(AddressMap.VRAM_START + 5, 0x45);
        assertThat(lcdController.read(AddressMap.VRAM_START + 5), is(equalTo(0x45)));
    }
    
    @Test
    void testReadReturnsOAMWhenOAMAddress() {
        lcdController.write(AddressMap.OAM_START + 5, 0x46);
        assertThat(lcdController.read(AddressMap.OAM_START + 5), is(equalTo(0x46)));
    }
    
    @Test
    void testReadReturnsRegsWhenRegsAddress() {
        lcdController.write(AddressMap.REGS_LCD_START + 5, 0b11);
        assertThat(lcdController.read(AddressMap.REGS_LCD_START + 5), is(equalTo(0b11)));
    }

    @Test
    void testWriteDoesNothingWhenOtherAddress() {
        lcdController.write(0, 0x43);
        //TODO
    }

    @Test
    void testWriteWritesToVRAMWhenCorrectAddress() {
        lcdController.write(AddressMap.VRAM_START + 50, 0x23);
        assertThat(lcdController.read(AddressMap.VRAM_START + 50), is(equalTo(0x23)));
    }
    
    @Test
    void testWriteWritesToOAMWhenCorrectAddress() {
        lcdController.write(AddressMap.OAM_START + 50, 0x25);
        assertThat(lcdController.read(AddressMap.OAM_START + 50), is(equalTo(0x25)));
    }
}

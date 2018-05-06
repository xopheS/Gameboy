package ch.epfl.gameboj.component;

import static ch.epfl.gameboj.component.Component.NO_DATA;

import static org.junit.jupiter.api.Assertions.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;

import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ch.epfl.gameboj.AddressMap;
import ch.epfl.gameboj.bits.Bits;
import ch.epfl.gameboj.component.Joypad.Key;
import ch.epfl.gameboj.component.cpu.Cpu;
import ch.epfl.gameboj.component.cpu.Cpu.Interrupt;

class JoypadTest {

    static Cpu mockCpu;
    Joypad joypad;
    
    @BeforeAll
    static void setupBeforeAll() {
        
    }
    
    @BeforeEach
    void setupBeforeEach() {
        mockCpu = mock(Cpu.class);
        joypad = new Joypad(mockCpu);
    }
    
    @Test
    void testJoypadConstructorRefusesNullCpu() {
        assertThrows(NullPointerException.class, () -> new Joypad(null));
    }

    @Test
    void testKeyPressed() {
        joypad.write(AddressMap.REG_P1, Bits.complement8(0b0010_0000));
        joypad.keyPressed(Key.A);
        assertThat(joypad.read(AddressMap.REG_P1), is(equalTo(Bits.complement8(0b0010_0001))));
    }

    @Test
    void testKeyReleased() {
        joypad.write(AddressMap.REG_P1, Bits.complement8(0b0010_0000));
        joypad.keyPressed(Key.A);
        joypad.keyReleased(Key.A);
        assertThat(joypad.read(AddressMap.REG_P1), is(equalTo(Bits.complement8(0b0010_0000))));
    }
    
    @Test
    void testP1requestsInterrupt() {
        joypad.write(AddressMap.REG_P1, Bits.complement8(0b0010_0000));
        joypad.keyPressed(Key.A);
        verify(mockCpu).requestInterrupt(Interrupt.JOYPAD);
    }

    @Test
    void testReadReturnsNO_DATAWhenOtherAddress() {
        joypad.write(AddressMap.REG_P1, Bits.complement8(0b0101_01010));
        assertThat(joypad.read(AddressMap.REG_P1 - 20), is(equalTo(NO_DATA)));
    }
    
    @Test
    void testReadReturnsCorrectDataWhenRegAddress() {
        joypad.write(AddressMap.REG_P1, Bits.complement8(0b0011_0000));
        assertThat(joypad.read(AddressMap.REG_P1), is(equalTo(Bits.complement8(0b0011_0000))));
    }

    @Test
    void testWriteDoesNothingWhenOtherAddress() {
        joypad.write(AddressMap.REG_P1 - 20, Bits.complement8(0b1111_0000));
        assertThat(joypad.read(AddressMap.REG_P1), is(equalTo(Bits.complement8(0b0000_0000))));
    }
    
    @Test
    void testWriteDoesNotModifyP1LSB() {
        joypad.write(AddressMap.REG_P1, Bits.complement8(0b0000_1111));
        assertThat(joypad.read(AddressMap.REG_P1), is(equalTo(Bits.complement8(0))));
    }

    @Test
    void testWriteWritesWhenRegAddress() {
        joypad.write(AddressMap.REG_P1, Bits.complement8(0b0010_0000));
        assertThat(joypad.read(AddressMap.REG_P1), is(equalTo(Bits.complement8(0b0010_0000))));
    }
}

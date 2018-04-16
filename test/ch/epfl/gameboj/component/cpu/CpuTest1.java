package ch.epfl.gameboj.component.cpu;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import ch.epfl.gameboj.Bus;
import ch.epfl.gameboj.component.memory.Ram;
import ch.epfl.gameboj.component.memory.RamController;

class CpuTest1 {
    private Bus connect(Cpu cpu, Ram ram) {
        RamController rc = new RamController(ram, 0);
        Bus b = new Bus();
        cpu.attachTo(b);
        rc.attachTo(b);
        return b;
    }

    private void cycleCpu(Cpu cpu, long cycles) {
        for (long c = 0; c < cycles; ++c)
            cpu.cycle(c);
    }

    @Test
    void nopDoesNothing() {
        Cpu c = new Cpu();
        Ram r = new Ram(10);
        Bus b = connect(c, r);
        b.write(0, Opcode.NOP.encoding);
        cycleCpu(c, Opcode.NOP.cycles);
        assertArrayEquals(new int[] {1,0,0,0,0,0,0,0,0,0}, c._testGetPcSpAFBCDEHL());
    }
    
    @Test
    void LD_R8_HLR_Works() {
        Cpu c = new Cpu();
        Ram r = new Ram(10);
        Bus b = connect(c, r);
        b.write(0,  Opcode.LD_A_HLR.encoding);
        cycleCpu(c, Opcode.LD_A_HLR.cycles);
        assertArrayEquals(new int[] {1,0,126,0,0,0,0,0,0,0}, c._testGetPcSpAFBCDEHL());
    }

    
    @Test
    void LD_R8_N8_Works_and_LD_A_B() {
        Cpu c = new Cpu();
        Ram r = new Ram(10);
        Bus b = connect(c, r);
        b.write(0, Opcode.LD_A_N8.encoding);
        b.write(1, 0x11);
        b.write(2, Opcode.LD_B_A.encoding);
        cycleCpu(c, Opcode.LD_A_N8.cycles + Opcode.LD_B_A.cycles);
        assertArrayEquals(new int[] {Opcode.LD_A_N8.totalBytes + Opcode.LD_B_A.totalBytes,0,0x11,0,0x11,0,0,0,0,0}, c._testGetPcSpAFBCDEHL());
    }
    
    @Test
    void LD_A_HLRU_Works() {
        Cpu c = new Cpu();
        Ram r = new Ram(10);
        Bus b = connect(c, r);
        b.write(0, Opcode.LD_A_HLRI.encoding);
        cycleCpu(c, Opcode.LD_A_HLRI.cycles);//increment HL
        assertArrayEquals(new int[] {1,0,42,0,0,0,0,0,0,1}, c._testGetPcSpAFBCDEHL());
    }
    
    @Test
    void POP_16_Works() {
        Cpu c = new Cpu();
        Ram r = new Ram(10);
        Bus b = connect(c, r);
        b.write(0, Opcode.LD_B_A.encoding);
        b.write(1, Opcode.LD_A_B.encoding);
        b.write(2, Opcode.POP_BC.encoding);
        cycleCpu(c, Opcode.LD_A_B.cycles + Opcode.LD_SP_N16.cycles + Opcode.LD_B_A.cycles);
        assertArrayEquals(new int[] {3,2,0,0,0x78,0x47,0,0,0,0}, c._testGetPcSpAFBCDEHL());
    }
    
    @Test
    void ADD_A_B() {
        Cpu c = new Cpu();
        Ram r = new Ram(10);
        Bus b = connect(c, r);
        b.write(0, Opcode.LD_A_N8.encoding);
        b.write(1, 0x13);
        b.write(2, Opcode.LD_B_A.encoding);
        b.write(3, Opcode.ADD_A_B.encoding);
        cycleCpu(c, Opcode.LD_A_N8.cycles + Opcode.LD_B_A.cycles + Opcode.ADD_A_B.cycles);
        assertArrayEquals(new int[] {4,0,0x26,0,0x13,0,0,0,0,0}, c._testGetPcSpAFBCDEHL());
    }
    
    @Test
    void SUB_A_B() {
        Cpu c = new Cpu();
        Ram r = new Ram(10);
        Bus b = connect(c, r);
        b.write(0, Opcode.LD_C_N8.encoding);
        b.write(1, 0x20);
        b.write(2, Opcode.LD_A_N8.encoding);
        b.write(3, 0x13);
        b.write(4, Opcode.SUB_A_C.encoding);
        cycleCpu(c, Opcode.LD_A_N8.cycles + Opcode.LD_C_N8.cycles + Opcode.SUB_A_C.cycles);
        assertArrayEquals(new int[] {5,0,0xF3,80,0,0x20,0,0,0,0}, c._testGetPcSpAFBCDEHL());
    }
         
    
}

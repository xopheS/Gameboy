package ch.epfl.gameboj.component.cpu;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import ch.epfl.gameboj.AddressMap;
import ch.epfl.gameboj.Bus;
import ch.epfl.gameboj.bits.Bits;
import ch.epfl.gameboj.component.memory.Ram;
import ch.epfl.gameboj.component.memory.RamController;

@DisplayName("Testinator 2000")
class CpuTest {
    
    Cpu cpu;
    Ram ram;
    Bus bus;
    Random r;
    int e8, n8a, n8b, n16;
    
    private Bus connect(Cpu cpu, Ram ram) {
        RamController rc = new RamController(ram, 0);
        Bus b = new Bus();
        cpu.attachTo(b);
        rc.attachTo(b);
        return b;
    }

    private void cycleCpu(Cpu cpu, long cycles) {
        for (long c = 0; c < cycles; ++c) {
            cpu.cycle(c);
        }
    }
    
    private void writeAllBytes(int... byteArray) {
        for (int i = 0; i < byteArray.length; i++) {
            bus.write(i, byteArray[i]);
        }
    }
    
    @BeforeEach
    void initAll() {
        cpu = new Cpu();
        ram = new Ram(0xFFFF - 1);
        bus = connect(cpu, ram);
        r = new Random();
        e8 = Bits.clip(8, r.nextInt());
        e8 = e8 > cpu._testGetPcSpAFBCDEHL()[0] ? 0 : e8;
        n8a = Bits.clip(8, r.nextInt());
        n8b = Bits.clip(8, r.nextInt());
        n16 = Bits.make16(n8b, n8a);
    }

    @RepeatedTest(5)
    void JP_HLworksCorrectly() {
        writeAllBytes(Opcode.LD_HL_N16.encoding, 0x22, 0x22, Opcode.JP_HL.encoding);
        cycleCpu(cpu, Opcode.LD_HL_N16.cycles + Opcode.JP_HL.cycles);
        assertArrayEquals(new int[] {0x2222, 0, 0, 0, 0, 0, 0, 0, 0x22, 0x22}, cpu._testGetPcSpAFBCDEHL());
    }
    
    @RepeatedTest(5)
    void JP_N16worksCorrectly() {
        writeAllBytes(Opcode.JP_N16.encoding, 0x22, 0x22);
        cycleCpu(cpu, Opcode.JP_N16.cycles);
        assertArrayEquals(new int[] {0x2222, 0, 0, 0, 0, 0, 0, 0, 0, 0}, cpu._testGetPcSpAFBCDEHL());
    }
    
    @Nested 
    class JP_CC_N16worksCorrectly {
        
        @RepeatedTest(5)
        void JP_CC_N16worksCorrectly_C() {
            cpu.setF(false, true);
            writeAllBytes(Opcode.JP_C_N16.encoding, 0x22, 0x22);
            cycleCpu(cpu, Opcode.JP_C_N16.cycles + Opcode.JP_C_N16.additionalCycles);
            assertArrayEquals(new int[] {0x2222, 0, 0, 0b00010000, 0, 0, 0, 0, 0, 0}, cpu._testGetPcSpAFBCDEHL());
        }
        
        @RepeatedTest(5)
        void JP_CC_N16worksCorrectly_NC() {
            cpu.setF(false, false);
            writeAllBytes(Opcode.JP_NC_N16.encoding, 0x22, 0x22);
            cycleCpu(cpu, Opcode.JP_NC_N16.cycles + Opcode.JP_NC_N16.additionalCycles);
            assertArrayEquals(new int[] {0x2222, 0, 0, 0b00000000, 0, 0, 0, 0, 0, 0}, cpu._testGetPcSpAFBCDEHL());
        }
        
        @RepeatedTest(5)
        void JP_CC_N16worksCorrectly_Z() {
            cpu.setF(true, false);
            writeAllBytes(Opcode.JP_Z_N16.encoding, 0x22, 0x22);
            cycleCpu(cpu, Opcode.JP_Z_N16.cycles + Opcode.JP_Z_N16.additionalCycles);
            assertArrayEquals(new int[] {0x2222, 0, 0, 0b10000000, 0, 0, 0, 0, 0, 0}, cpu._testGetPcSpAFBCDEHL());
        }
        
        @RepeatedTest(5)
        void JP_CC_N16worksCorrectly_NZ() {
            cpu.setF(false, false);
            writeAllBytes(Opcode.JP_NZ_N16.encoding, 0x22, 0x22);
            cycleCpu(cpu, Opcode.JP_NZ_N16.cycles + Opcode.JP_NZ_N16.additionalCycles);
            assertArrayEquals(new int[] {0x2222, 0, 0, 0b00000000, 0, 0, 0, 0, 0, 0}, cpu._testGetPcSpAFBCDEHL());
        }
    }
     
    @RepeatedTest(5)
    void JR_E8worksCorrectly() {
        writeAllBytes(Opcode.JR_E8.encoding, e8);
        cycleCpu(cpu, Opcode.JR_E8.cycles);
        assertArrayEquals(new int[] {2 + e8, 0, 0, 0b00000000, 0, 0, 0, 0, 0, 0}, cpu._testGetPcSpAFBCDEHL());
    }
    
    @Nested
    class JR_CC_E8worksCorrectly {
        
        @RepeatedTest(5)
        void JR_CC_E8worksCorrectly_C() {
            cpu.setF(false, true);
            writeAllBytes(Opcode.JR_C_E8.encoding, e8);
            cycleCpu(cpu, Opcode.JR_C_E8.cycles + Opcode.JR_C_E8.additionalCycles);
            assertArrayEquals(new int[] {2 + e8, 0, 0, 0b00010000, 0, 0, 0, 0, 0, 0}, cpu._testGetPcSpAFBCDEHL());
        }
        
        @RepeatedTest(5) 
        void JR_CC_E8worksCorrectly_NC() {
            cpu.setF(false, false);
            writeAllBytes(Opcode.JR_NC_E8.encoding, e8);
            cycleCpu(cpu, Opcode.JR_NC_E8.cycles + Opcode.JR_NC_E8.additionalCycles);
            assertArrayEquals(new int[] {2 + e8, 0, 0, 0b00000000, 0, 0, 0, 0, 0, 0}, cpu._testGetPcSpAFBCDEHL());
        }
        
        @RepeatedTest(5) 
        void JR_CC_E8worksCorrectly_Z() {
            cpu.setF(true, false);
            writeAllBytes(Opcode.JR_Z_E8.encoding, e8);
            cycleCpu(cpu, Opcode.JR_Z_E8.cycles + Opcode.JR_Z_E8.additionalCycles);
            assertArrayEquals(new int[] {2 + e8, 0, 0, 0b10000000, 0, 0, 0, 0, 0, 0}, cpu._testGetPcSpAFBCDEHL());
        }
        
        @RepeatedTest(5) 
        void JR_CC_E8worksCorrectly_NZ() {
            cpu.setF(false, false);
            writeAllBytes(Opcode.JR_NZ_E8.encoding, e8);
            cycleCpu(cpu, Opcode.JR_NZ_E8.cycles + Opcode.JR_NZ_E8.additionalCycles);
            assertArrayEquals(new int[] {2 + e8, 0, 0, 0b00000000, 0, 0, 0, 0, 0, 0}, cpu._testGetPcSpAFBCDEHL());
        }
    }
    
    @Test
    void CALL_N16worksCorrectly() {
        writeAllBytes(Opcode.LD_SP_N16.encoding, 0xFF, 0xFF, Opcode.CALL_N16.encoding, n8a, n8b);
        cycleCpu(cpu, Opcode.LD_SP_N16.cycles + Opcode.CALL_N16.cycles);
        assertArrayEquals(new int[] {n16, 0xFFFF - 2, 0, 0b00000000, 0, 0, 0, 0, 0, 0}, cpu._testGetPcSpAFBCDEHL());
    }
    
    @Nested
    class CALL_CC_N16worksCorrectly {
        
        @RepeatedTest(5)
        void CALL_CC_N16worksCorrectlyC() {
            cpu.setF(false, true);
            writeAllBytes(Opcode.LD_SP_N16.encoding, 0xFF, 0xFF, Opcode.CALL_C_N16.encoding, n8a, n8b);
            cycleCpu(cpu, Opcode.LD_SP_N16.cycles + Opcode.CALL_C_N16.cycles + Opcode.CALL_C_N16.additionalCycles);
            assertArrayEquals(new int[] {n16, 0xFFFF - 2, 0, 0b00010000, 0, 0, 0, 0, 0, 0}, cpu._testGetPcSpAFBCDEHL());
        }
        
        @RepeatedTest(5)
        void CALL_CC_N16worksCorrectlyNC() {
            cpu.setF(false, false);
            writeAllBytes(Opcode.LD_SP_N16.encoding, 0xFF, 0xFF, Opcode.CALL_NC_N16.encoding, n8a, n8b);
            cycleCpu(cpu, Opcode.LD_SP_N16.cycles + Opcode.CALL_NC_N16.cycles + Opcode.CALL_NC_N16.additionalCycles);
            assertArrayEquals(new int[] {n16, 0xFFFF - 2, 0, 0b00000000, 0, 0, 0, 0, 0, 0}, cpu._testGetPcSpAFBCDEHL());
        }
        
        @RepeatedTest(5)
        void CALL_CC_N16worksCorrectlyZ() {
            cpu.setF(true, false);
            writeAllBytes(Opcode.LD_SP_N16.encoding, 0xFF, 0xFF, Opcode.CALL_Z_N16.encoding, n8a, n8b);
            cycleCpu(cpu, Opcode.CALL_Z_N16.cycles + Opcode.CALL_Z_N16.cycles + Opcode.CALL_Z_N16.additionalCycles);
            assertArrayEquals(new int[] {n16, 0xFFFF - 2, 0, 0b10000000, 0, 0, 0, 0, 0, 0}, cpu._testGetPcSpAFBCDEHL());
        }
        
        @RepeatedTest(5)
        void CALL_CC_N16worksCorrectlyNZ() {
            cpu.setF(false, false);
            writeAllBytes(Opcode.LD_SP_N16.encoding, 0xFF, 0xFF, Opcode.CALL_NZ_N16.encoding, n8a, n8b);
            cycleCpu(cpu, Opcode.CALL_NZ_N16.cycles + Opcode.CALL_NZ_N16.cycles + Opcode.CALL_NZ_N16.additionalCycles);
            assertArrayEquals(new int[] {n16, 0xFFFF - 2, 0, 0b00000000, 0, 0, 0, 0, 0, 0}, cpu._testGetPcSpAFBCDEHL());
        }
    }
    
    @Test 
    void RST_U3worksCorrectly() {
        writeAllBytes(Opcode.LD_SP_N16.encoding, 0xFF, 0xFF, Opcode.RST_3.encoding);
        cycleCpu(cpu, Opcode.RST_3.cycles);
        assertArrayEquals(new int[] {8 * Bits.extract(Opcode.RST_3.encoding, 3, 3),
            0xFFFF - 2, 0, 0b00000000, 0, 0, 0, 0, 0, 0}, cpu._testGetPcSpAFBCDEHL());
    }
    
    @Test
    void RETworksCorrectly() {
            
        bus.write(0, Opcode.RET.encoding);
        bus.write(201, Opcode.LD_A_N8.encoding);
        bus.write(202, e8);
        cycleCpu(cpu, Opcode.RET.cycles + Opcode.LD_A_N8.cycles);
        assertArrayEquals(new int[] {203,2,e8,0,0,0,0,0,0,0}, cpu._testGetPcSpAFBCDEHL());
    }
    
    @Nested
    class RET_CCworksCorrectly {
        
        @RepeatedTest(5)
        void RET_CCworksCorrectlyC() {
            cpu.setF(false, true);
            bus.write(0, Opcode.RET.encoding);
            bus.write(201, Opcode.LD_A_N8.encoding);
            bus.write(202, e8);
            cycleCpu(cpu, Opcode.RET_C.cycles + Opcode.LD_A_N8.cycles + Opcode.RET_C.additionalCycles);
            assertArrayEquals(new int[] {204,2,e8,0b00010000,0,0,0,0,0,0}, cpu._testGetPcSpAFBCDEHL());
        }
        
        @RepeatedTest(5)
        void RET_CCworksCorrectlyNC() {
            cpu.setF(false, false);
            bus.write(0, Opcode.RET.encoding);
            bus.write(201, Opcode.LD_A_N8.encoding);
            bus.write(202, e8);
            cycleCpu(cpu, Opcode.RET_NC.cycles + Opcode.LD_A_N8.cycles + Opcode.RET_NC.additionalCycles);
            assertArrayEquals(new int[] {204,2,e8,0,0,0,0,0,0,0}, cpu._testGetPcSpAFBCDEHL());
        }
        
        @RepeatedTest(5)
        void RET_CCworksCorrectlyZ() {
            cpu.setF(true, false);
            bus.write(0, Opcode.RET.encoding);
            bus.write(201, Opcode.LD_A_N8.encoding);
            bus.write(202, e8);
            cycleCpu(cpu, Opcode.RET_Z.cycles + Opcode.LD_A_N8.cycles + Opcode.RET_Z.additionalCycles);
            assertArrayEquals(new int[] {204,2,e8,0b10000000,0,0,0,0,0,0}, cpu._testGetPcSpAFBCDEHL());
        }
        
        @RepeatedTest(5)
        void RET_CCworksCorrectlyNZ() {
            cpu.setF(false, false);
            bus.write(0, Opcode.RET.encoding);
            bus.write(201, Opcode.LD_A_N8.encoding);
            bus.write(202, e8);
            cycleCpu(cpu, Opcode.RET_NZ.cycles + Opcode.LD_A_N8.cycles + Opcode.RET_NZ.additionalCycles);
            assertArrayEquals(new int[] {204,2,e8,0,0,0,0,0,0,0}, cpu._testGetPcSpAFBCDEHL());
        }
    }
    
    @Nested
    class EDIworksCorrectly {
        
        @Test
        void EDIworksCorrectlyEI() {
            writeAllBytes(Opcode.EI.encoding);
            cycleCpu(cpu, Opcode.EI.cycles);
            assertTrue(cpu.getIME());
        }
        
        @Test
        void EDIworksCorrectlyDI() {
            writeAllBytes(Opcode.DI.encoding);
            cycleCpu(cpu, Opcode.DI.cycles);
            assertFalse(cpu.getIME());
        }
    }
    
    @Test 
    void RETI() {
        cpu.setIF(2);
        cpu.setIE(2);
        bus.write(0, Opcode.LD_SP_N16.encoding);
        bus.write(1, 0xFF);
        bus.write(2, 0xFF);
        bus.write(3, Opcode.EI.encoding);
        bus.write(4, Opcode.LD_B_N8.encoding);
        bus.write(5, 3);
        bus.write(AddressMap.INTERRUPTS[1], Opcode.LD_A_N8.encoding);
        bus.write(AddressMap.INTERRUPTS[1] + 1, 2);
        bus.write(AddressMap.INTERRUPTS[1] + 2, Opcode.RETI.encoding);
            
        cycleCpu(cpu, Opcode.LD_SP_N16.cycles + Opcode.EI.cycles 
                + Opcode.LD_A_N8.cycles + Opcode.RET.cycles + Opcode.LD_B_N8.cycles + 5);
        assertArrayEquals(new int[] {6,0xffff,2,0,3,0,0,0,0,0}, cpu._testGetPcSpAFBCDEHL());      
    }
    
    @Test
    void HALTworksCorrectly() {
        writeAllBytes(Opcode.HALT.encoding, Opcode.LD_A_N8.encoding, e8);
        cycleCpu(cpu, Opcode.HALT.cycles + Opcode.LD_A_N8.cycles);
        assertArrayEquals(new int[] {1,0,0,0,0,0,0,0,0,0}, cpu._testGetPcSpAFBCDEHL());
    }
    
    @Nested
    class InterruptsWork {
        
        @BeforeEach
        void initInterrupts() {
            cpu.setF(0b00000000);
        }
        
        @Test
        void Interrupt0() {          
            cpu.setIE(1);
            cpu.setIF(1);
            
            bus.write(0, Opcode.LD_SP_N16.encoding);
            bus.write(1, 0xFF);
            bus.write(2, 0xFF);
            bus.write(3, Opcode.EI.encoding);
            bus.write(4, Opcode.LD_A_N8.encoding);
            bus.write(5, 0xFF);
            bus.write(0x40, Opcode.LD_B_N8.encoding);
            bus.write(0x41, 12);
            bus.write(0x42, Opcode.RET.encoding);
                    
            cycleCpu(cpu,Opcode.LD_A_N8.cycles + Opcode.EI.cycles + 5 
                    + Opcode.LD_B_N8.cycles + Opcode.RET.cycles + Opcode.LD_SP_N16.cycles);     
            assertArrayEquals(
                    new int[] {6, 0xFFFF, 0xFF, 0, 12, 0, 0, 0, 0, 0},
                    cpu._testGetPcSpAFBCDEHL());
        }
        
        @Test
        void Interrupt1() {
            cpu.setIE(2);
            cpu.setIF(2);
            
            bus.write(0, Opcode.LD_SP_N16.encoding);
            bus.write(1, 0xFF);
            bus.write(2, 0xFF);
            bus.write(3, Opcode.EI.encoding);
            bus.write(4, Opcode.LD_A_B.encoding);
            bus.write(0x48, Opcode.LD_B_N8.encoding);
            bus.write(0x49, e8);
            bus.write(0x4A, Opcode.RET.encoding);
            
            
            cycleCpu(cpu,Opcode.LD_A_B.cycles + Opcode.EI.cycles + 5 + Opcode.LD_B_N8.cycles 
                    + Opcode.RET.cycles + Opcode.LD_SP_N16.cycles);     
            assertArrayEquals(
                    new int[] {5, 0xFFFF, e8, 0, e8, 0, 0, 0, 0, 0},
                    cpu._testGetPcSpAFBCDEHL());
        }
        
        @Test
        void Interrupt2() {
            cpu.setIE(4);
            cpu.setIF(4);
            
            bus.write(0, Opcode.LD_SP_N16.encoding);
            bus.write(1, 0xFF);
            bus.write(2, 0xFF);
            bus.write(3, Opcode.EI.encoding);
            bus.write(4, Opcode.LD_A_N8.encoding);
            bus.write(5, 0xFF);
            bus.write(0x50, Opcode.LD_B_N8.encoding);
            bus.write(0x51, 12);
            bus.write(0x52, Opcode.RET.encoding);
            
            
            cycleCpu(cpu,Opcode.LD_A_N8.cycles + Opcode.EI.cycles + 5 
                    + Opcode.LD_B_N8.cycles + Opcode.RET.cycles + Opcode.LD_SP_N16.cycles);     
            assertArrayEquals(
                    new int[] {6, 0xFFFF, 0xFF, 0, 12, 0, 0, 0, 0, 0},
                    cpu._testGetPcSpAFBCDEHL());
        }
        
        @Test
        void Interrupt3() {
            cpu.setIE(1);
            cpu.setIF(2);
            
            bus.write(0, Opcode.EI.encoding);
            bus.write(1, Opcode.LD_A_N8.encoding);
            bus.write(2, 0xFF);
            bus.write(0x88, Opcode.LD_B_N8.encoding);
            bus.write(0x89, 12);
            bus.write(0x8A, Opcode.RET.encoding);
            
            
            cycleCpu(cpu,Opcode.LD_A_N8.cycles + Opcode.EI.cycles);     
            assertArrayEquals(
                    new int[] {3, 0, 0xFF, 0, 0, 0, 0, 0, 0, 0},
                    cpu._testGetPcSpAFBCDEHL());
        }
    }   
}

package ch.epfl.gameboj.component;

import java.util.Objects;

import ch.epfl.gameboj.AddressMap;
import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.bits.Bits;
import ch.epfl.gameboj.component.cpu.Cpu;
import ch.epfl.gameboj.component.cpu.Cpu.Interrupt;

public final class JoypadVersion2 implements Component {

    
    private Cpu cpu;
    
    private int P1;
    private int line1;
    private int line2;
    
    public enum Key {RIGHT, LEFT, UP, DOWN, A, B, SELECT, START}; //change to private
    
    //TEST METHOD
    public void setSelection(int b) {       
        switch(b) {
        case 0 : P1 &= 0b11001111; break;
        case 1 : P1 = P1 | (0b01 << 4) ; break;
        case 2 : P1 = P1 | (0b10 << 4); break;
        case 3 : P1 = P1 | (0b11 << 4); break;
        }
    }
    
    public JoypadVersion2(Cpu cpu) {
        
        this.cpu = Objects.requireNonNull(cpu);
        line1 = 0;
        line2 = 0;
        P1 = 0;
        
    }
    
    public void keyPressed(Key k) {
        
        int lineSize = Key.values().length / 2;
        int index = k.ordinal();
        int lastP1 = P1;
         
        if(index < lineSize) line1 = Bits.set(line1, index, true);
        else line2 = Bits.set(line2, index % lineSize, true);
        
        setP1(true);
        
        if(Bits.clip(4, P1) != Bits.clip(4, lastP1)) cpu.requestInterrupt(Interrupt.JOYPAD); 
    }
    
    public void keyReleased(Key k) {
        
        int lineSize = Key.values().length / 2;
        int index = k.ordinal();
        
        if(index < lineSize) line1 = Bits.set(line1, index, false);
        else line2 = Bits.set(line2, index % lineSize, false); 
        
        setP1(false);
    }
    
    
    private void setP1(boolean pressed) {
                
        switch(Bits.extract(P1, 4, 2)) {
        case 0b00 : break;
        case 0b01 : P1 = pressed ? (P1 | line1) : (P1 & (line1 | 0b11110000)) ; break;
        case 0b10 : P1 = pressed ? (P1 | line2) : (P1 & (line2 | 0b11110000)); break;
        case 0b11 : P1 = pressed ? (P1 | (line1 | line2)) : (P1 & ((line1 | line2) | 0b11110000));break;
        }
    }
    
    @Override
    public int read(int address) throws IllegalArgumentException {
        if(Preconditions.checkBits16(address) == AddressMap.REG_P1) return Bits.complement8(P1);
        return NO_DATA;
    }

    @Override
    public void write(int address, int data) throws IllegalArgumentException {
        int lastP1 = P1;
        if(Preconditions.checkBits16(address) == AddressMap.REG_P1) 
            P1 = (lastP1 & 0b0000_1111) | (Bits.complement8(Preconditions.checkBits8(data)) & 0b0011_0000);
    }

}
package ch.epfl.gameboj.component;

import ch.epfl.gameboj.AddressMap;
import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.bits.Bit;
import ch.epfl.gameboj.bits.Bits;
import ch.epfl.gameboj.component.cpu.Cpu;
import ch.epfl.gameboj.component.cpu.Cpu.Interrupt;

public final class Joypad implements Component {

    private final Cpu cpu;

    public int P1 = 0;
    private int line0 = 0;
    private int line1 = 0;

    public enum Key { RIGHT, LEFT, UP, DOWN, A, B, SELECT, START }
    
    public enum KBState implements Bit { COL0, COL1, COL2, COL3, LINE0, LINE1, UNUSED_6, UNUSED_7 }
    
    private static final int LINE_LENGTH = 4;
    
    //TODO why are the lines always inactive despite being written to?

    /**
     * Construit un Joypad.
     * 
     * @param cpu
     * le cpu avec lequel le Joypad interagit
     */
    public Joypad(Cpu cpu) {
        this.cpu = cpu;
    }

    /**
     * Permet de simuler l'appui d'une touche.
     * 
     * @param k
     * la touche appuyée
     */
    public void keyPressed(Key k) {
        int tmp = P1;
        
        if (k.ordinal() < LINE_LENGTH) {
            line0 = Bits.set(line0, k.ordinal(), true);
        } else {
            line1 = Bits.set(line1, k.ordinal() % LINE_LENGTH, true);
        }
        
        System.out.println("key pressed " + k.name());
        System.out.println("line 0 " + Integer.toBinaryString(line0));
        System.out.println("line 1 " + Integer.toBinaryString(line1));
        
        updateP1();
        
        if (Bits.clip(4, P1) < Bits.clip(4, tmp)) {
            cpu.requestInterrupt(Interrupt.JOYPAD);
        }
    }

    /**
     * Permet de simuler l'éliberation d'une touche.
     * 
     * @param k 
     * la touche libérée
     */
    public void keyReleased(Key k) {        
        if (k.ordinal() < LINE_LENGTH) {
            line0 = Bits.set(line0, k.ordinal(), false);
        } else {
            line1 = Bits.set(line1, k.ordinal() % LINE_LENGTH, false);
        }
        
        updateP1();
    }

    private void updateP1() { 
        System.out.println("P1 " + Integer.toBinaryString(P1) + " LINE0 " + Bits.test(P1, KBState.LINE0) + " LINE1 " + Bits.test(P1, KBState.LINE1));
        
        //P1 = 0b0010_0000;
        
        if (Bits.test(P1, KBState.LINE0) && !Bits.test(P1, KBState.LINE1)) {
            P1 = (P1 & 0b1111_0000) | line0;
        } else if (!Bits.test(P1, KBState.LINE0) && Bits.test(P1, KBState.LINE1)) {
            P1 = (P1 & 0b1111_0000) | line1;
        } else if (Bits.test(P1, KBState.LINE0) && Bits.test(P1, KBState.LINE1)) {
            P1 = (P1 & 0b1111_0000) | line0 | line1;
        } else if (!Bits.test(P1, KBState.LINE0) && !Bits.test(P1, KBState.LINE1)) {
            P1 &= 0b1111_0000;
        }
        
        System.out.println("P1 " + Integer.toBinaryString(P1));
    }
    
    @Override
    public int read(int address) throws IllegalArgumentException {
        if (Preconditions.checkBits16(address) == AddressMap.REG_P1) {
            return Bits.complement8(P1);
        }
        
        return NO_DATA;
    }

    @Override
    public void write(int address, int data) throws IllegalArgumentException {
        if (Preconditions.checkBits16(address) == AddressMap.REG_P1) {
            P1 = (P1 & 0b0000_1111) | (Bits.complement8(Preconditions.checkBits8(data)) & 0b0011_0000);
        }
    }
}

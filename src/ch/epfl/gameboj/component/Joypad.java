package ch.epfl.gameboj.component;

import java.util.Objects;

import ch.epfl.gameboj.AddressMap;
import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.bits.Bit;
import ch.epfl.gameboj.bits.Bits;
import ch.epfl.gameboj.component.cpu.Cpu;
import ch.epfl.gameboj.component.cpu.Cpu.Interrupt;

public final class Joypad implements Component {

    private final Cpu cpu;

    private int P1 = 0; //TODO connect P10-15 ports to P1 register
    private int line0 = 0;
    private int line1 = 0;

    public enum Key { RIGHT, LEFT, UP, DOWN, A, B, SELECT, START }
    
    public enum KBState implements Bit { COL0, COL1, COL2, COL3, LINE0, LINE1, UNUSED_6, UNUSED_7 }
    
    private static final int LINE_LENGTH = 4;

    /**
     * Construit un Joypad.
     * 
     * @param cpu
     *            le cpu avec lequel le Joypad interagit
     */
    public Joypad(Cpu cpu) {
        this.cpu = Objects.requireNonNull(cpu);
    }

    /**
     * Permet de simuler l'appui d'une touche.
     * 
     * @param k
     *            la touche appuyée
     */
    public void keyPressed(Key k) {
        cpu.requestInterrupt(Interrupt.JOYPAD);
        
        //System.out.println("p1 before " + Integer.toBinaryString(P1));
        
        if (k.ordinal() < LINE_LENGTH) {
            line0 = Bits.set(line0, k.ordinal(), true);
        } else {
            line1 = Bits.set(line1, k.ordinal() % LINE_LENGTH, true);
        }
        
        updateP1();
        
        //System.out.println("p1 after " + Integer.toBinaryString(P1));
    }

    /**
     * Permet de simuler l'�liberation d'une touche.
     * 
     * @param k
     *            la touche lib�r�e
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
        P1 &= 0b1111_0000;
        if (Bits.test(P1, KBState.LINE0)) { 
            P1 |= line0;
        }
        
        if (Bits.test(P1, KBState.LINE1)) {
            P1 |= line1;
        }
    }
       
    @Override
    public int read(int address) {  
        /*if (address == AddressMap.REG_P1) {
            System.out.println(Integer.toBinaryString(Bits.complement8(P1)));
            if(Bits.test(P1, KBState.COL0)) {
                System.out.println("A pressed!");
            }
        }*/
        return Preconditions.checkBits16(address) == AddressMap.REG_P1 ? Bits.complement8(P1) : NO_DATA;
    }

    @Override
    public void write(int address, int data) {
        if (Preconditions.checkBits16(address) == AddressMap.REG_P1) {
            P1 = (P1 & 0b1100_1111) | (Bits.complement8(Preconditions.checkBits8(data)) & 0b0011_0000);        
        }
    }
}

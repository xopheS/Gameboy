package ch.epfl.gameboj.component;

import java.util.Objects;

import ch.epfl.gameboj.AddressMap;
import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.bits.Bit;
import ch.epfl.gameboj.bits.Bits;
import ch.epfl.gameboj.component.cpu.Cpu;
import ch.epfl.gameboj.component.cpu.Cpu.Interrupt;

/**
 * Cette classe représente le clavier de la gameboj.
 *
 * @author Christophe Saad (282557)
 * @author David Cian (287967)
 *
 */
public final class Joypad implements Component {

    private final Cpu cpu;

    private int P1 = 0; 
    private int line0 = 0;
    private int line1 = 0;

    
    /**
     * Touches du clavier
     * @author Christophe Saad (282557)
     * @author David Cian (287967)
     *
     */
    public enum Key { RIGHT, LEFT, UP, DOWN, A, B, SELECT, START }
    
    /**
     * Etat du clavier
     * @author Christophe Saad (282557)
     * @author David Cian (287967)
     *
     */
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
        if (k.ordinal() < LINE_LENGTH) {
            line0 = Bits.set(line0, k.ordinal(), true);
        } else {
            line1 = Bits.set(line1, k.ordinal() % LINE_LENGTH, true);
        }
    }

    /**
     * Permet de simuler l'élibération d'une touche.
     * 
     * @param k
     *            la touche libérée
     */
    public void keyReleased(Key k) {        
        if (k.ordinal() < LINE_LENGTH) {
            line0 = Bits.set(line0, k.ordinal(), false);
        } else {
            line1 = Bits.set(line1, k.ordinal() % LINE_LENGTH, false);
        }
    }

    private void computeP1() { 
        int tmp = P1; //TODO
    	
        P1 &= 0b1111_0000;
        
        if (Bits.test(P1, KBState.LINE0)) { 
            P1 |= line0;
        }
        
        if (Bits.test(P1, KBState.LINE1)) {
            P1 |= line1;
        }
        
        if (P1 > tmp) cpu.requestInterrupt(Interrupt.JOYPAD); //TODO
    }
       
    @Override
    public int read(int address) {      	
    	computeP1();
    	
        return Preconditions.checkBits16(address) == AddressMap.REG_P1 ? Bits.complement8(P1) : NO_DATA;
    }

    @Override
    public void write(int address, int data) {
        if (Preconditions.checkBits16(address) == AddressMap.REG_P1) {
            P1 = (P1 & 0b1100_1111) | (Bits.complement8(Preconditions.checkBits8(data)) & 0b0011_0000);
        }
    }
}

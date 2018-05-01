package ch.epfl.gameboj.component;

import ch.epfl.gameboj.AddressMap;
import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.bits.Bit;
import ch.epfl.gameboj.bits.Bits;
import ch.epfl.gameboj.component.cpu.Cpu;
import ch.epfl.gameboj.component.cpu.Cpu.Interrupt;

public final class Joypad implements Component {

    private final Cpu cpu;

    private int P1 = 0;
    private int line1 = 0;
    private int line2 = 0;

    public enum Key { RIGHT, LEFT, UP, DOWN, A, B, SELECT, START }
    
    public enum KBState implements Bit { COL0, COL1, COL2, COL3, LINE0, LINE1, UNUSED_6, UNUSED_7 }

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
     * a faire.
     * 
     * @param k
     * ?
     */
    public void keyPressed(Key k) {

        int lineSize = Key.values().length / 2;
        int index = k.ordinal();
        int lastP1 = P1;

        if (index < lineSize) {
            Bits.set(line1, index, true);
        } else {
            Bits.set(line2, index % lineSize, true);
        }

        switch (Bits.extract(P1, 4, 2)) {
            case 0b00:
                break;
            case 0b01:
                P1 |= line1;
                break;
            case 0b10:
                P1 |= line2;
                break;
            case 0b11:
                P1 |= (line1 | line2);
                break;
            default:
                break;
        }

        if (Bits.clip(4, P1) != Bits.clip(4, lastP1)) {
            cpu.requestInterrupt(Interrupt.JOYPAD);
        }
    }

    /**
     * a faire.
     * @param k ?
     */
    public void keyReleased(Key k) {

        int lineSize = Key.values().length / 2;
        int index = k.ordinal();
        int lastP1 = P1;

        if (index < lineSize) {
            Bits.set(line1, index, false);
        } else {
            Bits.set(line2, index % lineSize, false);
        }

        switch (Bits.extract(P1, 4, 2)) {
            case 0b00:
                break;
            case 0b01:
                P1 &= line1;
                break;
            case 0b10:
                P1 &= line2;
                break;
            case 0b11:
                P1 &= line1 | line2;
                break;
            default:
                break;
        }

        if (Bits.clip(4, P1) != Bits.clip(4, lastP1)) {
            cpu.requestInterrupt(Interrupt.JOYPAD);
        }
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
            P1 = (P1 & 0000_1111) | (Bits.complement8(Preconditions.checkBits8(data)) & 0011_0000);
        }
    }
}

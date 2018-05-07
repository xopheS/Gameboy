package ch.epfl.gameboj.component;

import java.util.Objects;

import ch.epfl.gameboj.AddressMap;
import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.bits.Bit;
import ch.epfl.gameboj.bits.Bits;
import ch.epfl.gameboj.component.cpu.Cpu;
import ch.epfl.gameboj.component.cpu.Cpu.Interrupt;

/**
 * Cette classe modélise le timer de la Gameboy, qui possède et actualise deux
 * compteurs, un principal et un secondaire.
 *
 * @author Christophe Saad (282557)
 * @author David Cian (287967)
 *
 */
public final class Timer implements Component, Clocked {
    private static final int maxSecondaryCounter = 0xFF, unitsPerCycle = 4;

    private final Cpu cpu;
    private int regDIV = 0, regTIMA = 0, regTMA = 0, regTAC = 0;
    
    //00: 1024, 01: 16, 10: 64, 11: 256
    private enum TAC implements Bit { CLK_SEL_0, CLK_SEL_1, TM_ENABLE }

    /**
     * Constructeur qui initialise un timer en spécifiant le processeur avec lequel
     * il va interagir.
     *
     * @param cpu
     *            Le processeur auquel le timer va solliciter les interruptions
     *
     * @throws NullPointerException
     *             si le Cpu fourni est null
     */
    public Timer(Cpu cpu) {
        this.cpu = Objects.requireNonNull(cpu, "The provided cpu cannot be null.");
    }

    /**
     * Cette méthode actualise les compteurs.
     *
     * @param cycle
     *            Le cycle actuel de la Gameboy
     */
    @Override
    public void cycle(long cycle) {
        change(() -> regDIV = Bits.clip(16, regDIV + unitsPerCycle));
    }

    /**
     * Cette méthode permet d'accéder aux registres du timer (qui stockent notamment
     * la valeur des compteurs) par le bus.
     *
     * @param address
     *            L'adresse de lecture
     *
     * @return la valeur du registre en question
     *
     * @throws IllegalArgumentException
     *             si l'adresse n'est pas sur 16 bits
     */
    @Override
    public int read(int address) {
        switch (Preconditions.checkBits16(address)) {
            case AddressMap.REG_DIV:
                return Bits.extract(regDIV, 8, 8);
            case AddressMap.REG_TIMA:
                return regTIMA;
            case AddressMap.REG_TMA:
                return regTMA;
            case AddressMap.REG_TAC:
                return regTAC;
            default:
                return NO_DATA;
        }
    }

    /**
     * Cette méthode permet d'écrire dans les registres du timer en tenant compte de
     * l'incrémentation des compteurs qui peut en découler.
     *
     * @param address
     *            L'adresse d'écriture
     *
     * @param data
     *            Les données à écrire
     *
     * @throws IllegalArgumentException
     *             si l'adresse n'est pas sur 16 bits ou les données ne sont pas sur
     *             8 bits
     */
    @Override
    public void write(int address, int data) {
        Preconditions.checkBits8(data);
        switch (Preconditions.checkBits16(address)) {
            case AddressMap.REG_DIV:
                change(() -> {
                    regDIV = 0;
                });
                break;
            case AddressMap.REG_TIMA:
                regTIMA = data;
                break;
            case AddressMap.REG_TMA:
                regTMA = data;
                break;
            case AddressMap.REG_TAC:
                change(() -> regTAC = data);
                break;
            default:
                break;
        }
    }

    private boolean state() {
        int divBitIndex = 0;

        switch (Bits.clip(2, regTAC)) {
            case 0:
                divBitIndex = 9;
                break;
            case 1:
                divBitIndex = 3;
                break;
            case 2:
                divBitIndex = 5;
                break;
            case 3:
                divBitIndex = 7;
                break;
            default:
                break;
        }

        return Bits.test(regTAC, 2) && Bits.test(regDIV, divBitIndex);
    }

    private void change(Runnable r) {
        boolean s0 = state();
        r.run();
        incIfChange(s0);
    }

    private void incIfChange(boolean previousState) {
        if (previousState && !state()) {
            if (regTIMA == maxSecondaryCounter) {
                cpu.requestInterrupt(Interrupt.TIMER);
                regTIMA = regTMA;
            } else {
                regTIMA++;
            }
        }
    }
}

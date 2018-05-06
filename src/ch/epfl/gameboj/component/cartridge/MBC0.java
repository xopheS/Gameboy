package ch.epfl.gameboj.component.cartridge;

import java.util.Objects;

import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.component.Component;
import ch.epfl.gameboj.component.memory.Rom;

/**
 * Cette classe simule une cartouche de type MBC0, le type le plus basique.
 *
 * @author Christophe Saad (282557)
 * @author David Cian (287967)
 *
 */
public class MBC0 implements Component {

    private final Rom rom;
    private final static int MBC0_SIZE = 32768;

    /**
     * Ceci est le constructeur publique de MBC0, qui effectue l'empaquetage d'une
     * ROM dans une cartouche de type MBC0.
     *
     * @param rom
     *            La ROM (read-only memory) utilisée comme base du MBC0
     *
     * @throws IllegalArgumentException
     *             si la ROM fournie a une capacité différente de 32768, celle d'un
     *             MBC0 dans la réalité
     *
     * @throws NullPointerException
     *             si la ROM fournie est null
     */
    public MBC0(Rom rom) {
        if (rom.size() != MBC0_SIZE) {
            throw new IllegalArgumentException("The provided ROM must have a capacity of 32768.");
        }

        this.rom = Objects.requireNonNull(rom, "The provided ROM cannot be null.");
    }

    /*
     * (non-Javadoc)
     * 
     * @see ch.epfl.gameboj.component.Component#read(int)
     */
    @Override
    public int read(int address) {
        if (Preconditions.checkBits16(address) >= 0 && address < MBC0_SIZE) {
            return rom.read(address);
        }
        return NO_DATA;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ch.epfl.gameboj.component.Component#write(int, int)
     */
    @Override
    public void write(int address, int data) {
        throw new UnsupportedOperationException("Cannot write to a ROM");
    }
}

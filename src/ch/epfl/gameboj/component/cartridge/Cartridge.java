package ch.epfl.gameboj.component.cartridge;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Objects;

import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.component.Component;
import ch.epfl.gameboj.component.memory.Rom;

/**
 * Cette classe simule une cartouche de Gameboy.
 *
 * @author Christophe Saad (282557)
 * @author David Cian (287967)
 *
 */
public final class Cartridge implements Component {

    private final Component mbc;

    private Cartridge(Component mbc) {
        this.mbc = Objects.requireNonNull(mbc);
    }

    /**
     * Ceci est le constructeur public de Cartridge, il y met dedans les contenus
     * d'un fichier ROM.
     *
     * @param romFile
     *            Le fichier ROM (read-only memory) dont les contenus sont copiés
     * @return nouvelle cartouche avec les contenus du fichier
     * @throws FileNotFoundException
     *             si le fichier n'est pas trouvé
     * @throws IOException
     *             si un problème de lecture intervient
     */
    public static Cartridge ofFile(File romFile) throws FileNotFoundException, IOException {

        try (FileInputStream fis = new FileInputStream(romFile)) {
            byte[] fileBytes = fis.readAllBytes();

            if (fileBytes[0x147] != 0) {
                throw new IllegalArgumentException("The byte at index 0x147 in the file must be equal to zero.");
            }

            return new Cartridge(new MBC0(new Rom(fileBytes)));
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ch.epfl.gameboj.component.Component#read(int)
     */
    @Override
    public int read(int address) {
        return mbc.read(Preconditions.checkBits16(address));
    }

    /*
     * (non-Javadoc)
     * 
     * @see ch.epfl.gameboj.component.Component#write(int, int)
     */
    @Override
    public void write(int address, int data) {
        mbc.write(Preconditions.checkBits16(address), Preconditions.checkBits8(data));
    }

}

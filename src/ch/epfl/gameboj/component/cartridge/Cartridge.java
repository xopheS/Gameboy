package ch.epfl.gameboj.component.cartridge;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import ch.epfl.gameboj.AddressMap;
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
    
    private enum CartridgeType { MBC0, MBC1, MBC1_RAM, MBC1_RAM_BAT, MBC_2, MBC2_BAT, ROM_RAM, ROM_RAM_BAT, MM01, MM01_RAM, MM01_RAM_BAT, MBC3_TIM_BAT,
        MBC3_TIM_RAM_BAT, MBC3, MBC3_RAM, MBC3_RAM_BAT, MBC5, MBC5_RAM, MBC5_RAM_BAT, MBC5_RUMBLE, MBC5_RUMBLE_RAM, MBC5_RUMBLE_RAM_BAT, MBC6, MBC7_ALL 
    } //TODO modify this for production?

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

            /*if (fileBytes[AddressMap.CARTRIDGE_TYPE] != cartridgeTypeNum.get(CartridgeType.MBC0)) {
                throw new IllegalArgumentException("At the moment only MBC0 cartridges are supported");
            }*/
            
            switch (fileBytes[AddressMap.CARTRIDGE_TYPE]) {
            case 0:
                return new Cartridge(new MBC1(new Rom(fileBytes), 0));
            case 1:
                return new Cartridge(new MBC1(new Rom(fileBytes), 2048));
            case 2:
                return new Cartridge(new MBC1(new Rom(fileBytes), 8192));
            case 3:
                return new Cartridge(new MBC1(new Rom(fileBytes), 32768));
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

package ch.epfl.gameboj.component.cartridge;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
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
    private static final int[] RAM_SIZES = new int[] { 0, 2048, 8192, 32768 };

    private final MBC mbc;

    private Cartridge(MBC mbc) {
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

            switch (fileBytes[AddressMap.CARTRIDGE_TYPE]) {
            case 0:
                return new Cartridge(new MBC0(new Rom(fileBytes)));
            case 1:
                return new Cartridge(new MBC1(new Rom(fileBytes), RAM_SIZES[fileBytes[AddressMap.RAM_SIZE]]));
            case 2:
                return new Cartridge(new MBC1(new Rom(fileBytes), RAM_SIZES[fileBytes[AddressMap.RAM_SIZE]]));
            case 3:
                return new Cartridge(new MBC1(new Rom(fileBytes), RAM_SIZES[fileBytes[AddressMap.RAM_SIZE]]));
            }
            
            throw new IllegalArgumentException("Cartridge file is not valid");
        }
    }
    
    public void toFile(String fileName) {
    	try (FileOutputStream fileOutputStream = new FileOutputStream(fileName)) {
			fileOutputStream.write(mbc.getByteArray());
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
    
    public void setMBCRam(byte[] byteArray) {
    	mbc.setByteArray(byteArray);
    }
}

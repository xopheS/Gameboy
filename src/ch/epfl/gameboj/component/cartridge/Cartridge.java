package ch.epfl.gameboj.component.cartridge;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import ch.epfl.gameboj.component.Component;
import ch.epfl.gameboj.component.memory.Rom;

/**Cette classe simule une cartouche de Gameboy
 * 
 * @author Cristophe Saad (282557)
 * @author David Cian (287967)
 *
 */
public final class Cartridge implements Component {

    private final Component mbc;
    
    private Cartridge(Component mbc) {
        this.mbc = mbc;
    }
    
    /**Ceci est le constructeur public de Cartridge, il y met dedans les contenus d'un fichier ROM
     * 
     * @param romFile
     * Le fichier ROM (read-only memory) dont les contenus sont copiés
     * 
     * @throws IOException
     * si un problème de lecture intervient
     */
    public static Cartridge ofFile(File romFile) { 
    	
    	try(FileInputStream fis = new FileInputStream(romFile)) {
            byte[] fileBytes = fis.readAllBytes();
            
            if(fileBytes[0x147] != 0) {
                throw new IllegalArgumentException("The byte at index 0x147 in the file must be equal to zero.");
            }
            
            return new Cartridge(new MBC0(new Rom(fileBytes)));
        }
        catch(IOException e) {
            System.err.println(e.getMessage());
        }
    	
    	return null;
    }
    
    @Override
    public int read(int address) {
        return mbc.read(address);
    }

    @Override
    public void write(int address, int data) {
        mbc.write(address, data);      
    }

}

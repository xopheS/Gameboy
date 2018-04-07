package ch.epfl.gameboj.component.cartridge;

import java.util.Objects;

import ch.epfl.gameboj.component.Component;
import ch.epfl.gameboj.component.memory.Rom;

/**Cette classe simule une cartouche de type MBC0, le type le plus basique
 * 
 * @author Cristophe Saad (282557)
 * @author David Cian (287967)
 *
 */
public class MBC0 implements Component {

    private final Rom rom;
    
    /**Ceci est le constructeur publique de MBC0, qui effectue l'empaquetage d'une ROM dans une cartouche de type MBC0
     * 
     * @param rom
     * La ROM (read-only memory) utilisée comme base du MBC0
     * 
     * @throws IllegalArgumentException
     * si la ROM fournie a une capacité différente de 32768, celle d'un MBC0 dans la réalité
     * 
     * @throws NullPointerException
     * si la ROM fournie est null
     */
    public MBC0(Rom rom) {
        if(rom.size() != 32768) {
            throw new IllegalArgumentException("The provided ROM must have a capacity of 32768.");
        }
        
        this.rom = Objects.requireNonNull(rom, "The provided ROM cannot be null.");
    }
    
    @Override
    public int read(int address) {
        if(address < 0 || address >= 32768) {
            return NO_DATA;
        }
        return rom.read(address);
    }

    @Override
    public void write(int address, int data) {
        //Impossible to alter a ROM
    }  
}

package ch.epfl.gameboj.component.memory;

import java.util.Objects;

import ch.epfl.gameboj.AddressMap;
import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.component.Component;
import ch.epfl.gameboj.component.cartridge.Cartridge;

/**
 * Cette classe modélise l'équivalent d'un {@link RamController} pour une
 * mémoire de démarrage (Boot ROM), cela permet notamment à la Gameboy
 * d'effectuer des actions lors du démarrage, avant l'exécution du programme de
 * la cartouche, comme par exemple:
 * <ul>
 * <li>d'initialiser les composants du système</li>
 * <li>d'afficher une animation Nintendo</li>
 * </ul>
 * 
 * @author Christophe Saad (282557)
 * @author David Cian (287967)
 *
 */
public final class BootRomController implements Component {
    
    private final Cartridge cartridge;
    private boolean isActivated = true;

    /**
     * Ce constructeur construit un contrôleur de mémoire de démarrage à partir
     * d'une cartouche
     * 
     * @param cartridge
     *            La cartouche à utiliser
     * 
     * @throws NullPointerException
     *             si la cartouche fournie est null
     */
    public BootRomController(Cartridge cartridge) {        
        this.cartridge = Objects.requireNonNull(cartridge, "The cartridge cannot be null.");
    }
    
    /**
     * Cette méthode lit dans la mémoire de démarrage ou dans la cartouche, si
     * celle-ci est désactivée
     * 
     * @param address
     *            L'adresse à laquelle il faut lire
     * 
     * @return la valeur stockée à l'adresse de lecture dans la mémoire de
     *         démarrage ou dans la cartouche
     * 
     * @throws IllegalArgumentException
     *             si l'adresse n'est pas une valeur sur 16 bits
     */
    @Override
    public int read(int address) {
        if((Preconditions.checkBits16(address) >= AddressMap.BOOT_ROM_START) && address < AddressMap.BOOT_ROM_END && isActivated) {
            return Byte.toUnsignedInt(BootRom.DATA[address]);
        }
        return cartridge.read(address);    
    }

    /**
     * Cette méthode écrit dans la cartouche, si la mémoire de démarrage est
     * désactivée
     * 
     * @param address
     *            L'addresse d'écriture
     * 
     * @param data
     *            Les données à y écrire
     */
    @Override
    public void write(int address, int data) {
        if(address == AddressMap.REG_BOOT_ROM_DISABLE) {
            isActivated = false;
        }
        else {
            cartridge.write(address, data);
        }
    }

}

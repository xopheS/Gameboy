package ch.epfl.gameboj.component.memory;

import ch.epfl.gameboj.component.Component;

import java.util.Objects;

import ch.epfl.gameboj.Preconditions;

/**
 * RamController : une mémoire vive dont le contenu peut changer au cours du temps
 *
 * @author Christophe Saad (282557)
 * @author David Cian (287967)
 */

public final class RamController implements Component {

    private final Ram ram;
    private int startAddress;
    private int endAddress;
    
    /**
     * Construit un contrôleur pour la mémoire vive donnée en argument,
     * accessible entre deux adresses données
     * 
     * @param ram
     *            mémoire vive
     * @param startAddress
     *            adresse de début
     * @param endAddress
     *            adresse de fin
     * @throws NullPointerException
     *             si la mémoire vive est nulle
     * @throws IllegalArgumentException
     *             si les adresses ne sont pas des valeurs de 16 bits
     *             si l'intervalle qu'elles décrivent a une taille négative ou
     *             supérieure à celle de la mémoire
     *             
     */
    public RamController(Ram ram, int startAddress, int endAddress) {       
        Preconditions.checkArgument(endAddress - startAddress >= 0 && endAddress - startAddress <= ram.size());
        
        this.startAddress = Preconditions.checkBits16(startAddress);
        this.endAddress = Preconditions.checkBits16(endAddress);
        this.ram = Objects.requireNonNull(ram);      
    }
    
    
    /**
     * Appelle le premier constructeur en lui passant une adresse de fin telle
     * que la totalité de la mémoire vive soit accessible au travers du
     * contrôleur (l'adresse de fin est l'adresse de début + la taille de la
     * mémoire vive)
     * 
     * @param ram
     *            mémoire vive
     * @param startAddress
     *            adresse de début
     * @throws NullPointerException
     *             si la mémoire vive est nulle
     * @throws IllegalArgumentException
     *             si les adresses ne sont pas des valeurs de 16 bits
     *             si l'intervalle qu'elles décrivent a une taille négative ou
     *             supérieure à celle de la mémoire
     *            
     */
    public RamController(Ram ram, int startAddress) {      
        this(ram, startAddress, startAddress + ram.size());
    }
    
    
    /**
     * Retourne l'octet se trouvant à l'adresse donnée dans la mémoire vive sous
     * la forme d'un entier ou NO_DATA (256) si l'adresse n'est pas dans
     * l'intervalle de contrôle du RamController
     * 
     * @param address l'adresse
     * @throws IllegalArgumentException
     *             si l'adresse n'est pas une valeur de 16 bits
     */
    @Override
    public int read(final int address) {
        if (Preconditions.checkBits16(address) < startAddress || address >= endAddress) {
            return NO_DATA;
        }
        return ram.read(address - startAddress);
    }

    /**
     * Modifie le contenu de la mémoire vive à l'adresse donnée pour qu'il soit
     * égal à la valeur donnée si celle-ci se trouve dans l'intervalle de
     * contrôle du RamController
     * 
     * @param address
     *            l'adresse
     * @param data
     *            la valeur
     * @throws IllegalArgumentException
     *             si l'adresse n'est pas une valeur de 16 bits
     *             si la valeur n'est pas une valeur de 8 bits
     *             
     */
    @Override
    public void write(final int address, final int data) {
        if (Preconditions.checkBits16(address) >= startAddress && address < endAddress) {
            ram.write(address - startAddress, Preconditions.checkBits8(data));
        }  
    }

}

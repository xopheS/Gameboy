package ch.epfl.gameboj.component.memory;
import ch.epfl.gameboj.Preconditions;

/**
 * Ram : une mémoire vive dont le contenu peut changer au cours du temps
 *
 * @author Christophe Saad (282557)
 * @author David Cian (287967)
 */

public final class Ram {

    private final byte[] data;
    
    /**
     * Constructeur qui construit une mémoire vive (tableau de byte) de taille
     * l'argument
     * 
     * @param size
     *            taille du tableau qui constitue la mémoire vive
     * @throws IllegalArgumentException
     *             si l'argument est négatif
     */
    public Ram(int size) {        
        Preconditions.checkArgument(size >= 0);
        data = new byte[size];
    }
    
    /**
     * Retourne la taille en octets de la mémoire (taille du tableau de byte)
     * 
     * @return data.lenghth la taille de la mémoire
     */
    public int size() {
        return data.length;
    }
    
    /**
     * Retourne l'octet se trouvant à l'index donné, sous la forme d'une valeur
     * comprise entre 0 et 0xFF
     * 
     * @param index
     * l'index de la donnée à lire
     *
     * @return entier entre 0 et 0xFF
     * @throws IndexOutOfBoundsException
     *             si l'index est invalide (négatif ou supérieur à la taille du
     *             tableau - 1)
     */
    public int read(final int index) {
        return Byte.toUnsignedInt(data[index]);
    }
    
    
    
    /**
     * Modifie le contenu de la mémoire à l'index donné pour qu'il soit égal à
     * la valeur donnée
     * 
     * @param index
     *            index de l'élément à modifier
     * @param value
     *            valeur à placer dans le tableau
     * @throws IndexOutOfBoundsException
     *             si l'index est invalide (négatif ou supérieur à la taille du
     *             tableau - 1)
     * @throws IllegalArgumentException
     *             si la valeur n'est pas un byte (une valeur de 8 bits)
     */
    public void write(int index, int value) {  
        data[index] = (byte) Preconditions.checkBits8(value);
    }
    
}

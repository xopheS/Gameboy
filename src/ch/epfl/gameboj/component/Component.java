package ch.epfl.gameboj.component;

import ch.epfl.gameboj.Bus;

/**
 * Component : un composant de la Gameboy.
 *
 * @author Christophe Saad (282557)
 * @author David Cian (287967)
 */

public interface Component {

    int NO_DATA = 256;
    
    
    /**
     * Retourne l'octet stocké à l'adresse donnée par le composant, ou NO_DATA
     * si le composant ne possède aucune valeur à cette adresse. 
     * 
     * @param address
     *            l'adresse
     * @return l'octet se trouvant à l'adresse donnée sous forme d'un entier ou
     *         NO_DATA (256)
     */
    int read(int address);
    
    /**
     * Stocke la valeur donnée à l'adresse donnée dans le composant, ou ne fait
     * rien si le composant ne permet pas de stocker de valeur à cette adresse.
     * 
     * @param address
     *            l'adresse
     * @param data
     *            la valeur à stocker
     */
    void write(int address, int data);
    
    
    
    /**
     * Attache le composant au bus donné.
     * 
     * @param bus
     *            le bus
     */
    default void attachTo(Bus bus) {
        bus.attach(this);
    }
}

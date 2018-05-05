package ch.epfl.gameboj;

import java.util.ArrayList;
import java.util.Objects;

import ch.epfl.gameboj.component.Component;

/**
 * Bus : relie les éléments les uns aux autres et leur permet de communiquer
 * entre eux.
 *
 * @author Christophe Saad (282557)
 * @author David Cian (287967)
 */

public final class Bus {
    private final ArrayList<Component> attachedComponents = new ArrayList<Component>();
       
    /**
     * Attache un composant au bus (en le rajoutant au tableau des composants).
     * 
     * @param component
     *            le composant
     * @throws NullPointerException
     *             si le composant vaut null
     */
    public void attach(Component component) {
        attachedComponents.add(Objects.requireNonNull(component, "The component to be attached cannot be null."));
    }
    
    /**
     * Retourne la valeur stockée à l'adresse donnée si au moins un des
     * composants attaché au bus possède une valeur à cette adresse ou 0xFF
     * sinon.
     * 
     * @param address
     *            l'adresse
     * @return La valeur stockée ou 0xFF
     * @throws IllegalArgumentException
     *             si l'adresse n'est pas une valeur 16 bits
     */
    public int read(int address) {   
        Preconditions.checkBits16(address);
        for (Component c : attachedComponents) {
            if (c.read(address) != Component.NO_DATA) {
                return c.read(address);
            }
        }
        
        return 255;
    }
    
    
    /**
     * Ecrit la valeur à l'adresse donnée dans tous les composants connectés au
     * bus.
     * 
     * @param address
     *            l'adresse
     * @param data
     *            la donnée
     * @throws IllegalArgumentException
     *             si l'adresse n'est pas une valeur 16 bits ou si la donnée
     *             n'est pas une valeur 8 bits
     */
    public void write(int address, int data) {
        Preconditions.checkBits16(address);
        Preconditions.checkBits8(data);
        for (Component c : attachedComponents) {
            c.write(address, data);
        }
    }
}

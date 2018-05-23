package ch.epfl.gameboj.component;

/**
 * Cette interface désigne les composantes synchrones (i.e. qui dépendent d'une horloge)
 * 
 * @author Christophe Saad (282557)
 * @author David Cian (287967)
 *
 */
public interface Clocked {

    
  /**
   * Cette méthode décrit le fonctionnement du composant
   * 
   * @param cycle
   *          la valeur du cycle actuel
   */
    void cycle(long cycle);
}

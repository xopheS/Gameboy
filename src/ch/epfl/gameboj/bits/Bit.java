package ch.epfl.gameboj.bits;

/**
 * Interface Bit : a pour but d'être implémentée par les types énumérés.
 * représentant un ensemble de bits
 *
 * @author Christophe Saad (282557)
 * @author David Cian (287967)
 */

public interface Bit {

    /**
     *
     * @return index
     */
    int ordinal();

    /**
     * Retourne la même valeur que la méthode ordinal du type énumération.
     * (retourne le numéro d'ordre d'un élément énuméré)
     *
     * @return méthode ordinal du type énumération
     */
    default int index() {
        return ordinal();
    }

    /**
     * Retourne une valeur dont seul le bit de même index que celui du récepteur
     * vaut 1.
     *
     * @return le masque correspondant au bit de position l'index
     */
    default int mask() {
        return 1 << index();
    }
}

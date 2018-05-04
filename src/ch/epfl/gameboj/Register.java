package ch.epfl.gameboj;

/**
 * Cette classe modélise un registre individuel, i.e. un espace de stockage pour
 * une seule valeur
 * 
 * @author Christophe Saad (282557)
 * @author David Cian (287967)
 *
 */
public interface Register {
    
    /**
     * Index d'un registre.
     * 
     * @return l'index du registre
     */
    int ordinal();

    /**
     * Nom alternatif pour {@link #ordinal}.
     * 
     * @return l'index du registre
     */
    default int index() {
        return ordinal();
    }
}

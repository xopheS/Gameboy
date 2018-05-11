package ch.epfl.gameboj;

/**
 * Preconditions : contient des fonctions qui permettent de vérifier que leurs
 * arguments satisfassent certaines conditions.
 *
 * @author Christophe Saad (282557)
 * @author David Cian (287967)
 */

public interface Preconditions {
    int MAX_DATA_VALUE = 255, MAX_ADDRESS_VALUE = 65535;

    /**
     * Check la validité de l'expression booléenne donnée en argument.
     *
     * @param b
     *            expression booléenne
     * 
     * @throws IllegalArgumentException
     *             si l'argument est faux
     * 
     * @see #checkArgument(boolean, String)
     */
    static void checkArgument(boolean b) {
        if (!b) {
            throw new IllegalArgumentException();
        }
    }

    /**
     * Vérifie la validité de l'expression booléenne donnée en argument, et affiche
     * le message d'erreur en cas d'exception.
     *
     * @param b
     *            L'expression booléenne à tester
     *
     * @param msg
     *            Le message à afficher en cas d'exception
     *
     * @throws IllegalArgumentException
     *             Si l'évaluation de b retourne false
     */
    static void checkArgument(boolean b, String msg) {
        if (!b) {
            throw new IllegalArgumentException(msg);
        }
    }
    
    /**
     * Vérifie la validité de l'expression booléenne donnée en argument, et exécute l'action spécifiée sinon.
     *
     * @param b
     *            L'expression booléenne à tester
     *
     * @param msg
     *            Le message à afficher en cas d'exception
     *
     * @throws IllegalArgumentException
     *             Si l'évaluation de b retourne false
     */
    static void checkArgument(boolean b, Runnable r) {
        if (!b) {
            r.run();
        }
    }

    /**
     * Retourne son argument si celui-ci est compris entre 0 et 0xFF inclus (valeur
     * de 8 bits).
     *
     * @param v
     *            entier à vérifier
     * @return entier v
     * @throws IllegalArgumentException
     *             si v n'est pas compris entre 0 et 0xFF (inclus)
     */
    static int checkBits8(int v) {
        checkArgument(v >= 0 && v <= MAX_DATA_VALUE);
        return v;
    }

    /**
     * Retourne son argument si celui-ci est compris entre 0 et 0xFFFF inclus
     * (valeur de 16 bits).
     *
     * @param v
     *            entier à vérifier
     * @return entier v
     * @throws IllegalArgumentException
     *             si v n'est pas compris entre 0 et 0xFF (inclus)
     */
    static int checkBits16(int v) {
        checkArgument(v >= 0 && v <= MAX_ADDRESS_VALUE);
        return v;
    }
}

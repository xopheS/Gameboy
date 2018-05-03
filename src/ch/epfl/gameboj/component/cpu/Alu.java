package ch.epfl.gameboj.component.cpu;

import java.util.Objects;

import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.bits.Bit;
import ch.epfl.gameboj.bits.Bits;

/**
 * Cette classe simule l'unité logique et arithmétique (UAL ou ALU en anglais)
 * du processeur de la Gameboy.
 *
 * @author Christophe Saad (282557)
 * @author David Cian (287967)
 *
 */
public final class Alu {
    private static final int valueStart = 8;
    private static final int valueBits = 16;

    /**
     * Cette énumération représente les bits dans l'ordre (du LSB au MSB) de l'octet
     * qui stocke les fanions dans un entier: Z est vrai si le résultat d'une
     * opération est nul, N est vrai si l'opération effectuée est une soustraction,
     * H est vrai si l'opération produit un carry pour les 4 premiers bits (LSBs), C
     * est vrai si l'opération produit un carry.
     */
    public enum Flag implements Bit {
        UNUSED_0, UNUSED_1, UNUSED_2, UNUSED_3, C, H, N, Z
    };

    /**
     * Cette énumération représente les deux directions possibles de rotation,
     * gauche et droite.
     *
     */
    public enum RotDir {
        LEFT, RIGHT
    };

    private Alu() {

    }

    private static int packValueZNHC(int v, boolean Z, boolean N, boolean H, boolean C) {
        return (v << valueStart) | maskZNHC(Z, N, H, C);
    }

    /**
     * Cette méthode met les fanions stockés dans un entier aux valeurs données.
     *
     * @param Z
     *            La valeur à donner au fanion Z
     *
     * @param N
     *            La valeur à donner au fanion N
     *
     * @param H
     *            La valeur à donner au fanion H
     *
     * @param C
     *            La valeur à donner au fanion C
     *
     * @return l'entier avec les fanions aux valeurs fournies
     */
    public static int maskZNHC(boolean Z, boolean N, boolean H, boolean C) {
        return (Z ? Flag.Z.mask() : 0) | (N ? Flag.N.mask() : 0) | (H ? Flag.H.mask() : 0) | (C ? Flag.C.mask() : 0);
    }

    /**
     * Cette méthode extrait la valeur stockée dans un entier.
     *
     * @param valueFlags
     *            L'entier à utiliser pour l'extraction
     *
     * @return la valeur stockée dans l'entier
     */
    public static int unpackValue(int valueFlags) {
        return Bits.extract(valueFlags, valueStart, valueBits);
    }

    /**
     * Cette méthode extrait la valeur des fanions stockés dans un entier.
     *
     * @param valueFlags
     *            L'entier à utiliser pour l'extraction
     *
     * @return la valeur des fanions stockée dans l'entier
     */
    public static int unpackFlags(int valueFlags) {
        return Bits.clip(valueStart, valueFlags);
    }

    /**
     * Cette méthode donne le résultat de l'addition de deux entiers de 8 bits et
     * d'un bit de carry initial.
     *
     * @param l
     *            Le premier entier à ajouter
     *
     * @param r
     *            Le deuxième entier à ajouter
     *
     * @param c0
     *            La valeur du bit de carry initial (0 ou 1)
     *
     * @return le résultat de l'addition
     *
     * @throws IllegalArgumentException
     *             si l'une des deux opérandes n'est pas une valeur de 8 bits
     *
     * @see #add16H(int, int)
     * @see #add16L(int, int)
     */
    public static int add(int l, int r, boolean c0) {
        int initialCarry = c0 ? 1 : 0;
        int javaSum = Preconditions.checkBits8(l) + Preconditions.checkBits8(r) + initialCarry;
        int result = Bits.clip(8, javaSum);
        return packValueZNHC(result, result == 0, false, Bits.clip(4, l) + Bits.clip(4, r) + initialCarry > 0xF,
                javaSum > 0xFF);
    }

    /**
     * Cette méthode fait la même chose que la méthode
     * {@link #add(int, int, boolean)}, sans carry initial.
     *
     * @param l
     *            Le premier entier à ajouter
     *
     * @param r
     *            Le deuxième entier à ajouter
     *
     * @return le résultat de l'addition
     *
     * @throws IllegalArgumentException
     *             si l'une des deux opérandes n'est pas une valeur de 8 bits
     *
     * @see #add16H(int, int)
     * @see #add16L(int, int)
     */
    public static int add(int l, int r) {
        return add(l, r, false);
    }

    /**
     * Cette méthode effectue l'addition de deux entiers de 16 bits, avec les
     * fanions de l'addition des 8 LSBs.
     *
     * @param l
     *            Le premier entier à ajouter
     *
     * @param r
     *            Le deuxième entier à ajouter
     *
     * @return le résultat de l'addition, avec les fanions H et C provenant de
     *         l'addition des 8 LSBs
     *
     * @throws IllegalArgumentException
     *             si l'une des deux opérandes n'est pas une valeur de 16 bits
     *
     * @see #add16H(int, int)
     */
    public static int add16L(int l, int r) {
        int lAdd = add(Bits.clip(8, Preconditions.checkBits16(l)), Bits.clip(8, Preconditions.checkBits16(r)));
        return packValueZNHC(Bits.clip(16, l + r), false, false, Bits.test(lAdd, Flag.H.index()),
                Bits.test(lAdd, Flag.C.index()));
    }

    /**
     * Cette méthode effectue l'addition de deux entiers de 16 bits, avec les
     * fanions de l'addition des 8 MSBs.
     *
     * @param l
     *            Le premier entier à ajouter
     *
     * @param r
     *            Le deuxième entier à ajouter
     *
     * @return le résultat de l'addition, avec les fanions H et C provenant de
     *         l'addition des 8 MSBs
     *
     * @throws IllegalArgumentException
     *             si l'une des deux opérandes n'est pas une valeur de 16 bits
     *
     * @see #add16L(int, int)
     */
    public static int add16H(int l, int r) {
        int lAdd = add(Bits.clip(8, Preconditions.checkBits16(l)), Bits.clip(8, Preconditions.checkBits16(r)));
        int hAdd = add(Bits.extract(l, 8, 8), Bits.extract(r, 8, 8), Bits.test(lAdd, Flag.C));
        return packValueZNHC(Bits.clip(16, l + r), false, false, Bits.test(hAdd, 5), Bits.test(hAdd, 4));
    }

    /**
     * Cette méthode donne le résultat de la soustraction de deux entiers de 8 bits
     * et d'un bit de borrow initial.
     *
     * @param l
     *            Le premier entier (le <em>minuend</em>)
     *
     * @param r
     *            Le deuxième entier (le <em>subtrahend</em>)
     * 
     * @param b0
     *            l'emprunt à prendre en compte
     *
     * @return le résultat de la soustraction
     *
     * @throws IllegalArgumentException
     *             si l'une des deux opérandes n'est pas une valeur de 8 bits
     */
    public static int sub(int l, int r, boolean b0) {
        int initialBorrow = b0 ? 1 : 0;
        int javaDifference = Preconditions.checkBits8(l) - Preconditions.checkBits8(r) - initialBorrow;
        int result = Bits.clip(8, javaDifference);
        return packValueZNHC(result, result == 0, true, Bits.clip(4, l) < Bits.clip(4, r) + initialBorrow,
                l < r + initialBorrow);
    }

    /**
     * Cette méthode fait la même chose que la méthode
     * {@link #sub(int, int, boolean)}, sans borrow initial.
     *
     * @param l
     *            Le premier entier (le <em>minuend</em>)
     *
     * @param r
     *            Le deuxième entier (le <em>subtrahend</em>)
     *
     * @return le résultat de la soustraction
     *
     * @throws IllegalArgumentException
     *             si l'une des deux opérandes n'est pas une valeur de 8 bits
     */
    public static int sub(int l, int r) {
        return sub(l, r, false);
    }

    /**
     * Cette méthode ajuste une valeur codée en décimal binaire (DCB ou BCD en
     * anglais) résultant d'une opération arithmétique.
     *
     * @param v
     *            La valeur à ajuster
     *
     * @param n
     *            La valeur du fanion N
     *
     * @param h
     *            La valeur du fanion H
     *
     * @param c
     *            La valeur du fanion C
     *
     * @return la valeur ajustée pour être correcte d'un point de vue mathématique
     *
     * @throws IllegalArgumentException
     *             si la valeur fournie n'est pas une valeur sur 8 bits
     */
    public static int bcdAdjust(int v, boolean n, boolean h, boolean c) {
        boolean fixL = h || (!n && Bits.clip(4, Preconditions.checkBits8(v)) > 9);
        boolean fixH = c || (!n && v > 0x99);
        int fix = 0x60 * (fixH ? 1 : 0) + 0x06 * (fixL ? 1 : 0);
        int adjustedValue = Bits.clip(8, n ? v - fix : v + fix);
        return packValueZNHC(adjustedValue, adjustedValue == 0, n, false, fixH);
    }

    /**
     * Cette méthode donne le résultat d'un <em>et</em> bit à bit entre deux entiers
     * sur 8 bits.
     *
     * @param l
     *            Le premier entier
     *
     * @param r
     *            Le deuxième entier
     *
     * @return la valeur de l AND r
     *
     * @throws IllegalArgumentException
     *             si l'un des deux entiers n'est pas un entier sur 8 bits
     */
    public static int and(int l, int r) {
        int result = Preconditions.checkBits8(l) & Preconditions.checkBits8(r);
        return packValueZNHC(result, result == 0, false, true, false);
    }

    /**
     * Cette méthode donne le résultat d'un <em>ou inclusif</em> bit à bit entre
     * deux entiers sur 8 bits.
     *
     * @param l
     *            Le premier entier
     *
     * @param r
     *            Le deuxième entier
     *
     * @return la valeur de l OR r
     *
     * @throws IllegalArgumentException
     *             si l'un des deux entiers n'est pas un entier sur 8 bits
     */
    public static int or(int l, int r) {
        int result = Preconditions.checkBits8(l) | Preconditions.checkBits8(r);
        return packValueZNHC(result, result == 0, false, false, false);
    }

    /**
     * Cette méthode donne le résultat d'un <em>ou exclusif</em> bit à bit entre
     * deux entiers sur 8 bits.
     *
     * @param l
     *            Le premier entier
     *
     * @param r
     *            Le deuxième entier
     *
     * @return la valeur de l XOR r
     *
     * @throws IllegalArgumentException
     *             si l'un des deux entiers n'est pas un entier sur 8 bits
     */
    public static int xor(int l, int r) {
        int result = Preconditions.checkBits8(l) ^ Preconditions.checkBits8(r);
        return packValueZNHC(result, result == 0, false, false, false);
    }

    /**
     * Cette méthode effectue un décalage de 1 bit vers la gauche de l'entier donné.
     *
     * @param v
     *            L'entier à utiliser pour effectuer la rotation
     *
     * @return l'entier "shifté" vers la gauche
     *
     * @throws IllegalArgumentException
     *             si l'entier fourni n'est pas un entier sur 8 bits
     */
    public static int shiftLeft(int v) {
        int shiftedInt = Preconditions.checkBits8(v) << 1;
        int result = Bits.clip(8, shiftedInt);
        return packValueZNHC(result, result == 0, false, false, Bits.test(v, 7));
    }

    /**
     * Cette méthode effectue un décalage arithmétique
     * <dd>(<b>Définition</b>: un décalage arithmétique tient compte du MSB, qui
     * représente le signe, et le copie)</dd> de 1 bit vers la droite de l'entier
     * donné.
     *
     * @param v
     *            L'entier à utiliser pour effectuer la rotation
     *
     * @return l'entier "shifté" vers la droite
     *
     * @throws IllegalArgumentException
     *             si l'entier fourni n'est pas un entier sur 8 bits
     *
     * @see #shiftRightL(int)
     */
    public static int shiftRightA(int v) {
        int shiftedInt = Bits.clip(8, Bits.signExtend8(Preconditions.checkBits8(v)) >> 1);
        return packValueZNHC(shiftedInt, shiftedInt == 0, false, false, Bits.test(v, 0));
    }

    /**
     * Cette méthode effectue un décalage logique
     * <dd>(<b>Définition</b>: un décalage logique introduit des 0 à gauche)</dd> de
     * 1 bit vers la droite de l'entier donné.
     *
     * @param v
     *            L'entier à utiliser pour effectuer la rotation
     *
     * @return l'entier "shifté" vers la droite
     *
     * @throws IllegalArgumentException
     *             si l'entier fourni n'est pas un entier sur 8 bits
     *
     * @see #shiftRightA(int)
     */
    public static int shiftRightL(int v) {
        int shiftedInt = Preconditions.checkBits8(v) >>> 1;
        return packValueZNHC(shiftedInt, shiftedInt == 0, false, false, Bits.test(v, 0));
    }

    /**
     * Cette méthode effectue la rotation d'un entier d'un bit dans la direction
     * donnée.
     *
     * @param d
     *            La direction de rotation
     *
     * @param v
     *            L'entier sur lequel la rotation est à effectuer
     *
     * @return l'entier avec la rotation effectuée
     *
     * @throws IllegalArgumentException
     *             si l'entier n'est pas un entier sur 8 bits
     */
    public static int rotate(RotDir d, int v) {
        int distance = (d == RotDir.LEFT) ? 1 : -1;
        int rotatedInt = Bits.rotate(8, Preconditions.checkBits8(v), distance);
        return packValueZNHC(rotatedInt, rotatedInt == 0, false, false,
                distance == 1 ? Bits.test(v, 7) : Bits.test(v, 0));
    }

    /**
     * Cette méthode effectue la rotation d'un entier d'un bit dans la direction
     * donnée, à travers la retenue.
     *
     * @param d
     *            La direction de rotation
     *
     * @param v
     *            L'entier sur lequel la rotation est à effectuer
     *
     * @param c
     *            La valeur de la retenue (0 ou 1)
     *
     * @return l'entier avec la rotation effectuée
     *
     * @throws IllegalArgumentException
     *             si l'entier n'est pas un entier sur 8 bits
     *
     * @see #rotate(RotDir, int)
     */
    public static int rotate(RotDir d, int v, boolean c) {
        int fusedValue = Bits.set(Preconditions.checkBits8(v), 8, c);
        int distance = (d == RotDir.LEFT) ? 1 : -1;
        int rotatedInt = Bits.rotate(9, fusedValue, distance);
        int rotatedValue = Bits.clip(8, rotatedInt);
        return packValueZNHC(rotatedValue, rotatedValue == 0, false, false, Bits.test(rotatedInt, 8));
    }

    /**
     * Cette méthode inverse les positions des bits d'un entier.
     *
     * @param v
     *            L'entier dont les bits sont à inverser entre eux
     *
     * @return l'entier "swappé"
     *
     * @throws IllegalArgumentException
     *             si l'entier n'est pas un entier sur 8 bits
     */
    public static int swap(int v) {
        int swappedInt = Bits.rotate(8, v, 4);
        return packValueZNHC(swappedInt, swappedInt == 0, false, false, false);
    }

    /**
     * Cette méthode teste la valeur du bit d'index donné d'un entier.
     *
     * @param v
     *            L'entier à tester
     *
     * @param bitIndex
     *            L'index du bit à tester
     *
     * @return vrai si le bit est à 1, faux si il est à 0
     */
    public static int testBit(int v, int bitIndex) {
        return packValueZNHC(0, !Bits.test(Preconditions.checkBits8(v), Objects.checkIndex(bitIndex, 8)), false, true,
                false);
    }
}

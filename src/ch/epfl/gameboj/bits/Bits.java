package ch.epfl.gameboj.bits;
import java.util.Objects;
import ch.epfl.gameboj.Preconditions;

/**
 * Classe Bits : contient des méthodes utilitaires statiques afin de manipuler
 * des ensembles de bits
 *
 * @author Christophe Saad (282557)
 * @author David Cian (287967)
 */

public final class Bits {
	
    /**
     * Constructeur privé afin de rendre la classe Bits non instanciable
     */
    private Bits() {
        
    }
    
    
    /**
     * Crée un masque dont seul le bit d'index donné vaut 1
     * 
     * @param index
     *            l'index donné
     * @return un entier dont seul le bit d'index donné vaut 1
     * @throws IndexOutOfBoundsException
     *             si l'index est invalide (s'il n'est pas compris entre 0
     *             (inclus) et 32 (exclus))
     */
    public static int mask(int index) {
        return 1 << Objects.checkIndex(index, Integer.SIZE);
    }
    
    
    
    /**
     * Teste si le bit d'index donné de l'entier bits vaut 1
     * 
     * @param bits
     *            un entier
     * @param index
     *            l'index
     * @return TRUE ssi le bit d'index donné de bits vaut 1
     * @throws IndexOutOfBoundsException
     *             si l'index est invalide (s'il n'est pas compris entre 0
     *             (inclus) et 32 (exclus))
     */
    public static boolean test(int bits, int index) {
        if((bits >> Objects.checkIndex(index, Integer.SIZE)) % 2 == 0) return false;
        return true;
    }
    
    /**
     * Teste si le bit donné de l'entier 'Bits' vaut 1
     * 
     * @param bits
     *            un entier
     * @param Bits
     *            le bit
     * @return TRUE ssi le bit d'index (donné par le Bit) de l'octet vaut 1
     * @throws IndexOutOfBoundsException
     *             si l'index est invalide (s'il n'est pas compris entre 0
     *             (inclus) et 32 (exclus))
     */
    public static boolean test(int bits, Bit Bits) {
        if((bits >> Objects.checkIndex(Bits.index(), Integer.SIZE)) % 2 == 0) return false;
        return true; 
    }
    
    /**
     * Remplace le bit d'index donné d'un entier par une nouvelle valeur (0 ou 1)
     * 
     * @param bits
     *            entier à modifier
     * @param index
     *            index du bit à modifier
     * @param newValue
     *            nouvelle valeur
     * @return un entier dont tous les bits sont égaux à ceux de l'entier bits
     *         sauf le bit d'index 'index' qui sera égal à 'newValue'
     * @throws IndexOutOfBoundsException
     *             si l'index est invalide (s'il n'est pas compris entre 0
     *             (inclus) et 32 (exclus))
     */
    public static int set(int bits, int index, boolean newValue) {
        if(newValue) {
            if(!test(bits, Objects.checkIndex(index, Integer.SIZE))) return bits + mask(index);
        } 
        else {
        	if(test(bits, Objects.checkIndex(index, Integer.SIZE))) return bits - mask(index);
        }

        return bits;
    }
     
    /**
     * Extrait de l'entier 'bits' donné ses 'size' bits de poids faible et les
     * retournent sous forme d'un entier  
     * 
     * @param size
     *            la taille de la chaîne de bits à extraire
     * @param bits
     *            l'entier à traiter
     * @return retourne une valeur dont les 'size' bits de poids faible sont
     *         égaux à ceux de 'bits', les autres valant 0
     * @throws IndexOutOfBoundsException
     *             si la taille size n'est pas comprise entre 0 (inclus) et 32
     *             (inclus)
     */
    public static int clip(int size, int bits) {
        if(Objects.checkIndex(size, Integer.SIZE + 1) == 0) {
            return 0;
        }
        return (bits << (Integer.SIZE - size)) >>> (Integer.SIZE - size);
    }
    
    /**
     * Extrait de l'entier 'bits' donné 'size' bits à partir de 'start' et les
     * retourne sous forme d'un entier  
     * 
     * @param size
     *            la taille de la chaîne de bits à extraire
     * @param bits
     *            l'entier à traiter
     * @return retourne une valeur dont les 'size' bits de poids faible sont
     *         égaux à ceux de bits allant de l'index 'start' (inclus) à l'index
     *         'start + size' (exclus)
     * @throws IndexOutOfBoundsException
     *             si start et size ne désignent pas une plage de bits valide
     */
    public static int extract(int bits, int start, int size) {
        return clip(size, bits >> Objects.checkFromIndexSize(start, size, Integer.SIZE));
    }
        
    /**
     * Applique une rotation vers la droite ou vers la gauche des size bits de
     * poids faible de bits si la distance est positive, la rotation se fait
     * vers la gauche, sinon elle se fait vers la droite
     * 
     * @param size
     *            la taille de la chaîne de bits
     * @param bits
     *            l'entier
     * @param distance
     *            distance de rotation
     * @return Retourne une valeur dont les size bits de poids faible sont ceux
     *         de bits mais auxquels une rotation de la distance donnée a été
     *         appliquée
     * @throws IllegalArgumentException
     *             si size n'est pas compris entre 0 (exclus) et 32 (inclus), ou
     *             si la valeur donnée n'est pas une valeur de size bits
     */
    public static int rotate(int size, int bits, int distance) {
        Preconditions.checkArgument(clip(size, bits) == bits);
        if(distance != 0) {
            int reducedDistance = Math.floorMod(distance, Objects.checkIndex(size, Integer.SIZE + 1));
            int bits1 = bits << reducedDistance;
            int bits2 = bits >>> size - reducedDistance;
            
            return clip(size, fuseInts(bits1, bits2));
        }
        return clip(size, bits);
    }     
    
    /**
     * Copie le bit d'index 7 dans les bits d'index 8 à 31 de la valeur
     * retournée (étend le signe de b)
     * 
     * @param b
     *            l'entier
     * @return Retourne une valeur égale à 'b' mais dont les bits de 8 à 31 sont
     *         égaux au bit d'index 7
     * @throws IllegalArgumentException
     *             si la valeur donnée n'est pas une valeur de 8 bits
     */
    public static int signExtend8(int b) {
        byte y = (byte)Preconditions.checkBits8(b);
        int z = (int)y;
        return z;
    }
    
    /**
     * Renverse la valeur des 8 bits de l'entier b (échange les valeurs des bits
     * 7 et 0, 6 et 1, 5 et 2, 4 et 3)
     * 
     * @param b
     *            l'entier
     * @return Retourne une valeur égale à b mais dont les bits d'index 0 et 7
     *         ont été échangés, de même que ceux d'index 1 et 6, 2 et 5, et 3
     *         et 4
     * @throws IllegalArgumentException
     *             si la valeur donnée n'est pas une valeur de 8 bits 
     */
    public static int reverse8(int b) {
        int[] reverse = new int[] {
                0x00, 0x80, 0x40, 0xC0, 0x20, 0xA0, 0x60, 0xE0,
                0x10, 0x90, 0x50, 0xD0, 0x30, 0xB0, 0x70, 0xF0,
                0x08, 0x88, 0x48, 0xC8, 0x28, 0xA8, 0x68, 0xE8,
                0x18, 0x98, 0x58, 0xD8, 0x38, 0xB8, 0x78, 0xF8,
                0x04, 0x84, 0x44, 0xC4, 0x24, 0xA4, 0x64, 0xE4,
                0x14, 0x94, 0x54, 0xD4, 0x34, 0xB4, 0x74, 0xF4,
                0x0C, 0x8C, 0x4C, 0xCC, 0x2C, 0xAC, 0x6C, 0xEC,
                0x1C, 0x9C, 0x5C, 0xDC, 0x3C, 0xBC, 0x7C, 0xFC,
                0x02, 0x82, 0x42, 0xC2, 0x22, 0xA2, 0x62, 0xE2,
                0x12, 0x92, 0x52, 0xD2, 0x32, 0xB2, 0x72, 0xF2,
                0x0A, 0x8A, 0x4A, 0xCA, 0x2A, 0xAA, 0x6A, 0xEA,
                0x1A, 0x9A, 0x5A, 0xDA, 0x3A, 0xBA, 0x7A, 0xFA,
                0x06, 0x86, 0x46, 0xC6, 0x26, 0xA6, 0x66, 0xE6,
                0x16, 0x96, 0x56, 0xD6, 0x36, 0xB6, 0x76, 0xF6,
                0x0E, 0x8E, 0x4E, 0xCE, 0x2E, 0xAE, 0x6E, 0xEE,
                0x1E, 0x9E, 0x5E, 0xDE, 0x3E, 0xBE, 0x7E, 0xFE,
                0x01, 0x81, 0x41, 0xC1, 0x21, 0xA1, 0x61, 0xE1,
                0x11, 0x91, 0x51, 0xD1, 0x31, 0xB1, 0x71, 0xF1,
                0x09, 0x89, 0x49, 0xC9, 0x29, 0xA9, 0x69, 0xE9,
                0x19, 0x99, 0x59, 0xD9, 0x39, 0xB9, 0x79, 0xF9,
                0x05, 0x85, 0x45, 0xC5, 0x25, 0xA5, 0x65, 0xE5,
                0x15, 0x95, 0x55, 0xD5, 0x35, 0xB5, 0x75, 0xF5,
                0x0D, 0x8D, 0x4D, 0xCD, 0x2D, 0xAD, 0x6D, 0xED,
                0x1D, 0x9D, 0x5D, 0xDD, 0x3D, 0xBD, 0x7D, 0xFD,
                0x03, 0x83, 0x43, 0xC3, 0x23, 0xA3, 0x63, 0xE3,
                0x13, 0x93, 0x53, 0xD3, 0x33, 0xB3, 0x73, 0xF3,
                0x0B, 0x8B, 0x4B, 0xCB, 0x2B, 0xAB, 0x6B, 0xEB,
                0x1B, 0x9B, 0x5B, 0xDB, 0x3B, 0xBB, 0x7B, 0xFB,
                0x07, 0x87, 0x47, 0xC7, 0x27, 0xA7, 0x67, 0xE7,
                0x17, 0x97, 0x57, 0xD7, 0x37, 0xB7, 0x77, 0xF7,
                0x0F, 0x8F, 0x4F, 0xCF, 0x2F, 0xAF, 0x6F, 0xEF,
                0x1F, 0x9F, 0x5F, 0xDF, 0x3F, 0xBF, 0x7F, 0xFF,
              };
       return reverse[Preconditions.checkBits8(b)];      
    }
    
    /**
     * Inverse la valeur des 8 bits de l'entier b (un bit 0 prend la valeur 1 et
     * vice versa)
     * 
     * @param b
     *            l'entier
     * @return retourne une valeur égale à celle donnée mais dont les 8 bits de
     *         poids faible ont été inversés bit à bit
     * @throws IllegalArgumentException
     *             si la valeur donnée n'est pas une valeur de 8 bits 
     */
    public static int complement8(int b) {
        return Preconditions.checkBits8(b) ^ 0b00000000_00000000_00000000_11111111;
    }
    
    /**
     * Combine deux entiers de 8 bits en un entier de 16 bits
     * 
     * @param highB
     *            entier conportant les bits de poids fort
     * @param lowB
     *            entier conportant les bits de poids fort
     * @return une valeur 16 bits dont les 8 bits de poids forts sont les 8 bits
     *         de poids faible de highB, et dont les 8 bits de poids faible sont
     *         ceux de lowB
     * @throws IllegalArgumentException
     *             si la valeur donnée n'est pas une valeur de 8 bits 
     */
    public static int make16(int highB, int lowB) {
        highB = Preconditions.checkBits8(highB) << 8;
        return fuseInts(highB, Preconditions.checkBits8(lowB));
    }
    
    /**
     * Applique un 'ou' à une liste d'entiers
     * 
     * @param intList
     *            liste d'entier
     * @return Retourne un entier résultant de l'application d'un 'ou' entre
     *         tous les entiers de la liste
     */
    public static int fuseInts(int... intList) {
        int fusion = intList[0];
        for(int i = 0; i < intList.length - 1; i++) {
            fusion |= intList[i + 1];
        }
        return fusion;
    }
}

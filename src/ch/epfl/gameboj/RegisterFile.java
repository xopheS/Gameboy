package ch.epfl.gameboj;

import java.util.Objects;

import ch.epfl.gameboj.bits.Bit;
import ch.epfl.gameboj.bits.Bits;

/**
 * Cette classe modélise un "banc de registres", i.e. un groupe cohérent de
 * registres
 * 
 * @author Christophe Saad (282557)
 * @author David Cian (287967)
 *
 * @param <E>
 *            le type de registres à stocker
 */
public final class RegisterFile<E extends Register> {
    
    private int[] registerFile;
            
    /**
     * Constructeur qui initialise le banc avec plusieurs registres
     * 
     * @param allRegs
     *            Les registres à appartenir au banc de registres
     */
    public RegisterFile(E[] allRegs) {
        
        registerFile = new int[allRegs.length];
    }
    
    /**
     * Getter de la valeur d'un registre du banc
     * 
     * @param reg
     *            Registre dont la valeur est à obtenir
     * 
     * @return la valeur du registre en question
     * 
     * @see #get(int)
     */
    public int get(E reg) {      
        return registerFile[reg.index()];
    }
    
    /**
     * Overload du getter de la valeur d'un registre du banc, pratique un contrôle plus direct
     * 
     * @param index
     * Index du registre
     * @return la valeur du registre en question
     * 
     * @see #get(Register)
     */
    public int get(int index) {
    	return registerFile[Objects.checkIndex(index, registerFile.length)];
    }
    
    /**
     * Setter de la valeur d'un registre du banc
     * 
     * @param reg
     *            Le registre dont la valeur est à modifier
     * 
     * @param newValue
     *            La valeur à lui donner
     * 
     * @throws IllegalArgumentException
     *             si la valeur n'est pas sur 8 bits
     */
    public void set(E reg, int newValue) {
        newValue &= 0xFF;
        registerFile[reg.index()] = Preconditions.checkBits8(newValue);
    }
    
    public void set(int index, int newValue) {
    	newValue &= 0xFF;
        registerFile[index] = Preconditions.checkBits8(newValue);
    }
    
    /**
     * Teste l'état d'un bit donné du registre spécifié
     * 
     * @param reg
     *            Le registre à examiner
     * 
     * @param b
     *            Le bit à tester
     * 
     * @return l'état du bit en question (0 ou 1)
     */
    public boolean testBit(E reg, Bit b) {
        return Bits.test(get(reg), b.index());
    }
    
    /**
     * Modifie l'état d'un bit donné du registre spécifié
     * 
     * @param reg
     *            Le registre à modifier
     * 
     * @param bit
     *            Le bit à modifier
     * 
     * @param newValue
     *            La valeur à donner au bit
     */
    public void setBit(E reg, Bit bit, boolean newValue) {
        set(reg, Bits.set(get(reg), bit.index(), newValue));
    }
}

package ch.epfl.gameboj;

import java.util.Objects;

import ch.epfl.gameboj.bits.Bit;
import ch.epfl.gameboj.bits.Bits;

/**
 * Cette classe modélise un "banc de registres", i.e. un groupe cohérent de
 * registres.
 *
 * @author Christophe Saad (282557)
 * @author David Cian (287967)
 *
 * @param <E>
 *            le type de registres à stocker
 */
public final class RegisterFile<E extends Register> {

	public final byte[] registerFile;

	/**
	 * Constructeur qui initialise le banc avec plusieurs registres.
	 *
	 * @param allRegs
	 *            Les registres à appartenir au banc de registres
	 */
	public RegisterFile(E[] allRegs) {

		registerFile = new byte[allRegs.length];
	}

	/**
	 * Getter de la valeur d'un registre du banc.
	 *
	 * @param reg
	 *            Registre dont la valeur est à obtenir
	 *
	 * @return la valeur du registre en question
	 *
	 * @see #get(int)
	 */
	public int get(E reg) {
		return Byte.toUnsignedInt(registerFile[reg.index()]);
	}

	/**
	 * Surcharge du getter de la valeur d'un registre du banc, offre un contrôle
	 * plus direct.
	 *
	 * @param index
	 *            Index du registre
	 * @return la valeur du registre en question
	 *
	 * @see #get(Register)
	 */
	public int get(int index) {
		return Byte.toUnsignedInt(registerFile[Objects.checkIndex(index, registerFile.length)]);
	}

	/**
	 * Setter de la valeur d'un registre du banc.
	 *
	 * @param reg
	 *            le registre dont la valeur est à modifier
	 *
	 * @param newValue
	 *            la valeur à lui donner
	 *
	 * @throws IllegalArgumentException
	 *             si la valeur n'est pas sur 8 bits
	 */
	public void set(E reg, int newValue) {
		registerFile[reg.index()] = (byte) Preconditions.checkBits8(newValue);
	}

	/**
	 * Surcharge du setter de la valeur d'un registre du banc, offre un cotrôle plus
	 * direct.
	 * 
	 * @param index
	 *            l'index du registre
	 * @param newValue
	 *            la valeur à lui donner
	 */
	public void set(int index, int newValue) {
		registerFile[index] = (byte) Preconditions.checkBits8(newValue);
	}

	/**
	 * Teste l'état d'un bit donné du registre spécifié.
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
	 * Modifie l'état d'un bit donné du registre spécifié.
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

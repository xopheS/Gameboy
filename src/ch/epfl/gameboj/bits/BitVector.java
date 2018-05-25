package ch.epfl.gameboj.bits;

import static ch.epfl.gameboj.component.lcd.LcdController.LCD_WIDTH;

import java.util.Arrays;
import java.util.Objects;

import ch.epfl.gameboj.Preconditions;

/**
 * Cette classe, immuable, simule un vecteur de bits plus long que 64, qui est
 * la taille maximale en bits d'un type intégral Java (le type long).
 *
 * @author Christophe Saad (282557)
 * @author David Cian (287967)
 *
 */
public final class BitVector {
	// Un vecteur de bits à zéro partout d'une longueur égale à la largeur de
	// l'écran de la GameBoy
	public static final BitVector BLANK_LCD_VECTOR = new BitVector(LCD_WIDTH);

	// Le tableau d'entiers sous-jacent du vecteur de bits
	private final int[] bitVector;

	/**
	 * Cette méthode construit un tableau d'entiers de la taille donnée, tous les
	 * bits étant à la valeur initiale spécifiée.
	 * 
	 * @param size
	 *            la taille du tableau
	 * @param initialValue
	 *            la valeur initiale des bits
	 * @return un tableau avec tous ses bits à la valeur donnée
	 */
	private static int[] bitVector(int size, boolean initialValue) {
		Preconditions.checkArgument(size % Integer.SIZE == 0 && size > 0);
		int[] bitVector = new int[size / Integer.SIZE];
		// Le tableau est rempli soit de 1 soit de 0
		Arrays.fill(bitVector, initialValue ? ~0 : 0);
		return bitVector;
	}

	/**
	 * Cette classe utilitaire modélise une division euclidienne, en emballant le
	 * quotient et le reste dans un même objet.
	 * 
	 * @author Christophe Saad (282557)
	 * @author David Cian (287967)
	 *
	 */
	private static class FloorEuclideanDiv {
		final int div, mod;

		FloorEuclideanDiv(int dividend, int divisor) {
			div = Math.floorDiv(dividend, divisor);
			mod = Math.floorMod(dividend, divisor);
		}
	}

	/**
	 * Cette méthode permet d'effectuer une extraction sur un vecteur de bits.
	 * Celle-ci peut être au moyen d'une extension par zéro ou d'un enroulement.
	 * 
	 * @param start
	 *            l'index de début de l'extraction
	 * @param size
	 *            la taille du vecteur extrait
	 * @param methodZeroExtended
	 *            spécifie la nature de l'extraction: extension par zéro ou
	 *            enroulement
	 * @return le vecteur de bits extrait
	 */
	private BitVector extract(int start, int size, boolean methodZeroExtended) {
		Preconditions.checkArgument(size % Integer.SIZE == 0 && size > 0);
		int[] extractedInts = new int[size / Integer.SIZE];
		// Le quotient représente l'index de début mesuré en blocs de 32 bits (la taille
		// en bits d'un entier)
		FloorEuclideanDiv d = new FloorEuclideanDiv(start, Integer.SIZE);

		if (methodZeroExtended) {
			// Cas où l'extraction n'a que des blocs entiers du vecteur d'origine
			if (Math.floorMod(start, Integer.SIZE) == 0) {
				for (int i = 0; i < extractedInts.length; i++) {
					/*
					 * Si l'index du bloc du vecteur extrait décalé de l'index du bloc de départ
					 * n'appartient pas à l'intervalle [0, longueur du vecteur[, le bloc extrait est
					 * nul, sinon c'est celui à l'index décalé
					 */
					extractedInts[i] = (i < -d.div || i >= bitVector.length - d.div) ? 0 : bitVector[i + d.div];
				}
			} else {
				for (int i = 0; i < extractedInts.length; i++) {
					extractedInts[i] = intExtZeroExtended(start + Integer.SIZE * i);
				}
			}
		} else {
			// Cas où l'extraction n'a que des blocs entiers du vecteur d'origine
			if (Math.floorMod(start, Integer.SIZE) == 0) {
				for (int i = 0; i < extractedInts.length; i++) {
					// Le bloc extrait est égal à celui de même index modulo la longueur du vecteur
					// d'origine
					extractedInts[i] = bitVector[Math.floorMod(i + d.div, bitVector.length)];
				}
			} else {
				for (int i = 0; i < extractedInts.length; i++) {
					extractedInts[i] = intExtWrapped(start + Integer.SIZE * i);
				}
			}
		}

		return new BitVector(extractedInts);
	}

	/**
	 * Cette méthode extrait un bloc (un entier) de ce vecteur, au moyen d'une
	 * extension par zéro.
	 * 
	 * @param start
	 *            l'index de départ, en bits
	 * @return l'entier extrait
	 */
	private int intExtZeroExtended(int start) {
		// Le quotient de la division représente l'index de départ mesuré en blocs
		// (entiers)
		FloorEuclideanDiv f = new FloorEuclideanDiv(start, Integer.SIZE);
		if (f.div >= bitVector.length || f.div < -1) {
			// Si l'index de départ n'appartient pas à [0, longueur du vecteur[, le bloc
			// retourné est nul
			return 0;
		} else {
			if (f.div == bitVector.length - 1) {
				return bitVector[f.div] >>> f.mod;
			}
			if (f.div == -1) {
				return (bitVector[f.div + 1] << (Integer.SIZE - f.mod));
			} else {
				return (bitVector[f.div] >>> f.mod) | (bitVector[(f.div + 1)] << (Integer.SIZE - f.mod));
			}
		}
	}

	/**
	 * Cette méthode extrait un bloc (un entier) de ce vecteur, au moyen d'un
	 * enroulement.
	 * 
	 * @param start
	 *            l'index de départ, en bits
	 * @return l'entier extrait
	 */
	private int intExtWrapped(int start) {
		FloorEuclideanDiv f = new FloorEuclideanDiv(start, Integer.SIZE);
		int startIntIndex = Math.floorDiv((Math.floorMod(start, size())), Integer.SIZE);
		return (bitVector[startIntIndex] >>> f.mod)
				| (bitVector[Math.floorMod((startIntIndex + 1), bitVector.length)] << (Integer.SIZE - f.mod));
	}

	/**
	 * Constructeur utilitaire, qui assigne un tableau d'entiers au tableau
	 * sous-jacent.
	 * 
	 * @param bitVector
	 *            le tableau à assigner
	 */
	private BitVector(int[] bitVector) {
		this.bitVector = bitVector;
	}

	/**
	 * Constructeur qui crée un vecteur de bits de taille et valeur initiale
	 * données.
	 * 
	 * @param size
	 *            la taille du vecteur
	 * @param initialValue
	 *            la valeur initiale de ses bits
	 */
	public BitVector(int size, boolean initialValue) {
		this(bitVector(size, initialValue));
	}

	/**
	 * Constructeur qui crée un vecteur de bits à 0 de taille donnée.
	 * 
	 * @param size
	 *            la taille du vecteur
	 */
	public BitVector(int size) {
		this(size, false);
	}

	/**
	 * Ce getter retourne le tableau sous-jacent d'entiers du vecteur de bits, 
	 * en préservant l'immuabilité de la classe
	 * 
	 * @return une copie du tableau sous-jacent
	 */
	public int[] getArray() {
		return Arrays.copyOf(this.bitVector, this.bitVector.length);
	}

	/**
	 * Ce getter retourne la taille du vecteur en bits.
	 * 
	 * @return la taille du vecteur en bits
	 */
	public int size() {
		return bitVector.length * Integer.SIZE;
	}

	/**
	 * Cette méthode permet de savoir si un bit donné est à 1 ou 0
	 * 
	 * @param index
	 *            l'index du bits à tester
	 * @return la valeur du bit
	 */
	public boolean testBit(int index) {
		return Bits.test(bitVector[Math.floorDiv(Objects.checkIndex(index, size()), Integer.SIZE)],
				Math.floorMod(index, Integer.SIZE));
	}

	/**
	 * Effectue la négation bit à bit du vecteur de bits.
	 * 
	 * @return la négation
	 */
	public BitVector not() {
		int[] notVector = new int[bitVector.length];

		for (int i = 0; i < notVector.length; i++) {
			notVector[i] = ~bitVector[i];
		}

		return new BitVector(notVector);
	}

	/**
	 * Effectue le ET bit à bit entre ce vecteur et un autre.
	 * 
	 * @param otherVector
	 *            l'autre vecteur
	 * @return la conjonction
	 */
	public BitVector and(BitVector otherVector) {
		Preconditions.checkArgument(
				size() == Objects.requireNonNull(otherVector, "The provided vector must not be null").size(),
				"Two bit vectors of different sizes cannot be compared.");

		int[] andVector = new int[bitVector.length];

		for (int i = 0; i < andVector.length; i++) {
			andVector[i] = bitVector[i] & otherVector.getArray()[i];
		}

		return new BitVector(andVector);
	}

	/**
	 * Effectue le OU bit à bit entre ce vecteur et un autre.
	 * 
	 * @param otherVector
	 *            l'autre vecteur
	 * @return la disjonction
	 */
	public BitVector or(BitVector otherVector) {
		Preconditions.checkArgument(
				size() == Objects.requireNonNull(otherVector, "The provided vector must not be null").size(),
				"Two bit vectors of different sizes cannot be compared.");

		int[] orVector = new int[bitVector.length];

		for (int i = 0; i < orVector.length; i++) {
			orVector[i] = bitVector[i] | otherVector.getArray()[i];
		}

		return new BitVector(orVector);
	}

	/**
	 * Cette méthode permet d'obtenir un masque des MSB de la longueur du vecteur, à
	 * partir d'un index donné.
	 * 
	 * @param index
	 *            l'index de démarcation
	 * @return le masque
	 */
	private BitVector maskMSB(int index) {
		BitVector oneVector = new BitVector(size(), true);
		return oneVector.shift(index);
	}

	/**
	 * Cette méthode permet d'obtenir un masque des LSB de la longueur du vecteur, à
	 * partir d'un index donné.
	 * 
	 * @param index
	 *            l'index de démarcation
	 * @return le masque
	 */
	private BitVector maskLSB(int index) {
		return maskMSB(index).not();
	}

	/**
	 * Cette méthode permet d'obtenir seulement les MSB d'un vecteur à partir d'un
	 * index donné.
	 * 
	 * @param index
	 *            l'index de démarcation
	 * @return le vecteur "coupé"
	 */
	public BitVector clipMSB(int index) {
		return and(maskMSB(index));
	}

	/**
	 * Cette méthode permet d'obtenir seulement les LSB d'un vecteur à partir d'un
	 * index donné.
	 * 
	 * @param index
	 *            l'index de démarcation
	 * @return le vecteur "coupé"
	 */
	public BitVector clipLSB(int index) {
		return and(maskLSB(index));
	}

	/**
	 * Cette méthode permet de donner une certaine valeur aux bits du vecteur
	 * sélectionnés par le masque.
	 * 
	 * @param mask
	 *            le masque de sélection
	 * @param value
	 *            la valeur à donner aux bits
	 * @return le vecteur avec les bits sélectionnés à la valeur donnée
	 */
	public BitVector setBits(BitVector mask, boolean value) {
		return value ? this.or(mask) : this.and(mask.not());
	}

	/**
	 * Cette méthode permet d'obtenir un vecteur extrait de celui-ci au moyen d'une
	 * extension par zéro.
	 * 
	 * @param start
	 *            l'index de départ de l'extraction
	 * @param size
	 *            la taille du vecteur extrait
	 * @return le vecteur extrait
	 */
	public BitVector extractZeroExtended(int start, int size) {
		return extract(start, size, true);
	}

	/**
	 * Cette méthode permet d'obtenir un vecteur extrait de celui-ci au moyen d'une
	 * extension par enroulement.
	 * 
	 * @param start
	 *            l'index de départ de l'extraction
	 * @param size
	 *            la taille du vecteur extrait
	 * @return le vecteur extrait
	 */
	public BitVector extractWrapped(int start, int size) {
		return extract(start, size, false);
	}

	/**
	 * Cette méthode permet d'obtenir un vecteur décalé d'une distance donnée, dans
	 * les deux sens.
	 * 
	 * @param distance
	 *            la distance de décalage
	 * @return le vecteur décalé
	 */
	public BitVector shift(int distance) {
		return extractZeroExtended(-distance, size());
	}

	@Override
	public boolean equals(Object o) {
		return (o instanceof BitVector) && Arrays.equals(bitVector, ((BitVector) o).getArray());
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(bitVector);
	}

	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		for (int i = size() - 1; i >= 0; --i) {
			b.append(testBit(i) ? 1 : 0);
		}
		return b.toString();
	}

	/**
	 * Cette classe permet de construire un vecteur de bits progressivement (comme
	 * celui-ci est immuable).
	 * 
	 * @author Christophe Saad (282557)
	 * @author David Cian (287967)
	 *
	 */
	public static final class Builder {

		private int[] vector;

		public Builder(int size) {
			vector = bitVector(size, false);
		}

		/**
		 * Change la valeur d'un octet.
		 * 
		 * @param index
		 *            l'index de début (en bits)
		 * @param b
		 *            la valeur à lui donner
		 * @return le builder
		 */
		public Builder setByte(int index, int b) {
			/*
			 * Si l'instance du constructeur a déjà construit un vecteur, il ne peut plus en
			 * construire (mais il est possible de garder la même variable et changer la
			 * référence en instanciant un nouveau constructeur)
			 */
			if (vector == null) {
				throw new IllegalStateException();
			}

			Objects.checkIndex(index, vector.length * Integer.SIZE - Byte.SIZE + 1);

			// L'index de l'entier à modifier dans le vecteur
			int intIndex = Math.floorDiv(index, Integer.SIZE);
			// La position de départ au sein de l'entier
			int startPosition = Math.floorMod(index, Integer.SIZE);
			// L'octet à modifier est mis à zéro
			vector[intIndex] &= ~(0b1111_1111 << startPosition);
			// L'octet à modifier prend la valeur donnée
			vector[intIndex] |= Preconditions.checkBits8(b) << startPosition;
			return this;
		}

		/**
		 * Construit le vecteur de bits.
		 * 
		 * @return le vecteur
		 */
		public BitVector build() {
			if (vector == null) {
				throw new IllegalStateException();
			}
			BitVector built = new BitVector(vector);
			vector = null;
			return built;
		}
	}
}

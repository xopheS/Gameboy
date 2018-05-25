package ch.epfl.gameboj.component.lcd;

import static ch.epfl.gameboj.bits.BitVector.BLANK_LCD_VECTOR;

import java.util.Objects;

import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.bits.BitVector;
import ch.epfl.gameboj.bits.Bits;

/**
 * Cette classe représente une ligne d'une image LCD.
 * 
 * @author Christophe Saad (282557)
 * @author David Cian (287967)
 *
 */
public final class LcdImageLine {

	public static final LcdImageLine BLANK_LCD_IMAGE_LINE = new LcdImageLine(BLANK_LCD_VECTOR, BLANK_LCD_VECTOR,
			BLANK_LCD_VECTOR);
	
	private static final int DMG_COLORS = 4;

	private final BitVector lsb;
	private final BitVector msb;
	private final BitVector opacity;

	/**
	 * Construit une ligne d'image.
	 * 
	 * @param msb
	 *            les most significant bits
	 * @param lsb
	 *            les least significant bits
	 * @param opacity
	 *            l'opacité (alpha)
	 */
	public LcdImageLine(BitVector msb, BitVector lsb, BitVector opacity) {
		Preconditions.checkArgument(lsb.size() == msb.size() && msb.size() == opacity.size(),
				"The three BitVectors must have the same length");

		this.lsb = lsb;
		this.msb = msb;
		this.opacity = opacity;
	}

	public int size() {
		return lsb.size();
	}

	public BitVector getLsb() {
		return this.lsb;
	}

	public BitVector getMsb() {
		return this.msb;
	}

	public BitVector getOpacity() {
		return this.opacity;
	}

	/**
	 * Cette méthode permet de décaler une ligne d'image vers la droite (si la
	 * distance est positive), ou vers la gauche si elle est négative.
	 * 
	 * @param distance
	 *            la distance de décalage
	 * @return la ligne décalée
	 */
	public LcdImageLine shift(int distance) {
		return new LcdImageLine(msb.shift(-distance), lsb.shift(-distance), opacity.shift(-distance));
	}

	/**
	 * Cette méthode permet d'effectuer une extraction par enroulement sur la ligne
	 * d'image.
	 * 
	 * @param pixel
	 *            l'index de départ
	 * @param size
	 *            la taille de la ligne extraite
	 * @return la ligne d'image extraite
	 */
	public LcdImageLine extractWrapped(int pixel, int size) {
		return new LcdImageLine(msb.extractWrapped(pixel, size), lsb.extractWrapped(pixel, size),
				opacity.extractWrapped(pixel, size));
	}

	public LcdImageLine extractZeroExtended(int pixel, int size) {
		return new LcdImageLine(msb.extractZeroExtended(pixel, size), lsb.extractZeroExtended(pixel, size),
				opacity.extractZeroExtended(pixel, size));
	}

	/**
	 * Change les couleurs d'une ligne en lui appliquant une palette.
	 * 
	 * @param palette
	 *            la palette à appliquer
	 * @return la ligne coloriée
	 */
	public LcdImageLine mapColors(int palette) {
		if (Preconditions.checkBits8(palette) == 0b11100100) {
			return this;
		}

		BitVector mask = null;
		LcdImageLine coloredLine = this;

		// Une itération est faite sur les 4 couleurs du DMG (Dot Matrix Game)
		for (int i = 0; i < DMG_COLORS; i++) {

			// La couleur correspondante, qui doit remplacer celle représentée par i
			int color = Bits.extract(palette, 2 * i, 2);

			// D'abord, seuls les pixels originellement de la couleur représentée par i sont
			// sélectionnés avec un masque
			switch (i) {
			case 0:
				mask = msb.not().and(lsb.not());
				break;
			case 1:
				mask = msb.not().and(lsb);
				break;
			case 2:
				mask = msb.and(lsb.not());
				break;
			case 3:
				mask = msb.and(lsb);
				break;
			}

			// Leur couleur est transformée
			coloredLine = coloredLine.setColor(mask, color);
		}

		return coloredLine;
	}

	/**
	 * Cette méthode permet de changer la couleurs des pixels de la ligne
	 * sélectionnés avec un masque.
	 * 
	 * @param mask
	 *            le masque de sélection
	 * @param color
	 *            la couleur à leur appliquer
	 * @return la ligne coloriée
	 */
	private LcdImageLine setColor(BitVector mask, int color) {
		BitVector msbCopy = msb.extractZeroExtended(0, size()), lsbCopy = lsb.extractZeroExtended(0, size());

		switch (Objects.checkIndex(color, DMG_COLORS)) {
		case 0:
			msbCopy = msbCopy.setBits(mask, false);
			lsbCopy = lsbCopy.setBits(mask, false);
			break;
		case 1:
			msbCopy = msbCopy.setBits(mask, false);
			lsbCopy = lsbCopy.setBits(mask, true);
			break;
		case 2:
			msbCopy = msbCopy.setBits(mask, true);
			lsbCopy = lsbCopy.setBits(mask, false);
			break;
		case 3:
			msbCopy = msbCopy.setBits(mask, true);
			lsbCopy = lsbCopy.setBits(mask, true);
			break;
		}

		return new LcdImageLine(msbCopy, lsbCopy, opacity);
	}

	/**
	 * Superpose une ligne avec une autre, en la mettant en dessous.
	 * 
	 * @param other
	 *            l'autre ligne qui est au-dessus
	 * @return la nouvelle ligne
	 */
	public LcdImageLine below(LcdImageLine other) {
		Preconditions.checkArgument(other.size() == size(), "The two lines must have the same length");

		BitVector newMSB = (msb.and(other.opacity.not())).or(other.msb.and(other.opacity));
		BitVector newLSB = (lsb.and(other.opacity.not())).or(other.lsb.and(other.opacity));

		return new LcdImageLine(newMSB, newLSB, opacity.or(other.opacity));
	}

	/**
	 * Superpose une ligne avec une autre, en la mettant en dessous, avec une
	 * opacité donnée.
	 * 
	 * @param other
	 *            l'autre ligne
	 * @param opacity
	 *            l'opacité à utiliser
	 * @return la nouvelle ligne
	 */
	public LcdImageLine below(LcdImageLine other, BitVector opacity) {
		Preconditions.checkArgument(other.size() == size() && opacity.size() == size(),
				"The two lines and the opacity vector must have the same length");

		BitVector newMSB = msb.and(opacity.not()).or(other.msb.and(opacity));
		BitVector newLSB = lsb.and(opacity.not()).or(other.lsb.and(opacity));

		return new LcdImageLine(newMSB, newLSB, this.opacity.or(opacity));
	}

	/**
	 * Joint deux lignes, à partir d'un index.
	 * 
	 * @param other
	 *            l'autre ligne
	 * @param n
	 *            l'index de démarcation
	 * @return la nouvelle ligne
	 */
	public LcdImageLine join(LcdImageLine other, int n) {
		int size = size();
		Preconditions.checkArgument(other.size() == size, "The two image lines must have the same length");
		Objects.checkIndex(n, size);

		return new LcdImageLine(msb.clipLSB(n).or(other.msb.clipMSB(n)), lsb.clipLSB(n).or(other.lsb.clipMSB(n)),
				opacity.or(other.opacity));
	}

	@Override
	public boolean equals(Object o) {
		return (o instanceof LcdImageLine) && lsb.equals(((LcdImageLine) o).getLsb())
				&& msb.equals(((LcdImageLine) o).getMsb()) && opacity.equals(((LcdImageLine) o).getOpacity());
	}

	@Override
	public int hashCode() {
		return Objects.hash(msb, lsb, opacity);
	}

	public static final class Builder {

		BitVector.Builder msbBuilder;
		BitVector.Builder lsbBuilder;

		public Builder(int width) {
			msbBuilder = new BitVector.Builder(width);
			lsbBuilder = new BitVector.Builder(width);
		}

		/**
		 * Modifie les octets d'une ligne.
		 * 
		 * @param index
		 *            l'index des octets à modifier (en bits)
		 * @param byteMSB
		 *            l'octet des MSB
		 * @param byteLSB
		 *            l'octet des LSB
		 * @return le constructeur
		 */
		public Builder setBytes(int index, int byteMSB, int byteLSB) {
			msbBuilder.setByte(index, byteMSB);
			lsbBuilder.setByte(index, byteLSB);
			return this;
		}

		/**
		 * Construit la ligne.
		 * 
		 * @return la ligne
		 */
		public LcdImageLine build() {
			BitVector lsb = lsbBuilder.build();
			BitVector msb = msbBuilder.build();
			return new LcdImageLine(msb, lsb, msb.or(lsb));
		}
	}
}

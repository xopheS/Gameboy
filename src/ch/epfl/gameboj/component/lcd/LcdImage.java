package ch.epfl.gameboj.component.lcd;

import static ch.epfl.gameboj.component.lcd.LcdController.LCD_HEIGHT;
import static ch.epfl.gameboj.component.lcd.LcdController.LCD_WIDTH;
import static ch.epfl.gameboj.component.lcd.LcdImageLine.BLANK_LCD_IMAGE_LINE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.bits.BitVector;

/**
 * Cette classe modélise de manière abstraite une image LCD de la GameBoy.
 * 
 * @author Christophe Saad (282557)
 * @author David Cian (287967)
 *
 */
public final class LcdImage {

	public static final LcdImage BLANK_LCD_IMAGE = new LcdImage(LCD_WIDTH, LCD_HEIGHT,
			Collections.nCopies(LCD_HEIGHT, BLANK_LCD_IMAGE_LINE));

	private final int width, height;
	private final List<LcdImageLine> imageLines;

	/**
	 * Construit une image à partir d'une liste de lignes.
	 * 
	 * @param width
	 *            la largeur
	 * @param height
	 *            la hauteur
	 * @param list
	 *            la liste des lignes
	 */
	public LcdImage(int width, int height, List<LcdImageLine> list) {
		this.width = width;
		this.height = height;
		imageLines = Collections.unmodifiableList(new ArrayList<>(list));
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	/**
	 * Obtient la couleur d'un pixel donné.
	 * 
	 * @param x
	 *            son abscisse
	 * @param y
	 *            son ordonnée
	 * @return la couleur du pixel
	 */
	public int get(int x, int y) {
		Preconditions.checkArgument(x < width && x >= 0 && y < height && y >= 0,
				"Pixel coordinates must be within the bounds of the image");
		int lsb = imageLines.get(y).getLsb().testBit(x) ? 1 : 0;
		int msb = (imageLines.get(y).getMsb().testBit(x) ? 1 : 0) << 1;
		return (lsb | msb);
	}

	@Override
	public boolean equals(Object o) {
		for (int i = 0; i < height; ++i) {
			if (!imageLines.get(i).equals(((LcdImage) o).imageLines.get(i))) {
				return false;
			}
		}

		return (o instanceof LcdImage) && height == (((LcdImage) o).height) && width == (((LcdImage) o).width);
	}

	@Override
	public int hashCode() {
		return Objects.hash(height, width, imageLines);
	}

	public static final class Builder {
		int builderHeight;
		int builderWidth;
		List<LcdImageLine> imageLines;

		/**
		 * Construit un constructeur d'images.
		 * 
		 * @param width
		 *            la largeur de l'image à construire
		 * @param height
		 *            la hauteur de l'image à construire
		 */
		public Builder(int width, int height) {
			builderWidth = width;
			builderHeight = height;
			imageLines = new ArrayList<>(height);
			for (int i = 0; i < height; i++) {
				imageLines.add(new LcdImageLine(new BitVector(width), new BitVector(width), new BitVector(width)));
			}
		}

		/**
		 * Modifie une ligne.
		 * 
		 * @param index
		 *            l'index de la ligne à modifier
		 * @param l
		 *            la ligne à y mettre
		 * @return le constructeur
		 */
		public Builder setLine(int index, LcdImageLine l) {
			imageLines.set(Objects.checkIndex(index, builderHeight), l);
			return this;
		}

		public LcdImage build() {
			return new LcdImage(builderWidth, builderHeight, imageLines);
		}
	}
}

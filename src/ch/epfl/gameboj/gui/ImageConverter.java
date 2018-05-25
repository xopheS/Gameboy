package ch.epfl.gameboj.gui;

import ch.epfl.gameboj.component.lcd.LcdImage;
import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

/**
 * Cette classe utilitaire permet de faire la liaison entre le format LcdImage
 * et Image (de JavaFX).
 * 
 * @author Christophe Saad (282557)
 * @author David Cian (287967)
 *
 */
public final class ImageConverter {

	private enum JavaFXColor {
		COLOR0(0xFF, 0xFF, 0xFF, 0xFF), COLOR1(0xFF, 0xD3, 0xD3, 0xD3), COLOR2(0xFF, 0xA9, 0xA9, 0xA9), COLOR3(0xFF,
				0x00, 0x00, 0x00);

		private int alpha, red, green, blue;

		JavaFXColor(int alpha, int red, int green, int blue) {
			this.alpha = alpha;
			this.red = red;
			this.green = green;
			this.blue = blue;
		}

		/**
		 * Cette méthode permet d'obtenir un entier représentant une couleur sous format
		 * ARGB (Alpha Red Green Blue).
		 * 
		 * @return la couleur codée en ARGB
		 */
		int getARGB() {
			return alpha << Byte.SIZE * 3 | red << Byte.SIZE * 2 | green << Byte.SIZE | blue;
		}
	}

	public static Image convert(LcdImage lcdImage) {
		WritableImage wImage = new WritableImage(lcdImage.getWidth(), lcdImage.getHeight());
		PixelWriter pWriter = wImage.getPixelWriter();

		for (int y = 0; y < lcdImage.getHeight(); ++y) {
			for (int x = 0; x < lcdImage.getWidth(); ++x) {
				int color = 0;

				switch (lcdImage.get(x, y)) {
				case 0:
					color = JavaFXColor.COLOR0.getARGB();
					break;
				case 1:
					color = JavaFXColor.COLOR1.getARGB();
					break;
				case 2:
					color = JavaFXColor.COLOR2.getARGB();
					break;
				case 3:
					color = JavaFXColor.COLOR3.getARGB();
					break;
				}

				pWriter.setArgb(x, y, color);
			}
		}

		return wImage;
	}
}

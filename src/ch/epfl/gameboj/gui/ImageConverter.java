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

	private static int[] JavaFXColor = new int[] {0xFFFFFFFF, 0xFFD3D3D3, 0xFFA9A9A9, 0xFF000000};
	
	/**
	 * Cette méthode permet de convertir une image lcd en image JavaFX
	 * 
	 * @param lcdImage
	 *            l'image à convertir
	 * @return l'image JavaFX
	 */
	public static Image convert(LcdImage lcdImage) {
        WritableImage wImage = new WritableImage(lcdImage.getWidth(), lcdImage.getHeight());
        PixelWriter pWriter = wImage.getPixelWriter();

        for (int y = 0; y < lcdImage.getHeight(); ++y) {

            for (int x = 0; x < lcdImage.getWidth(); ++x) {

                int color = JavaFXColor[lcdImage.get(x, y)];

                pWriter.setArgb(x, y, color);
            }
        }

        return wImage;
    }
}

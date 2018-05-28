package ch.epfl.gameboj.gui;

import ch.epfl.gameboj.component.lcd.LcdImage;
import ch.epfl.gameboj.gui.color.ColorTheme;
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
    private static ColorTheme currentColorTheme = ColorTheme.STANDARD_COLOR_THEME;

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
                int color = 0;

                switch (lcdImage.get(x, y)) {
                case 0:
                    color = currentColorTheme.getColor0().getARGB();
                    break;
                case 1:
                    color = currentColorTheme.getColor1().getARGB();
                    break;
                case 2:
                    color = currentColorTheme.getColor2().getARGB();
                    break;
                case 3:
                    color = currentColorTheme.getColor3().getARGB();
                    break;
                }

                pWriter.setArgb(x, y, color);
            }
        }
        
        return wImage;
    }
    
    public static void setColorTheme(ColorTheme newColorTheme) {
    	currentColorTheme = newColorTheme;
    }
}

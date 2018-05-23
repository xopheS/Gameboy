package ch.epfl.gameboj.gui;

import ch.epfl.gameboj.component.lcd.LcdImage;
import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

/**
 * Cette classe représente le convertisseur d'image de la gameboj en image JavaFX
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

        int getARG_B() {
            return alpha << Byte.SIZE * 3 | red << Byte.SIZE * 2 | green << Byte.SIZE | blue;
        }
    }

  /**
   * Cette méthode convertit l'image donnée en argument en image JavaFx
   * 
   * @param lcdImage
   *          l'image
   * @return une image JavaFx
   */
    public static Image convert(LcdImage lcdImage) {
        WritableImage wImage = new WritableImage(lcdImage.getWidth(), lcdImage.getHeight());
        PixelWriter pWriter = wImage.getPixelWriter();

        for (int y = 0; y < lcdImage.getHeight(); ++y) {
            for (int x = 0; x < lcdImage.getWidth(); ++x) {
                int color = 0;

                switch (lcdImage.get(x, y)) {
                case 0:
                    color = JavaFXColor.COLOR0.getARG_B();
                    break;
                case 1:
                    color = JavaFXColor.COLOR1.getARG_B();
                    break;
                case 2:
                    color = JavaFXColor.COLOR2.getARG_B();
                    break;
                case 3:
                    color = JavaFXColor.COLOR3.getARG_B();
                    break;
                }

                pWriter.setArgb(x, y, color);
            }
        }
        
        return wImage;
    }
}

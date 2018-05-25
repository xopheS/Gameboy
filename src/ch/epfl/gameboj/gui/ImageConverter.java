package ch.epfl.gameboj.gui;

import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.bits.Bits;
import ch.epfl.gameboj.component.lcd.LcdImage;
import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

public final class ImageConverter {

    public static enum JavaFXColor {
        COLOR0(0xFF, 0xFF, 0xFF, 0xFF), COLOR1(0xFF, 0xD3, 0xD3, 0xD3), COLOR2(0xFF, 0xA9, 0xA9, 0xA9), COLOR3(0xFF,
                0x00, 0x00, 0x00);

        private int alpha, red, green, blue;

        JavaFXColor(int alpha, int red, int green, int blue) {
            this.alpha = alpha;
            this.red = red;
            this.green = green;
            this.blue = blue;
        }
        
        void setAlpha(int i) {
        	this.alpha = i;
        }
        
        void setRed(int red) {
        	this.red = red;
        }
        
        void setGreen(int green) {
        	this.green = green;
        }
        
        void setBlue(int blue) {
        	this.blue = blue;
        }
        
        void setARGB(int argb) {
        	this.alpha = Bits.extract(argb, 0, Byte.SIZE);
        	this.red = Bits.extract(argb, Byte.SIZE, Byte.SIZE);
        	this.green = Bits.extract(argb, 2 * Byte.SIZE, Byte.SIZE);
        	this.blue = Bits.extract(argb, 3 * Byte.SIZE, Byte.SIZE);
        }

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

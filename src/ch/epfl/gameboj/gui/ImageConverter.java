package ch.epfl.gameboj.gui;

import static ch.epfl.gameboj.component.lcd.LcdController.LCD_WIDTH;
import static ch.epfl.gameboj.component.lcd.LcdController.LCD_HEIGHT;

import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.component.lcd.LcdImage;
import javafx.scene.image.Image;

public final class ImageConverter {
    
    private static class JavaFXColor {       
        private int alpha, red, green, blue;
        
        JavaFXColor(int alpha, int red, int green, int blue) {
            this.alpha = alpha;
            this.red = red;
            this.green = green;
            this.blue = blue;
        }
    } //nested class or enum? TODO
    
    JavaFXColor[] fxColors = new JavaFXColor[] { new JavaFXColor(0xFF, 0xFF, 0xFF, 0xFF), new JavaFXColor(0xFF, 0xD3, 0xD3, 0xD3),
            new JavaFXColor(0xFF, 0xA9, 0xA9, 0xA9), new JavaFXColor(0xFF, 0x00, 0x00, 0x00)
    };
    
    public static Image convert(LcdImage lcdImage) {
        Preconditions.checkArgument(lcdImage.getWidth() == LCD_WIDTH && lcdImage.getHeight() == LCD_HEIGHT);
    }
}
